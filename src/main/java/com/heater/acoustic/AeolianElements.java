package com.heater.acoustic;

/**
 * Aeolian chimes — perimeter elements excited by airflow.
 */
public final class AeolianElements {

    public record AeolianResult(double[] lwPerBand, double excitationLevelDb) {}

    private AeolianElements() {}

    public static AeolianResult compute(MechanicalEqualizerConfig cfg, double perimeterWindMps) {
        int n = ThirdOctaveBands.bandCount();
        double[] lw = new double[n];
        if (!cfg.aeolianEnabled || cfg.aeolianElementCount <= 0) {
            return new AeolianResult(lw, -200);
        }

        double wind = Math.max(0.5, perimeterWindMps);
        double base = 42.0 + 10.0 * Math.log10(cfg.aeolianElementCount) + 20.0 * Math.log10(wind / 2.0);

        for (int i = 0; i < n; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            if (f >= 500 && f <= 4000) {
                lw[i] = base - 8.0 * Math.abs(Math.log(f / 1000.0));
            }
        }
        return new AeolianResult(lw, ThirdOctaveBands.overallLevelDb(lw));
    }
}
