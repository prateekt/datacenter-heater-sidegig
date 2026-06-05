package com.heater.acoustic;

import java.util.Random;

/**
 * Scheme 2 — Mechanical Diffusion Music Generator (MDMG).
 * Overdamped Langevin dynamics on coupled oscillators toward a fixed music template landscape.
 */
public final class MechanicalDiffusionPhysics {

    public record DiffusionResult(
            double[] outputWaveform,
            double spectralDistanceToTemplate,
            double harmonicity,
            double roughnessProxy,
            double energyDissipated,
            int reverseSteps
    ) {}

    private MechanicalDiffusionPhysics() {}

    public static DiffusionResult denoise(
            MechanicalDiffusionConfig cfg,
            double[] fanNoise,
            Random rng
    ) {
        int n = Math.min(cfg.oscillatorCount, fanNoise.length);
        double[] x = new double[n];
        System.arraycopy(fanNoise, 0, x, 0, n);

        double[][] coupling = buildCoupling(n, cfg.couplingStrength);
        double energyDissipated = 0.0;

        for (int step = 0; step < cfg.reverseSteps; step++) {
            double t = step / (double) Math.max(1, cfg.reverseSteps - 1);
            double sigma = cfg.noiseScheduleStart
                    + t * (cfg.noiseScheduleEnd - cfg.noiseScheduleStart);

            double[] grad = gradient(x, coupling, cfg);
            for (int i = 0; i < n; i++) {
                double noise = rng.nextGaussian() * sigma * 0.01;
                double dx = -cfg.damping * grad[i] * cfg.dtS + noise;
                x[i] += dx;
                energyDissipated += Math.abs(dx * grad[i]);
            }
        }

        double[] output = upsampleToFull(x, fanNoise.length);
        applyTemplateHarmonics(output, cfg, fanNoise.length / cfg.sampleRateHz);

        double specDist = spectralDistance(output, cfg);
        double harmonicity = computeHarmonicity(output, cfg.sampleRateHz);
        double roughness = computeRoughness(output);

        return new DiffusionResult(output, specDist, harmonicity, roughness, energyDissipated, cfg.reverseSteps);
    }

    private static double[][] buildCoupling(int n, double strength) {
        double[][] k = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double w = strength * Math.exp(-Math.abs(i - j) / 8.0);
                k[i][j] = w;
                k[j][i] = w;
            }
        }
        return k;
    }

    private static double[] gradient(double[] x, double[][] coupling, MechanicalDiffusionConfig cfg) {
        int n = x.length;
        double[] g = new double[n];

        for (int i = 0; i < n; i++) {
            double spring = x[i];
            for (int j = 0; j < n; j++) {
                if (i != j) spring += coupling[i][j] * (x[i] - x[j]);
            }
            g[i] = spring;
        }

        for (int h = 0; h < cfg.harmonicFrequenciesHz.length; h++) {
            double target = cfg.harmonicAmplitudes[h] * 0.02;
            int idx = Math.min(n - 1, (int) (h * n / (double) cfg.harmonicFrequenciesHz.length));
            g[idx] += cfg.pinkNoiseWeight * (x[idx] - target);
        }
        return g;
    }

    private static double[] upsampleToFull(double[] x, int targetLen) {
        double[] out = new double[targetLen];
        for (int i = 0; i < targetLen; i++) {
            double src = i * (x.length - 1.0) / Math.max(1, targetLen - 1);
            int lo = (int) Math.floor(src);
            int hi = Math.min(x.length - 1, lo + 1);
            double frac = src - lo;
            out[i] = x[lo] * (1 - frac) + x[hi] * frac;
        }
        return out;
    }

    private static void applyTemplateHarmonics(double[] x, MechanicalDiffusionConfig cfg, double durationHint) {
        int sr = cfg.sampleRateHz;
        for (int i = 0; i < x.length; i++) {
            double t = i / (double) sr;
            double harmonic = 0.0;
            for (int h = 0; h < cfg.harmonicFrequenciesHz.length; h++) {
                harmonic += cfg.harmonicAmplitudes[h] * 0.05
                        * Math.sin(2 * Math.PI * cfg.harmonicFrequenciesHz[h] * t);
            }
            x[i] = 0.6 * x[i] + 0.4 * harmonic;
        }
        normalize(x, 0.85);
    }

    private static double spectralDistance(double[] x, MechanicalDiffusionConfig cfg) {
        double fanEnergy = bandEnergy(x, cfg.sampleRateHz, 1000, 4000);
        double templateEnergy = 0.5;
        return Math.abs(10.0 * Math.log10(Math.max(1e-30, fanEnergy / templateEnergy)));
    }

    private static double bandEnergy(double[] x, int sr, double fLo, double fHi) {
        double sum = 0.0;
        for (int i = 1; i < x.length / 2; i++) {
            double f = i * sr / (double) x.length;
            if (f >= fLo && f <= fHi) sum += x[i] * x[i];
        }
        return sum;
    }

    private static double computeHarmonicity(double[] x, int sr) {
        double fundamental = 220.0;
        int period = Math.max(1, (int) Math.round(sr / fundamental));
        if (period >= x.length) return 0;
        double corr = 0.0;
        double energy = 0.0;
        for (int i = period; i < x.length; i++) {
            corr += x[i] * x[i - period];
            energy += x[i] * x[i];
        }
        return Math.max(0.0, corr / Math.max(1e-30, energy));
    }

    private static double computeRoughness(double[] x) {
        double diff = 0.0;
        for (int i = 1; i < x.length; i++) {
            diff += Math.abs(x[i] - x[i - 1]);
        }
        return diff / x.length;
    }

    private static void normalize(double[] x, double peak) {
        double max = 0.0;
        for (double v : x) max = Math.max(max, Math.abs(v));
        if (max > 1e-12) {
            double s = peak / max;
            for (int i = 0; i < x.length; i++) x[i] *= s;
        }
    }
}
