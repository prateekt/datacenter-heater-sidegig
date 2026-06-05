package com.heater.analysis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeatApplicationAnalyzerTest {

    @Test
    void analyzesThreeApplicationScenarios() throws Exception {
        GpuProfile.GpuProfileRegistry registry = GpuProfile.load("config/gpu_profiles.yaml");
        List<HeatApplicationPoint> points = HeatApplicationAnalyzer.analyzeAll(
                "config/heat_applications.yaml",
                "config/nvidia_us_expansion.yaml",
                registry,
                3600.0
        );
        assertEquals(4, points.size());
        HeatApplicationPoint dac = points.stream().filter(p -> "dac_priority".equals(p.scenarioId())).findFirst().orElseThrow();
        HeatApplicationPoint community = points.stream().filter(p -> "community_heat".equals(p.scenarioId())).findFirst().orElseThrow();
        HeatApplicationPoint plastic = points.stream()
                .filter(p -> "plastic_recycling_priority".equals(p.scenarioId())).findFirst().orElseThrow();
        assertTrue(dac.netCo2eTonnesPerYear() > 0);
        assertTrue(dac.heatDacMwh() > community.heatDacMwh());
        assertTrue(community.heatPoolMwh() + community.heatAquacultureMwh() > 0);
        assertTrue(plastic.heatPlasticMwh() > 0);
        assertTrue(plastic.petTonnesEquivalent() > 0);
        assertTrue(plastic.heatDacMwh() < dac.heatDacMwh());
        assertTrue(dac.hotShowersEquivalent() > 1_000_000, "DAC heat should equate to millions of shelter showers");
        assertEquals(dac.heatTotalMwh() * 1000.0 / 2.5, dac.hotShowersEquivalent(), dac.heatTotalMwh() * 0.01);
        assertEquals(0.0, dac.heatPoolMwh(), 1.0, "DAC priority does not route to pools");
        assertEquals(0.0, dac.heatAquacultureMwh(), 1.0);
        assertEquals(0.0, dac.heatAlgaeMwh(), 1.0);
        assertEquals(0.0, dac.algaeHectaresEquivalent(), 0.01, "algae ha from heat, not configured pond size");
        assertEquals(0.0, dac.olympicPoolsEquivalent(), 0.01);
    }
}
