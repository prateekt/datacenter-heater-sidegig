package com.heater.acoustic;

public final class SoundscapeMetrics {

    public record MetricsResult(
            double fenceLineDba,
            double baselineDba,
            double reductionDba,
            double tonalProminenceDb,
            double soundscapeQualityIndex,
            double annoyanceProxy,
            double natureBandEnergyDb
    ) {}

    private SoundscapeMetrics() {}

    public static MetricsResult compute(
            double[] baselineLw,
            double[] combinedLw,
            double baselineTonalProminence
    ) {
        double baselineDba = ThirdOctaveBands.aWeightedLevelDb(baselineLw);
        double fenceDba = ThirdOctaveBands.aWeightedLevelDb(combinedLw);
        double reduction = baselineDba - fenceDba;

        double tonalEnergy = 0.0;
        double totalEnergy = ThirdOctaveBands.sumPower(combinedLw);
        for (int i = 0; i < combinedLw.length; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            if (f >= 150 && f <= 1500) {
                tonalEnergy += Math.pow(10.0, combinedLw[i] / 10.0);
            }
        }
        double tonalProminence = 10.0 * Math.log10(Math.max(1e-30, tonalEnergy / Math.max(1e-30, totalEnergy - tonalEnergy)));

        double natureEnergy = 0.0;
        for (int i = 0; i < combinedLw.length; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            if (f >= 400 && f <= 2500) {
                natureEnergy += Math.pow(10.0, combinedLw[i] / 10.0);
            }
        }
        double natureBandDb = 10.0 * Math.log10(Math.max(1e-30, natureEnergy));

        double tonalImprovement = Math.max(0, baselineTonalProminence - tonalProminence);
        double sqi = clamp(0.35 * reduction + 0.25 * tonalImprovement + 0.40 * (natureBandDb / 70.0), 0, 1);

        double annoyance = clamp(0.5 + (fenceDba - 55) / 40.0 - 0.3 * sqi, 0, 1);

        return new MetricsResult(fenceDba, baselineDba, reduction, tonalProminence, sqi, annoyance, natureBandDb);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
