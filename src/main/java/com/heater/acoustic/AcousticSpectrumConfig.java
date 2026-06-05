package com.heater.acoustic;

import com.heater.config.ConfigLoader;

import java.util.Map;

@SuppressWarnings("unchecked")
public final class AcousticSpectrumConfig {

    public double fanRpm = 4200;
    public int bladesPerFan = 7;
    public int fansPerRack = 6;
    public int rackCount = 2500;
    public double distanceToFenceM = 150;
    public double baselineSplDbaAtFence = 92.0;
    public int tonalHarmonics = 6;
    public double broadbandTurbulenceFraction = 0.35;
    public int sampleRateHz = 44100;
    public double clipDurationS = 4.0;

    public static AcousticSpectrumConfig fromMap(Map<String, Object> root) {
        Map<String, Object> m = ConfigLoader.map(root, "acoustic_spectrum");
        AcousticSpectrumConfig c = new AcousticSpectrumConfig();
        c.fanRpm = ConfigLoader.d(m, "fan_rpm", c.fanRpm);
        c.bladesPerFan = (int) ConfigLoader.d(m, "blades_per_fan", c.bladesPerFan);
        c.fansPerRack = (int) ConfigLoader.d(m, "fans_per_rack", c.fansPerRack);
        c.rackCount = (int) ConfigLoader.d(m, "rack_count", c.rackCount);
        c.distanceToFenceM = ConfigLoader.d(m, "distance_to_fence_m", c.distanceToFenceM);
        c.baselineSplDbaAtFence = ConfigLoader.d(m, "baseline_spl_dba_at_fence", c.baselineSplDbaAtFence);
        c.tonalHarmonics = (int) ConfigLoader.d(m, "tonal_harmonics", c.tonalHarmonics);
        c.broadbandTurbulenceFraction = ConfigLoader.d(m, "broadband_turbulence_fraction", c.broadbandTurbulenceFraction);
        c.sampleRateHz = (int) ConfigLoader.d(m, "sample_rate_hz", c.sampleRateHz);
        c.clipDurationS = ConfigLoader.d(m, "clip_duration_s", c.clipDurationS);
        return c;
    }

    public double bladePassingFrequencyHz() {
        return bladesPerFan * fanRpm / 60.0;
    }

    public int totalFanCount() {
        return fansPerRack * rackCount;
    }
}
