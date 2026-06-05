package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.ScenarioProfile;
import com.heater.thermal.ScenarioUtil;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class FacilityScaler {

    private FacilityScaler() {}

    public enum ScaleMode {
        PROPORTIONAL,
        SATURATION
    }

    public record ScaledFacility(
            Map<String, Object> config,
            ScenarioProfile scenario,
            int gpuCount,
            GpuProfile profile,
            double plantScale
    ) {}

    public static ScaledFacility scale(
            Map<String, Object> baseConfig,
            String scenarioName,
            GpuProfile profile,
            int gpuCount,
            GpuProfile.GpuProfileRegistry registry,
            ScaleMode mode,
            double simDurationS
    ) {
        Map<String, Object> config = ConfigDeepCopy.copy(baseConfig);
        GpuProfile refProfile = registry.referenceProfile();
        int refGpus = registry.referenceGpuCount();

        double heatScale = switch (mode) {
            case PROPORTIONAL -> (double) gpuCount / refGpus;
            case SATURATION -> 1.0;
        };

        double plantScale = switch (mode) {
            case PROPORTIONAL -> heatScale * (profile.systemWasteWPerGpu() / refProfile.systemWasteWPerGpu());
            case SATURATION -> 1.0;
        };

        applyPlantScale(config, plantScale);

        double qBase = gpuCount * profile.systemWasteWPerGpu() * registry.baseUtilizationFactor();
        double qPeak = gpuCount * profile.systemWasteWPerGpu() * registry.peakUtilizationFactor();

        Map<String, Object> scenarios = ConfigLoader.map(config, "scenarios");
        Map<String, Object> sc = ConfigLoader.map(scenarios, scenarioName);
        sc.put("duration", simDurationS);
        sc.put("q_waste_base", qBase);
        sc.put("q_waste_peak", qPeak);

        List<Map<String, Object>> temps = (List<Map<String, Object>>) sc.get("outside_temps");
        ScenarioProfile scenario = new ScenarioProfile(
                scenarioName, simDurationS, qBase, qPeak, temps
        );

        return new ScaledFacility(config, scenario, gpuCount, profile, plantScale);
    }

    public static ScaledFacility scaleByHeatMultiplier(
            Map<String, Object> baseConfig,
            String scenarioName,
            GpuProfile profile,
            int referenceGpuCount,
            double heatMultiplier,
            GpuProfile.GpuProfileRegistry registry,
            double simDurationS
    ) {
        int effectiveGpus = (int) (referenceGpuCount * heatMultiplier);
        Map<String, Object> config = ConfigDeepCopy.copy(baseConfig);
        // Plant fixed at reference H100 hall size
        applyPlantScale(config, 1.0);

        double qBase = effectiveGpus * profile.systemWasteWPerGpu() * registry.baseUtilizationFactor();
        double qPeak = effectiveGpus * profile.systemWasteWPerGpu() * registry.peakUtilizationFactor();

        Map<String, Object> scenarios = ConfigLoader.map(config, "scenarios");
        Map<String, Object> sc = ConfigLoader.map(scenarios, scenarioName);
        sc.put("duration", simDurationS);
        sc.put("q_waste_base", qBase);
        sc.put("q_waste_peak", qPeak);

        List<Map<String, Object>> temps = (List<Map<String, Object>>) sc.get("outside_temps");
        ScenarioProfile scenario = new ScenarioProfile(
                scenarioName, simDurationS, qBase, qPeak, temps
        );

        return new ScaledFacility(config, scenario, effectiveGpus, profile, 1.0);
    }

    private static void applyPlantScale(Map<String, Object> config, double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("plant scale must be positive");
        }
        scaleMap(config, "primary_loop", "mdot", scale);
        scaleMap(config, "primary_loop", "reject_capacity", scale);
        scaleMap(config, "primary_loop", "t_min_flow", scale);
        scaleMap(config, "heat_exchanger", "ua", scale);
        scaleMap(config, "buffer_tank", "volume", scale);
        scaleNested(config, "loads", "carbon_capture", "hp_capacity_w", scale);
        scaleNested(config, "loads", "algae", "surface_area_m2", scale);
        scaleNested(config, "loads", "algae", "volume_m3", scale);
        scaleNested(config, "loads", "algae", "loss_ua", scale);
        scaleMap(config, "control", "max_secondary_flow", scale);
    }

    private static void scaleMap(Map<String, Object> root, String section, String key, double scale) {
        Map<String, Object> sec = ConfigLoader.map(root, section);
        if (sec.isEmpty()) return;
        Object v = sec.get(key);
        if (v instanceof Number n) {
            sec.put(key, n.doubleValue() * scale);
        }
    }

    private static void scaleNested(Map<String, Object> root, String section, String subsection, String key, double scale) {
        Map<String, Object> sec = ConfigLoader.map(root, section);
        Map<String, Object> sub = ConfigLoader.map(sec, subsection);
        if (sub.isEmpty()) return;
        Object v = sub.get(key);
        if (v instanceof Number n) {
            sub.put(key, n.doubleValue() * scale);
        }
    }
}
