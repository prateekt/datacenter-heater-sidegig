package com.heater.acoustic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetamaterialBankTest {

    @Test
    void zeroDepthProducesNoAttenuationOrFanPenalty() {
        MechanicalEqualizerConfig cfg = new MechanicalEqualizerConfig();
        cfg.linerDepthMm = 0;
        var result = MetamaterialBank.compute(cfg, 490.0);
        for (double a : result.attenuationDbPerBand()) {
            assertEquals(0.0, a, 1e-9);
        }
        assertEquals(0.0, result.addedFanPowerW(), 1e-9);
    }

    @Test
    void deeperLinerIncreasesFanPowerPenalty() {
        MechanicalEqualizerConfig shallow = new MechanicalEqualizerConfig();
        shallow.linerDepthMm = 15;
        MechanicalEqualizerConfig deep = new MechanicalEqualizerConfig();
        deep.linerDepthMm = 50;

        var shallowResult = MetamaterialBank.compute(shallow, 490.0);
        var deepResult = MetamaterialBank.compute(deep, 490.0);

        assertTrue(deepResult.addedPressureDropPa() > shallowResult.addedPressureDropPa());
        assertTrue(deepResult.addedFanPowerW() > shallowResult.addedFanPowerW());
    }
}
