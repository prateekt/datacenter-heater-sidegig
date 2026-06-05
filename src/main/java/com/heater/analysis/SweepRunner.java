package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.SimulationMetrics;
import com.heater.thermal.Simulator;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class SweepRunner {

    private final Map<String, Object> baseConfig;
    private final Map<String, Object> sweepConfig;
    private final GpuProfile.GpuProfileRegistry registry;
    private final String scenarioName;
    private final double simDurationS;

    public SweepRunner(String baseConfigPath, String sweepConfigPath) throws IOException {
        this.sweepConfig = ConfigLoader.load(sweepConfigPath);
        String basePath = sweepConfig.getOrDefault("base_config", baseConfigPath).toString();
        this.baseConfig = ConfigLoader.load(basePath);
        String gpuProfilesPath = sweepConfig.getOrDefault("gpu_profiles", "config/gpu_profiles.yaml").toString();
        this.registry = GpuProfile.load(gpuProfilesPath);
        this.scenarioName = sweepConfig.getOrDefault("scenario_name", "nvidia_us_module").toString();
        this.simDurationS = ConfigLoader.d(sweepConfig, "sim_duration_s", 604_800.0);
    }

    public Map<String, Object> sweepConfig() {
        return sweepConfig;
    }

    public ResultsSummary runAll() {
        ResultsSummary summary = new ResultsSummary();
        summary.putMetadata("sim_duration_s", simDurationS);
        summary.putMetadata("reference_profile_id", registry.referenceProfileId());
        summary.putMetadata("reference_gpu_count", registry.referenceGpuCount());

        Map<String, Object> sweeps = ConfigLoader.map(sweepConfig, "sweeps");
        runGpuCountRamp(summary, ConfigLoader.map(sweeps, "gpu_count_ramp"));
        runGpuGeneration(summary, ConfigLoader.map(sweeps, "gpu_generation"));
        runSaturation(summary, ConfigLoader.map(sweeps, "saturation"));
        runMultiHall(summary, ConfigLoader.map(sweeps, "multi_hall"));
        runForecastTimeline(summary, sweeps.get("forecast_timeline"));
        return summary;
    }

    private void runGpuCountRamp(ResultsSummary summary, Map<String, Object> cfg) {
        if (cfg.isEmpty()) return;
        GpuProfile profile = registry.require(cfg.get("profile_id").toString());
        List<Integer> counts = intList(cfg.get("gpu_counts"));
        for (int gpus : counts) {
            summary.addPoint(runPoint("gpu_count_ramp", gpus + " GPUs", gpus, profile, 1,
                    FacilityScaler.scale(baseConfig, scenarioName, profile, gpus, registry,
                            FacilityScaler.ScaleMode.PROPORTIONAL, simDurationS)));
        }
    }

    private void runGpuGeneration(ResultsSummary summary, Map<String, Object> cfg) {
        if (cfg.isEmpty()) return;
        int gpus = (int) ConfigLoader.d(cfg, "gpu_count", 37_000);
        List<String> ids = ConfigLoader.stringList(cfg, "profile_ids");
        for (String id : ids) {
            GpuProfile profile = registry.require(id);
            summary.addPoint(runPoint("gpu_generation", profile.displayName(), gpus, profile, 1,
                    FacilityScaler.scale(baseConfig, scenarioName, profile, gpus, registry,
                            FacilityScaler.ScaleMode.PROPORTIONAL, simDurationS)));
        }
    }

    private void runSaturation(ResultsSummary summary, Map<String, Object> cfg) {
        if (cfg.isEmpty()) return;
        GpuProfile profile = registry.require(cfg.get("profile_id").toString());
        int refGpus = (int) ConfigLoader.d(cfg, "reference_gpu_count", 37_000);
        List<Double> multipliers = doubleList(cfg.get("heat_multipliers"));
        for (double mult : multipliers) {
            int effective = (int) (refGpus * mult);
            String label = String.format("%.1fx heat (%d GPUs equiv.)", mult, effective);
            summary.addPoint(runPoint("saturation", label, effective, profile, 1,
                    FacilityScaler.scaleByHeatMultiplier(baseConfig, scenarioName, profile,
                            refGpus, mult, registry, simDurationS)));
        }
    }

    private void runMultiHall(ResultsSummary summary, Map<String, Object> cfg) {
        if (cfg.isEmpty()) return;
        GpuProfile profile = registry.require(cfg.get("profile_id").toString());
        int refGpus = registry.referenceGpuCount();
        List<Integer> halls = intList(cfg.get("halls"));
        FacilityScaler.ScaledFacility hall = FacilityScaler.scale(
                baseConfig, scenarioName, profile, refGpus, registry,
                FacilityScaler.ScaleMode.PROPORTIONAL, simDurationS);
        SweepPoint oneHall = runPoint("multi_hall", "1 hall", refGpus, profile, 1, hall);
        for (int h : halls) {
            double netTonnes = oneHall.annualizedNetTonnes() * h;
            double grossTonnes = oneHall.annualizedGrossTonnes() * h;
            summary.addPoint(new SweepPoint(
                    "multi_hall",
                    h + (h == 1 ? " hall" : " halls"),
                    refGpus,
                    profile.id(),
                    profile.displayName(),
                    h,
                    profile.avgWasteHeatMw(refGpus) * h,
                    oneHall.netCo2eKg() * h,
                    oneHall.grossCo2Kg() * h,
                    netTonnes,
                    grossTonnes,
                    profile.forecast()
            ));
        }
    }

    private void runForecastTimeline(ResultsSummary summary, Object raw) {
        if (!(raw instanceof List<?> list)) return;
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> m)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> entry = (Map<String, Object>) m;
            int year = (int) ConfigLoader.d(entry, "year", 2024);
            String label = entry.getOrDefault("label", "Year " + year).toString();
            int gpus = (int) ConfigLoader.d(entry, "gpu_count", 37_000);
            GpuProfile profile = registry.require(entry.get("profile_id").toString());
            summary.addPoint(runPoint("forecast_timeline", year + ": " + label, gpus, profile, 1,
                    FacilityScaler.scale(baseConfig, scenarioName, profile, gpus, registry,
                            FacilityScaler.ScaleMode.PROPORTIONAL, simDurationS)));
        }
    }

    private SweepPoint runPoint(
            String sweepId,
            String label,
            int gpuCount,
            GpuProfile profile,
            int halls,
            FacilityScaler.ScaledFacility facility
    ) {
        Simulator sim = new Simulator(facility.config());
        SimulationMetrics metrics = sim.run(facility.scenario(), null);
        var c = metrics.climate();
        double annualFactor = simDurationS > 0 ? (365.0 * 86400.0 / simDurationS) : 0.0;
        double annualNet = c.annualizedTonnesCo2e() * halls;
        double annualGross = c.grossRemovalKg() * annualFactor / 1000.0 * halls;
        return new SweepPoint(
                sweepId,
                label,
                gpuCount,
                profile.id(),
                profile.displayName(),
                halls,
                profile.avgWasteHeatMw(gpuCount) * halls,
                c.netCo2eRemovedKg() * halls,
                c.grossRemovalKg() * halls,
                annualNet,
                annualGross,
                profile.forecast()
        );
    }

    private static List<Integer> intList(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().map(o -> ((Number) o).intValue()).toList();
    }

    private static List<Double> doubleList(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().map(o -> ((Number) o).doubleValue()).toList();
    }
}
