package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TemplateExplainerTest {

    @Test
    void outputsFirstStructureWithThermalChartsAndAppendix() throws Exception {
        SweepRunner runner = new SweepRunner(
                "config/nvidia_us_expansion.yaml",
                "src/test/resources/scalability_sweep_fast.yaml");
        ResultsSummary summary = runner.runAll();
        String markdown = TemplateExplainer.explain(summary, "config/gpu_profiles.yaml");

        assertTrue(markdown.contains("side gig"));
        assertTrue(markdown.contains("output-side thermodynamic potential"));
        assertTrue(markdown.contains("Interpretation"));
        assertTrue(markdown.contains("Closing synthesis"));
        assertTrue(markdown.contains("thermal_service_vs_gpu_count.png"));
        assertTrue(markdown.contains("Appendix: Grid-dependent carbon scenario"));
        assertTrue(markdown.contains("co2_vs_gpu_count.png"));
        assertTrue(markdown.contains("GWh/yr"));
        assertFalse(markdown.indexOf("GWh/yr") > markdown.indexOf("Appendix: Grid-dependent"),
                "Thermal GWh should appear before grid appendix");
        assertTrue(markdown.contains("capture yield"), "Narratives should show capture yield %");
        assertTrue(markdown.contains("linear extrapolation"), "Multi-hall caveat should be documented");
        assertTrue(markdown.contains("250,000 (25,000/hall)"),
                "10-hall table row should show total GPU count");
        assertTrue(markdown.contains("10 halls (×10 linear extrapolation)"),
                "10-hall narrative should document scaling method");
    }
}
