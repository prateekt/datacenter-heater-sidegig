package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GpuProfileTest {

    @Test
    void loadsProfilesAndComputesGpuCount() throws Exception {
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load("config/gpu_profiles.yaml");
        GpuProfile h100 = registry.require("H100_SXM");
        assertEquals(950, h100.systemWasteWPerGpu(), 1.0);

        int gpus = h100.gpuCountForWasteHeat(23_750_000);
        assertTrue(gpus >= 24_000 && gpus <= 25_500, "25k H100s ≈ 23.75 MW");

        GpuProfile b200 = registry.referenceProfile();
        assertEquals("B200_LC", b200.id());
        assertEquals(25_000, registry.referenceGpuCount());
        assertEquals(33.75, b200.avgWasteHeatMw(registry.referenceGpuCount()), 0.5);
    }
}
