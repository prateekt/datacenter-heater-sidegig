package com.heater.acoustic;

/**
 * Synthetic 1/3-octave fan noise spectrum: BPF harmonics + broadband turbulence shelf.
 */
public final class FanNoiseSpectrum {

    public record SpectrumResult(
            double[] lwPerBand,
            double bladePassingFrequencyHz,
            double overallDba,
            double tonalProminenceDb
    ) {}

    private FanNoiseSpectrum() {}

    public static SpectrumResult compute(AcousticSpectrumConfig cfg) {
        int n = ThirdOctaveBands.bandCount();
        double[] lw = new double[n];
        double bpf = cfg.bladePassingFrequencyHz();

        // Scale total fan count to baseline fence-line SPL
        double fanCountFactor = 10.0 * Math.log10(Math.max(1, cfg.totalFanCount()));
        double distanceAtten = 20.0 * Math.log10(cfg.distanceToFenceM / 1.0);
        double referenceLw = cfg.baselineSplDbaAtFence + fanCountFactor - distanceAtten - 20.0;

        double tonalEnergy = 0.0;
        double broadbandEnergy = 0.0;

        for (int h = 1; h <= cfg.tonalHarmonics; h++) {
            double freq = bpf * h;
            if (freq > 8000) break;
            int band = ThirdOctaveBands.nearestBandIndex(freq);
            double harmonicLw = referenceLw - 3.0 * (h - 1) + 6.0 * Math.log10(cfg.bladesPerFan);
            lw[band] = logAdd(lw[band], harmonicLw);
            tonalEnergy += Math.pow(10.0, harmonicLw / 10.0);
        }

        for (int i = 0; i < n; i++) {
            double f = ThirdOctaveBands.CENTER_HZ[i];
            if (f >= 2000 && f <= 7000) {
                double shelf = referenceLw - 8.0 + cfg.broadbandTurbulenceFraction * 6.0;
                lw[i] = logAdd(lw[i], shelf);
                broadbandEnergy += Math.pow(10.0, shelf / 10.0);
            } else if (f >= 150 && f < 2000) {
                double mid = referenceLw - 12.0;
                lw[i] = logAdd(lw[i], mid);
            }
        }

        double tonalProminence = 10.0 * Math.log10(Math.max(1e-30, tonalEnergy / Math.max(1e-30, broadbandEnergy)));
        return new SpectrumResult(lw, bpf, ThirdOctaveBands.aWeightedLevelDb(lw), tonalProminence);
    }

    /** Generate time-domain fan noise for WAV export and MDMG input. */
    public static double[] synthesizeWaveform(AcousticSpectrumConfig cfg, java.util.Random rng) {
        int samples = (int) (cfg.sampleRateHz * cfg.clipDurationS);
        double[] x = new double[samples];
        double bpf = cfg.bladePassingFrequencyHz();
        double amp = Math.pow(10.0, (cfg.baselineSplDbaAtFence - 94.0) / 20.0);

        for (int n = 0; n < samples; n++) {
            double t = n / (double) cfg.sampleRateHz;
            double v = 0.0;
            for (int h = 1; h <= cfg.tonalHarmonics; h++) {
                double phase = rng.nextDouble() * 2 * Math.PI;
                v += amp * Math.pow(0.7, h - 1) * Math.sin(2 * Math.PI * bpf * h * t + phase);
            }
            v += amp * cfg.broadbandTurbulenceFraction * (rng.nextDouble() * 2 - 1);
            x[n] = v;
        }
        normalize(x, 0.85);
        return x;
    }

    private static double logAdd(double existing, double addition) {
        if (existing <= -200) return addition;
        return 10.0 * Math.log10(Math.pow(10.0, existing / 10.0) + Math.pow(10.0, addition / 10.0));
    }

    private static void normalize(double[] x, double peak) {
        double max = 0.0;
        for (double v : x) max = Math.max(max, Math.abs(v));
        if (max > 1e-12) {
            double scale = peak / max;
            for (int i = 0; i < x.length; i++) x[i] *= scale;
        }
    }
}
