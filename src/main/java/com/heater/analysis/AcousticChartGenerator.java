package com.heater.analysis;

import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AcousticChartGenerator {

    private static final int W = 1000;
    private static final int H = 580;

    private final Path outputDir;

    public AcousticChartGenerator(Path outputDir) {
        this.outputDir = outputDir;
    }

    public List<String> generateAll(AcousticResultsSummary summary) throws IOException {
        Files.createDirectories(outputDir);
        List<String> paths = new ArrayList<>();
        paths.add(writeSplVsLinerDepth(summary));
        paths.add(writeSoundscapeVsWaterFlow(summary));
        paths.add(writeMdmgVsSteps(summary));
        paths.add(writeCouplingComparison(summary));
        for (String p : paths) {
            summary.addChart(p);
        }
        return paths;
    }

    private String writeSplVsLinerDepth(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("liner_depth");
        List<Double> x = pts.stream().map(AcousticSweepPoint::linerDepthMm).toList();
        List<Double> baseline = pts.stream().map(AcousticSweepPoint::baselineDba).toList();
        List<Double> fence = pts.stream().map(AcousticSweepPoint::fenceLineDba).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Fence-line SPL vs. Metamaterial Liner Depth (MSE)")
                .xAxisTitle("Liner depth (mm)")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.TEAL);
        chart.addSeries("baseline fan noise", x, baseline);
        chart.addSeries("after MSE", x, fence);
        return save(chart, "acoustic_spl_vs_liner_depth.png");
    }

    private String writeSoundscapeVsWaterFlow(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("water_flow");
        List<Double> x = pts.stream().map(AcousticSweepPoint::waterFlowLS).toList();
        List<Double> sqi = pts.stream().map(AcousticSweepPoint::soundscapeQualityIndex).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Soundscape Quality Index vs. Water Feature Flow")
                .xAxisTitle("Water flow (L/s)")
                .yAxisTitle("Soundscape quality index (0–1)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.GREEN);
        chart.addSeries("SQI", x, sqi);
        return save(chart, "acoustic_sqi_vs_water_flow.png");
    }

    private String writeMdmgVsSteps(AcousticResultsSummary summary) throws IOException {
        List<AcousticSweepPoint> pts = summary.bySweep("diffusion_steps");
        List<Double> x = pts.stream().map(p -> (double) p.diffusionSteps()).toList();
        List<Double> dist = pts.stream().map(AcousticSweepPoint::spectralDistance).toList();
        List<Double> harm = pts.stream().map(AcousticSweepPoint::harmonicity).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Mechanical Diffusion: Template Distance vs. Reverse Steps")
                .xAxisTitle("Reverse denoising steps")
                .yAxisTitle("Metric value")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.AMBER, ChartStyle.TEAL);
        chart.addSeries("spectral distance to template", x, dist);
        chart.addSeries("harmonicity", x, harm);
        return save(chart, "acoustic_mdmg_vs_steps.png");
    }

    private String writeCouplingComparison(AcousticResultsSummary summary) throws IOException {
        var runs = summary.referenceRuns();
        if (runs == null) {
            return saveEmptyPlaceholder();
        }
        List<String> labels = List.of("Baseline", "MSE decoupled", "MSE chimney-coupled");
        List<Double> dba = List.of(runs.baselineDba(), runs.mseDecoupledDba(), runs.mseCoupledDba());
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Fence-line SPL: Baseline vs. MSE Modes")
                .xAxisTitle("Mode")
                .yAxisTitle("SPL (dBA)")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.addSeries("dBA", labels, dba);
        return saveCategory(chart, "acoustic_coupling_comparison.png");
    }

    private String save(XYChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String saveCategory(CategoryChart chart, String filename) throws IOException {
        Path path = outputDir.resolve(filename);
        org.knowm.xchart.BitmapEncoder.saveBitmap(chart, path.toString(), org.knowm.xchart.BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String saveEmptyPlaceholder() throws IOException {
        XYChart chart = new XYChartBuilder().width(W).height(H).title("No coupling data").build();
        return save(chart, "acoustic_coupling_comparison.png");
    }
}
