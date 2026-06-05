package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GpuProfileTest {

    @Test
    void loadsProfilesAndComputesGpuCount() throws Exception {
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load("config/gpu_profiles.yaml");
        GpuProfile h100 = registry.require("H100_SXM");
        assertEquals(950, h100.systemWasteWPerGpu(), 1.0);

        int gpus = h100.gpuCountForWasteHeat(35_000_000);
        assertTrue(gpus >= 36_000 && gpus <= 37_000, "37k H100s ≈ 35 MW");

        GpuProfile b200 = registry.referenceProfile();
        assertEquals("B200_LC", b200.id());
        assertEquals(37_000, registry.referenceGpuCount());
        assertEquals(50.0, b200.avgWasteHeatMw(registry.referenceGpuCount()), 1.0);
    }
}
