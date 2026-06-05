package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FanNoiseSpectrumTest {

    @Test
    void bladePassingFrequencyMatchesFormula() {
        AcousticSpectrumConfig cfg = new AcousticSpectrumConfig();
        cfg.fanRpm = 4200;
        cfg.bladesPerFan = 7;
        assertEquals(490.0, cfg.bladePassingFrequencyHz(), 0.01);
    }

    @Test
    void spectrumProducesPositiveLevels() {
        AcousticSpectrumConfig cfg = new AcousticSpectrumConfig();
        var result = FanNoiseSpectrum.compute(cfg);
        assertTrue(result.overallDba() > 50);
        assertEquals(490.0, result.bladePassingFrequencyHz(), 0.01);
        assertEquals(ThirdOctaveBands.bandCount(), result.lwPerBand().length);
    }

    @Test
    void waveformHasExpectedLength() {
        AcousticSpectrumConfig cfg = new AcousticSpectrumConfig();
        cfg.clipDurationS = 1.0;
        cfg.sampleRateHz = 8000;
        double[] wave = FanNoiseSpectrum.synthesizeWaveform(cfg, new Random(1));
        assertEquals(8000, wave.length);
    }
}
