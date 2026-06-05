package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class AcousticFigureMain {

    private AcousticFigureMain() {}

    public static void main(String[] args) throws Exception {
        String sweepPath = "config/acoustic_sweep.yaml";
        Path figuresDir = Path.of("docs/figures");
        Path resultsPath = Path.of("docs/acoustic_summary.json");
        Path readmePath = Path.of("README.md");
        boolean patchReadme = true;

        for (int i = 0; i < args.length; i++) {
            if ("--sweep".equals(args[i])) {
                sweepPath = args[++i];
            } else if ("--no-readme".equals(args[i])) {
                patchReadme = false;
            }
        }

        Map<String, Object> sweepCfg = ConfigLoader.load(sweepPath);
        String spectrumPath = sweepCfg.getOrDefault("spectrum_defaults", "config/acoustic_spectrum.yaml").toString();
        String equalizerPath = sweepCfg.getOrDefault("equalizer_defaults", "config/mechanical_equalizer.yaml").toString();
        String diffusionPath = sweepCfg.getOrDefault("diffusion_defaults", "config/mechanical_diffusion.yaml").toString();
        String convectionPath = sweepCfg.getOrDefault("convection_defaults", "config/passive_convection_capture.yaml").toString();
        String analogiesPath = sweepCfg.getOrDefault("analogies", "config/acoustic_analogies.yaml").toString();
        String referencesPath = sweepCfg.getOrDefault("references", "config/acoustic_references.yaml").toString();

        System.out.println("Running acoustic side-gig sweeps (speculative)...");
        AcousticCaptureAnalyzer analyzer = new AcousticCaptureAnalyzer(
                sweepPath, spectrumPath, equalizerPath, diffusionPath, convectionPath);
        AcousticResultsSummary summary = analyzer.runAll();

        AcousticChartGenerator charts = new AcousticChartGenerator(figuresDir);
        charts.generateAll(summary);

        Files.createDirectories(resultsPath.getParent());
        Files.writeString(resultsPath, summary.toJson());
        System.out.println("Wrote " + resultsPath);

        AcousticAnalogies analogies = AcousticAnalogies.load(analogiesPath);
        AcousticLiterature literature = AcousticLiterature.load(referencesPath);
        String markdown = AcousticTemplateExplainer.explain(summary, analogies, literature);

        if (patchReadme && Files.exists(readmePath)) {
            ReadmePatcher.patchAcoustic(readmePath, markdown);
            System.out.println("Patched " + readmePath);
        }
    }
}
