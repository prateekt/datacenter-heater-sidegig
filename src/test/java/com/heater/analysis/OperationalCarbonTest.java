package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationalCarbonTest {

    @Test
    void recoveryIsPartialForReferenceHall() throws Exception {
        OperationalCarbon ops = OperationalCarbon.fromConfig();
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load("config/gpu_profiles.yaml");
        GpuProfile b200 = registry.require("B200_LC");

        double qBase = 25_000 * b200.systemWasteWPerGpu() * registry.baseUtilizationFactor();
        double qPeak = 25_000 * b200.systemWasteWPerGpu() * registry.peakUtilizationFactor();
        OperationalCarbon.RecoveryAnalysis r = ops.analyze(qBase, qPeak, 37_776.0);

        assertTrue(r.operationalCo2Tonnes() > 90_000, "GPU ops should emit substantial CO2");
        assertTrue(r.recoveryPercent() > 25 && r.recoveryPercent() < 45,
                "DAC should recover a minority fraction, got " + r.recoveryPercent());
        assertTrue(r.netBalanceTonnes() < 0, "Hall should remain net emitter");
    }
}
