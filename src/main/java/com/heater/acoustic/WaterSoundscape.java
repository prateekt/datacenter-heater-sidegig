package com.heater.acoustic;

/**
 * Parametric water-feature spectrum (Galbrun & Ali simplified).
 */
public final class WaterSoundscape {

    public record WaterResult(
            double[] lwPerBand,
            double overallDba,
            double maskingZoneWidthDb
    ) {}

    private WaterSoundscape() {}

    public static WaterResult compute(MechanicalEqualizerConfig cfg, double airflowBoostFactor) {
        int n = ThirdOctaveBands.bandCount();
        double[] lw = new double[n];
        if (!cfg.waterEnabled || cfg.waterFlowLS <= 0) {
            return new WaterResult(lw, -200, 0);
        }

        double flow = cfg.waterFlowLS * airflowBoostFactor;
        double baseLw = 55.0 + 10.0 * Math.log10(Math.max(0.1, flow));

        for (int i = 0; i < n; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            double bandLw = switch (cfg.waterType) {
                case "natural_stream" -> streamSpectrum(f, baseLw);
                case "cascade" -> cascadeSpectrum(f, baseLw);
                default -> fountainSpectrum(f, baseLw, cfg.jetHeightM);
            };
            lw[i] = bandLw;
        }

        double overall = ThirdOctaveBands.aWeightedLevelDb(lw);
        double maskingWidth = Math.max(0, overall - 3.0);
        return new WaterResult(lw, overall, maskingWidth);
    }

    private static double fountainSpectrum(double f, double baseLw, double jetHeight) {
        double midPeak = baseLw + 4.0 * Math.exp(-Math.pow((f - 800) / 600.0, 2));
        double high = baseLw - 6.0 + jetHeight * 2.0 * Math.exp(-Math.pow((f - 2500) / 1500.0, 2));
        return 10.0 * Math.log10(
                Math.pow(10.0, midPeak / 10.0) + Math.pow(10.0, high / 10.0));
    }

    private static double streamSpectrum(double f, double baseLw) {
        return baseLw - 4.0 + 3.0 * Math.exp(-Math.pow((f - 400) / 350.0, 2));
    }

    private static double cascadeSpectrum(double f, double baseLw) {
        return baseLw - 2.0 + 5.0 * Math.exp(-Math.pow((f - 1200) / 800.0, 2));
    }
}
