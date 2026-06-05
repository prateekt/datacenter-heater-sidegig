package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MechanicalDiffusionPhysicsTest {

    @Test
    void denoiseProducesStableOutput() {
        MechanicalDiffusionConfig cfg = new MechanicalDiffusionConfig();
        cfg.oscillatorCount = 32;
        cfg.reverseSteps = 20;
        cfg.clipDurationS = 0.5;
        cfg.sampleRateHz = 8000;

        AcousticSpectrumConfig spec = new AcousticSpectrumConfig();
        spec.clipDurationS = 0.5;
        spec.sampleRateHz = 8000;
        double[] input = FanNoiseSpectrum.synthesizeWaveform(spec, new Random(7));

        var result = MechanicalDiffusionPhysics.denoise(cfg, input, new Random(7));
        assertEquals(input.length, result.outputWaveform().length);
        assertFalse(Double.isNaN(result.harmonicity()));
        assertFalse(Double.isInfinite(result.spectralDistanceToTemplate()));
        for (double v : result.outputWaveform()) {
            assertFalse(Double.isNaN(v));
            assertTrue(Math.abs(v) <= 1.0);
        }
    }

    @Test
    void moreStepsDoNotExplodeEnergy() {
        MechanicalDiffusionConfig few = new MechanicalDiffusionConfig();
        few.reverseSteps = 5;
        few.oscillatorCount = 16;
        few.clipDurationS = 0.25;
        few.sampleRateHz = 4000;

        MechanicalDiffusionConfig many = new MechanicalDiffusionConfig();
        many.reverseSteps = 80;
        many.oscillatorCount = 16;
        many.clipDurationS = 0.25;
        many.sampleRateHz = 4000;

        double[] input = new double[1000];
        Random rng = new Random(3);
        for (int i = 0; i < input.length; i++) input[i] = rng.nextGaussian() * 0.1;

        var rFew = MechanicalDiffusionPhysics.denoise(few, input, new Random(3));
        var rMany = MechanicalDiffusionPhysics.denoise(many, input, new Random(3));
        assertTrue(rMany.energyDissipated() >= rFew.energyDissipated());
    }
}
