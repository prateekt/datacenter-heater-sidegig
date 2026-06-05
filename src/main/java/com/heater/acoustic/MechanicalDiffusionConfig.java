package com.heater.acoustic;

import com.heater.config.ConfigLoader;

import java.util.Arrays;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class MechanicalDiffusionConfig {

    public boolean enabled = true;
    public int oscillatorCount = 64;
    public int reverseSteps = 40;
    public double dtS = 0.001;
    public double damping = 0.15;
    public double couplingStrength = 0.08;
    public double noiseScheduleStart = 0.5;
    public double noiseScheduleEnd = 0.02;
    public double pinkNoiseWeight = 0.4;
    public double[] harmonicFrequenciesHz = {220, 330, 440, 554, 659};
    public double[] harmonicAmplitudes = {1.0, 0.6, 0.45, 0.35, 0.25};
    public int sampleRateHz = 44100;
    public double clipDurationS = 4.0;

    public static MechanicalDiffusionConfig fromMap(Map<String, Object> root) {
        Map<String, Object> m = ConfigLoader.map(root, "mechanical_diffusion");
        MechanicalDiffusionConfig c = new MechanicalDiffusionConfig();
        c.enabled = bool(m, "enabled", true);
        c.oscillatorCount = (int) ConfigLoader.d(m, "oscillator_count", c.oscillatorCount);
        c.reverseSteps = (int) ConfigLoader.d(m, "reverse_steps", c.reverseSteps);
        c.dtS = ConfigLoader.d(m, "dt_s", c.dtS);
        c.damping = ConfigLoader.d(m, "damping", c.damping);
        c.couplingStrength = ConfigLoader.d(m, "coupling_strength", c.couplingStrength);
        c.noiseScheduleStart = ConfigLoader.d(m, "noise_schedule_start", c.noiseScheduleStart);
        c.noiseScheduleEnd = ConfigLoader.d(m, "noise_schedule_end", c.noiseScheduleEnd);
        c.sampleRateHz = (int) ConfigLoader.d(m, "sample_rate_hz", c.sampleRateHz);
        c.clipDurationS = ConfigLoader.d(m, "clip_duration_s", c.clipDurationS);

        Map<String, Object> tmpl = ConfigLoader.map(m, "template");
        c.pinkNoiseWeight = ConfigLoader.d(tmpl, "pink_noise_weight", c.pinkNoiseWeight);
        c.harmonicFrequenciesHz = doubleArray(tmpl, "harmonic_frequencies_hz", c.harmonicFrequenciesHz);
        c.harmonicAmplitudes = doubleArray(tmpl, "harmonic_amplitudes", c.harmonicAmplitudes);
        return c;
    }

    private static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean b ? b : def;
    }

    private static double[] doubleArray(Map<String, Object> m, String key, double[] def) {
        Object v = m.get(key);
        if (v instanceof java.util.List<?> list) {
            return list.stream().mapToDouble(o -> ((Number) o).doubleValue()).toArray();
        }
        return Arrays.copyOf(def, def.length);
    }
}
