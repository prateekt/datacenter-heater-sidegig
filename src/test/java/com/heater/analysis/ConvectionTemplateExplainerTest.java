package com.heater.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvectionTemplateExplainerTest {

    @Test
    void explainIncludesKindergartenSections() throws Exception {
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(
                "config/convection_sweep.yaml",
                "config/passive_convection_capture.yaml"
        );
        ConvectionResultsSummary summary = analyzer.runAll();
        ConvectionAnalogies analogies = ConvectionAnalogies.load("config/convection_analogies.yaml");
        ConvectionLiterature literature = ConvectionLiterature.load("config/convection_references.yaml");
        String markdown = ConvectionTemplateExplainer.explain(summary, analogies, literature);

        assertTrue(markdown.contains("In one sentence"));
        assertTrue(markdown.contains("Picture this"));
        assertTrue(markdown.contains("plain English"));
        assertTrue(markdown.contains("published research"));
        assertTrue(markdown.contains("doi.org/10.1016/j.joule.2018.05.006"));
        assertTrue(markdown.contains("Honest limits"));
        assertTrue(markdown.toLowerCase().contains("speculative"));

        var validation = ConvectionExplainerValidator.validate(markdown, summary);
        assertTrue(validation.warnings().isEmpty(), () -> String.join(", ", validation.warnings()));
    }
}
