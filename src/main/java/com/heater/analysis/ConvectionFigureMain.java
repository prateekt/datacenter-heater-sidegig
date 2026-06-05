package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class ConvectionFigureMain {

    private ConvectionFigureMain() {}

    public static void main(String[] args) throws Exception {
        String sweepPath = "config/convection_sweep.yaml";
        Path figuresDir = Path.of("docs/figures");
        Path resultsPath = Path.of("docs/convection_summary.json");
        Path readmePath = Path.of("README.md");
        boolean patchReadme = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sweep" -> sweepPath = args[++i];
                case "--no-readme" -> patchReadme = false;
                default -> { }
            }
        }

        Map<String, Object> sweepCfg = ConfigLoader.load(sweepPath);
        String defaultsPath = sweepCfg.getOrDefault("capture_defaults", "config/passive_convection_capture.yaml").toString();
        String analogiesPath = sweepCfg.getOrDefault("analogies", "config/convection_analogies.yaml").toString();
        String referencesPath = sweepCfg.getOrDefault("references", "config/convection_references.yaml").toString();

        System.out.println("Running convection DAC sweeps (speculative)...");
        ConvectionCaptureAnalyzer analyzer = new ConvectionCaptureAnalyzer(sweepPath, defaultsPath);
        ConvectionResultsSummary summary = analyzer.runAll();

        ConvectionChartGenerator charts = new ConvectionChartGenerator(figuresDir);
        charts.generateAll(summary);

        Files.createDirectories(resultsPath.getParent());
        Files.writeString(resultsPath, summary.toJson());
        System.out.println("Wrote " + resultsPath);

        ConvectionAnalogies analogies = ConvectionAnalogies.load(analogiesPath);
        ConvectionLiterature literature = ConvectionLiterature.load(referencesPath);
        String markdown = ConvectionTemplateExplainer.explain(summary, analogies, literature);
        ConvectionExplainerValidator.ValidationResult v = ConvectionExplainerValidator.validate(markdown, summary);
        if (!v.ok()) {
            System.err.println("Convection explainer warnings:");
            v.warnings().forEach(w -> System.err.println("  - " + w));
        }

        if (patchReadme && Files.exists(readmePath)) {
            ReadmePatcher.patchConvection(readmePath, markdown);
            System.out.println("Patched " + readmePath);
        }
    }
}
