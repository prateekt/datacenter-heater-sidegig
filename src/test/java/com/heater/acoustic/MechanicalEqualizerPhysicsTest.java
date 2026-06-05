package com.heater.acoustic;

import com.heater.carbon.ConvectionCaptureConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MechanicalEqualizerPhysicsTest {

    @Test
    void thermalCouplingIncreasesOrganPipeContribution() throws Exception {
        AcousticSpectrumConfig spec = AcousticSpectrumConfig.fromMap(
                com.heater.config.ConfigLoader.load("config/acoustic_spectrum.yaml"));

        MechanicalEqualizerConfig decoupled = MechanicalEqualizerConfig.fromMap(
                com.heater.config.ConfigLoader.load("config/mechanical_equalizer.yaml"));
        decoupled.thermalCouplingEnabled = false;

        MechanicalEqualizerConfig coupled = MechanicalEqualizerConfig.fromMap(
                com.heater.config.ConfigLoader.load("config/mechanical_equalizer.yaml"));
        coupled.thermalCouplingEnabled = true;
        coupled.useChimneyDraft = true;

        ConvectionCaptureConfig conv = ConvectionCaptureConfig.fromYaml(
                com.heater.config.ConfigLoader.load("config/passive_convection_capture.yaml"));
        double qWaste = 39_523_125.0;
        var draft = MechanicalEqualizerPhysics.resolveDraft(coupled, conv, qWaste, 22.0);

        var rDec = MechanicalEqualizerPhysics.solve(spec, decoupled, null);
        var rCou = MechanicalEqualizerPhysics.solve(spec, coupled, draft);

        assertNotNull(draft);
        assertTrue(draft.volumeFlowM3S() > 0);
        assertTrue(rCou.volumeFlowM3S() > rDec.volumeFlowM3S());
    }
}
