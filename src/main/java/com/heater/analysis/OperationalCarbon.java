package com.heater.analysis;

import com.heater.config.ConfigLoader;
import com.heater.thermal.ScenarioUtil;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

/**
 * Compares DAC net removal to CO₂ emitted powering GPU operations (grid electricity).
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
        return new RecoveryAnalysis(
                avgW / 1_000_000.0,
                kwhPerYear / 1_000_000.0,
                operationalTonnes,
                netRemovedTonnes,
                recoveryPct,
                netRemovedTonnes - operationalTonnes
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
        String balanceLine = r.netBalanceTonnes() >= 0
                ? String.format(Locale.US, "Net **carbon sink** of **%,.0f tonnes CO₂e/year**.", r.netBalanceTonnes())
                : String.format(Locale.US,
                "Still a **net emitter** of **%,.0f tonnes CO₂e/year** after DAC — partial clawback, not full offset.",
                -r.netBalanceTonnes());

        return String.format(Locale.US,
                "Facility draw **~%.0f GWh/year** (≈ %.0f MW IT heat × PUE %.2f). "
                        + "At today's U.S. grid mix (0.39 kg CO₂/kWh), GPU operations emit **%,.0f tonnes CO₂e/year**. "
                        + "DAC returns **%,.0f tonnes CO₂e/year** — **%.0f%% operational recovery**. %s "
                        + "As the grid decarbonizes, operational emissions fall but waste heat (and DAC opportunity) remain.",
                r.annualElectricityGwh(), r.avgItHeatMw(), pue,
                r.operationalCo2Tonnes(), r.netRemovedTonnes(), r.recoveryPercent(), balanceLine);
    }
}
