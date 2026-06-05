package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.ScenarioUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Compares DAC net removal to CO₂ emitted powering GPU operations (grid electricity).
 * Waste-heat watts are used as a proxy for IT thermal load — nearly all GPU power becomes heat.
 */
public final class OperationalCarbon {

    private final double gridCo2KgPerKwh;
    private final double pue;

    public OperationalCarbon(double gridCo2KgPerKwh, double pue) {
        this.gridCo2KgPerKwh = gridCo2KgPerKwh;
        this.pue = pue;
    }

    public static OperationalCarbon fromConfig() throws IOException {
        Map<String, Object> climate = ConfigLoader.map(
                ConfigLoader.load("config/nvidia_us_expansion.yaml"), "climate");
        Map<String, Object> analogies = ConfigLoader.load("config/impact_analogies.yaml");
        return new OperationalCarbon(
                ConfigLoader.d(climate, "grid_co2_kg_per_kwh", 0.39),
                ConfigLoader.d(analogies, "facility_pue", 1.15)
        );
    }

    public record RecoveryAnalysis(
            double avgItHeatMw,
            double annualElectricityGwh,
            double operationalCo2Tonnes,
            double netRemovedTonnes,
            double recoveryPercent,
            double netBalanceTonnes
    ) {}

    public RecoveryAnalysis analyze(double qWasteBaseW, double qWastePeakW, double netRemovedTonnes) {
        double avgW = averageWasteHeatW(qWasteBaseW, qWastePeakW);
        double facilityAvgW = avgW * pue;
        double kwhPerYear = facilityAvgW * 8760.0 / 1000.0;
        double operationalTonnes = kwhPerYear * gridCo2KgPerKwh / 1000.0;
        double recoveryPct = operationalTonnes > 0 ? 100.0 * netRemovedTonnes / operationalTonnes : 0.0;
        double netBalance = netRemovedTonnes - operationalTonnes;
        return new RecoveryAnalysis(
                avgW / 1_000_000.0,
                kwhPerYear / 1_000_000.0,
                operationalTonnes,
                netRemovedTonnes,
                recoveryPct,
                netBalance
        );
    }

    public RecoveryAnalysis forHall(
            GpuProfile profile,
            int gpuCount,
            GpuProfile.GpuProfileRegistry registry,
            double netRemovedTonnes
    ) {
        double qBase = gpuCount * profile.systemWasteWPerGpu() * registry.baseUtilizationFactor();
        double qPeak = gpuCount * profile.systemWasteWPerGpu() * registry.peakUtilizationFactor();
        return analyze(qBase, qPeak, netRemovedTonnes);
    }

    static double averageWasteHeatW(double qBase, double qPeak) {
        double sum = 0.0;
        int steps = 1440;
        for (int i = 0; i < steps; i++) {
            sum += ScenarioUtil.qWasteAtTime(i * 60.0, qBase, qPeak);
        }
        return sum / steps;
    }

    public String explainRecovery(RecoveryAnalysis r, ClimateAnalogies impact) {
        double opsCars = impact.carsFromAnnualTonnes(r.operationalCo2Tonnes());
        double netCars = impact.carsFromAnnualTonnes(r.netRemovedTonnes());
        double balanceCars = impact.carsFromAnnualTonnes(Math.abs(r.netBalanceTonnes()));

        String balanceLine = r.netBalanceTonnes() >= 0
                ? String.format(Locale.US,
                "The hall is a **net carbon sink** by **%,.0f tonnes/year** (≈ %s).",
                r.netBalanceTonnes(), impact.formatCars(balanceCars))
                : String.format(Locale.US,
                "The hall is still a **net emitter** by **%,.0f tonnes/year** (≈ %s) — DAC recovers part of the damage, not all.",
                -r.netBalanceTonnes(), impact.formatCars(balanceCars));

        return String.format(Locale.US,
                "Running these GPUs draws **~%.1f GWh/year** from the grid (≈ **%.0f MW** average IT heat × PUE %.2f). "
                        + "At the U.S. average grid, that electricity emits **%,.0f tonnes CO₂/year** (≈ %s). "
                        + "DAC net removal is **%,.0f tonnes/year** (≈ %s) — about **%.0f%% recovery** of the hall's own operational CO₂. "
                        + "%s",
                r.annualElectricityGwh(), r.avgItHeatMw(), pue,
                r.operationalCo2Tonnes(), impact.formatCars(opsCars),
                r.netRemovedTonnes(), impact.formatCars(netCars),
                r.recoveryPercent(), balanceLine);
    }
}
