package com.heater.analysis;

import com.heater.config.ConfigLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FacilityScalerTest {

    private static Map<String, Object> baseConfig;
    private static GpuProfile.GpuProfileRegistry registry;

    @BeforeAll
    static void setup() throws Exception {
        baseConfig = ConfigLoader.load("config/nvidia_us_expansion.yaml");
        registry = GpuProfile.load("config/gpu_profiles.yaml");
    }

    @Test
    void doublesGpusDoublesWasteAndPlant() {
        GpuProfile profile = registry.referenceProfile();
        FacilityScaler.ScaledFacility one = FacilityScaler.scale(
                baseConfig, "nvidia_us_module", profile, 37_000, registry,
                FacilityScaler.ScaleMode.PROPORTIONAL, 3600);
        FacilityScaler.ScaledFacility two = FacilityScaler.scale(
                baseConfig, "nvidia_us_module", profile, 74_000, registry,
                FacilityScaler.ScaleMode.PROPORTIONAL, 3600);

        assertEquals(two.scenario().qWasteBase(), one.scenario().qWasteBase() * 2, one.scenario().qWasteBase() * 0.01);
        double hpOne = ConfigLoader.d(ConfigLoader.map(ConfigLoader.map(one.config(), "loads"), "carbon_capture"), "hp_capacity_w", 0);
        double hpTwo = ConfigLoader.d(ConfigLoader.map(ConfigLoader.map(two.config(), "loads"), "carbon_capture"), "hp_capacity_w", 0);
        assertEquals(2.0, hpTwo / hpOne, 0.01);
    }
}
