package com.heater.acoustic;

import java.util.List;

/** Standard 1/3-octave center frequencies (Hz) for lumped acoustic modeling. */
public final class ThirdOctaveBands {

    public static final double[] CENTER_HZ = {
            50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800,
            1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000
    };

    /** A-weighting (dB) at each center frequency — simplified IEC 61672 table. */
    public static final double[] A_WEIGHT_DB = {
            -30.2, -26.2, -22.5, -19.1, -16.1, -13.4, -10.9, -8.6, -6.6, -4.8, -3.2, -1.9, -0.8,
            0.0, 0.6, 1.0, 1.2, 1.3, 1.2, 0.5, -0.1, -1.1, -2.5
    };

    private ThirdOctaveBands() {}

    public static int bandCount() {
        return CENTER_HZ.length;
    }

    public static int nearestBandIndex(double frequencyHz) {
        int best = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < CENTER_HZ.length; i++) {
            double d = Math.abs(Math.log(frequencyHz / CENTER_HZ[i]));
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    public static double sumPower(double[] lwPerBand) {
        double sum = 0.0;
        for (double lw : lwPerBand) {
            sum += Math.pow(10.0, lw / 10.0);
        }
        return sum;
    }

    public static double overallLevelDb(double[] lwPerBand) {
        return 10.0 * Math.log10(Math.max(1e-30, sumPower(lwPerBand)));
    }

    public static double aWeightedLevelDb(double[] lwPerBand) {
        double sum = 0.0;
        for (int i = 0; i < lwPerBand.length; i++) {
            sum += Math.pow(10.0, (lwPerBand[i] + A_WEIGHT_DB[i]) / 10.0);
        }
        return 10.0 * Math.log10(Math.max(1e-30, sum));
    }

    public static double[] copy(double[] src) {
        double[] out = new double[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    public static double[] addIncoherent(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = 10.0 * Math.log10(
                    Math.pow(10.0, a[i] / 10.0) + Math.pow(10.0, b[i] / 10.0));
        }
        return out;
    }

    public static double[] subtractAttenuation(double[] lw, double[] attenDb) {
        double[] out = new double[lw.length];
        for (int i = 0; i < lw.length; i++) {
            out[i] = lw[i] - attenDb[i];
        }
        return out;
    }

    public static List<Double> asList(double[] arr) {
        return java.util.Arrays.stream(arr).boxed().toList();
    }
}
