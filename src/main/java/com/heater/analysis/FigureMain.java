package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class FigureMain {

    private FigureMain() {}

    public static void main(String[] args) throws Exception {
        String sweepPath = "config/scalability_sweep.yaml";
        Path figuresDir = Path.of("docs/figures");
        Path resultsPath = Path.of("docs/results_summary.json");
        Path readmePath = Path.of("README.md");

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--sweep" -> sweepPath = args[++i];
                case "--no-readme" -> readmePath = null;
                default -> { }
            }
        }

        System.out.println("Running scalability sweeps (this may take several minutes)...");
        SweepRunner runner = new SweepRunner(sweepPath, sweepPath);
        ResultsSummary summary = runner.runAll();

        Map<String, Object> sweepCfg = runner.sweepConfig();
        double simDurationS = ConfigLoader.d(sweepCfg, "sim_duration_s", 604_800.0);
        String gpuProfilesPath = sweepCfg.getOrDefault("gpu_profiles", "config/gpu_profiles.yaml").toString();

        GpuProfile.GpuProfileRegistry registry = GpuProfile.load(gpuProfilesPath);
        ClimateAnalogies analogies = ClimateAnalogies.loadDefault();

        ChartGenerator charts = new ChartGenerator(figuresDir, analogies, simDurationS);
        charts.generateAll(summary, registry);

        Files.createDirectories(resultsPath.getParent());
        Files.writeString(resultsPath, summary.toJson());
        System.out.println("Wrote " + resultsPath);

        ResultsExplainer explainer = new ResultsExplainer();
        String markdown = explainer.explain(summary, gpuProfilesPath);

        if (readmePath != null && Files.exists(readmePath)) {
            ReadmePatcher.patch(readmePath, markdown);
            System.out.println("Patched " + readmePath);
        }

        System.out.println("Done. Figures in docs/figures/");
    }
}
