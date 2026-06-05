package com.heater.acoustic;

/**
 * Lorentzian absorption peaks per 1/3-octave band — SeMSA-style metamaterial bank cartoon.
 */
public final class MetamaterialBank {

    public record AttenuationResult(
            double[] attenuationDbPerBand,
            double addedPressureDropPa,
            double addedFanPowerW
    ) {}

    private MetamaterialBank() {}

    public static AttenuationResult compute(MechanicalEqualizerConfig cfg, double bpfHz) {
        int n = ThirdOctaveBands.bandCount();
        double[] atten = new double[n];

        if (cfg.linerDepthMm <= 0 || cfg.resonatorCellCount <= 0) {
            return new AttenuationResult(atten, 0, 0);
        }

        double depthFactor = Math.min(1.0, cfg.linerDepthMm / 15.0);
        double cellFactor = Math.min(1.0, cfg.resonatorCellCount / 48.0);
        double semsaAnchorDb = 2.5 * depthFactor * cellFactor;

        for (int i = 0; i < n; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            double lorentz = lorentzianDb(f, bpfHz, bpfHz * 0.15, cfg.maxAttenuationDbPerBand);
            double broadband = cfg.maxAttenuationDbPerBand * 0.35 * depthFactor
                    * Math.exp(-Math.pow((f - 2000) / 2500.0, 2));
            atten[i] = Math.min(cfg.maxAttenuationDbPerBand, lorentz + broadband + semsaAnchorDb * 0.15);
        }

        double deltaP = cfg.linerDepthMm * cfg.pressureDropPaPerMm * (1.0 + 0.5 * cellFactor);
        double fanW = fanPower(cfg.baselineFanAirflowKgS, deltaP, cfg.airDensityKgM3, cfg.fanEfficiency);
        return new AttenuationResult(atten, deltaP, fanW);
    }

    private static double lorentzianDb(double f, double f0, double width, double peakDb) {
        double x = (f - f0) / Math.max(1.0, width);
        return peakDb / (1.0 + x * x);
    }

    private static double fanPower(double mdot, double deltaPa, double rho, double efficiency) {
        if (mdot <= 0 || deltaPa <= 0 || efficiency <= 0) return 0;
        return mdot * deltaPa / (rho * efficiency);
    }
}
