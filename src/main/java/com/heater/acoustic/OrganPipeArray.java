package com.heater.acoustic;

/**
 * Organ-pipe bank excited by exhaust/chimney airflow — reallocates BPF energy to musical partials.
 */
public final class OrganPipeArray {

    public record PipeResult(
            double[] lwPerBand,
            double musicalTonalContentDb,
            double airflowUtilization
    ) {}

    private OrganPipeArray() {}

    public static PipeResult compute(
            MechanicalEqualizerConfig cfg,
            double volumeFlowM3S,
            double[] sourceLwAfterAtten,
            double bpfHz
    ) {
        int n = ThirdOctaveBands.bandCount();
        double[] lw = new double[n];
        if (!cfg.organPipesEnabled || cfg.pipeCount <= 0) {
            return new PipeResult(lw, -200, 0);
        }

        double flowFactor = Math.min(1.0, volumeFlowM3S / 50.0);
        if (flowFactor <= 0 && !cfg.thermalCouplingEnabled) {
            flowFactor = 0.15;
        }

        double pipeLw = 48.0 + 10.0 * Math.log10(Math.max(0.01, flowFactor * cfg.pipeCount));

        for (int p = 0; p < 4; p++) {
            double freq = cfg.pipeFundamentalHz * (p + 1);
            if (freq > 8000) break;
            int band = ThirdOctaveBands.nearestBandIndex(freq);
            lw[band] = logAdd(lw[band], pipeLw - 3.0 * p);
        }

        double reallocated = 0.0;
        for (int h = 1; h <= 3; h++) {
            int srcBand = ThirdOctaveBands.nearestBandIndex(bpfHz * h);
            if (srcBand < sourceLwAfterAtten.length) {
                reallocated += Math.pow(10.0, sourceLwAfterAtten[srcBand] / 10.0) * 0.05 * flowFactor;
            }
        }
        double musicalContent = 10.0 * Math.log10(Math.max(1e-30, ThirdOctaveBands.sumPower(lw) + reallocated));
        return new PipeResult(lw, musicalContent, flowFactor);
    }

    private static double logAdd(double a, double b) {
        if (a <= -200) return b;
        return 10.0 * Math.log10(Math.pow(10.0, a / 10.0) + Math.pow(10.0, b / 10.0));
    }
}
