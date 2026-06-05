package com.heater.analysis;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChartGenerator {

    private static final int W = 1000;
    private static final int H = 580;
    private static final String Y_THERMAL_ANNUAL = "Thermal service delivered (GWh / year)";
    private static final String Y_THERMAL_7DAY = "Thermal service delivered (GWh, 7-day sim)";
    private static final String Y_ANNUAL = "Net CO₂e removed (metric tonnes / year)";
    private static final String Y_GROSS_NET = "CO₂e removed (metric tonnes / year)";
    private static final String Y_7DAY = "Net CO₂e removed (metric tonnes, 7-day sim)";

    private final Path outputDir;
    private final double simDurationS;

    public ChartGenerator(Path outputDir, ClimateAnalogies analogies, double simDurationS) {
        this.outputDir = outputDir;
        this.simDurationS = simDurationS;
    }

    public List<String> generateAll(ResultsSummary summary, GpuProfile.GpuProfileRegistry registry) throws IOException {
        Files.createDirectories(outputDir);
        List<String> paths = new ArrayList<>();
        paths.add(writeThermalVsGpuCount(summary));
        paths.add(writeThermalByGeneration(summary));
        paths.add(writeThermalSaturation(summary));
        paths.add(writeThermalMultiHall(summary));
        paths.add(writeThermalLoadSplit(summary, registry));
        paths.add(writeGpuTdpTimeline(registry));
        paths.add(writeCo2VsGpuCount(summary));
        paths.add(writeCo2VsGeneration(summary));
        paths.add(writeCo2Saturation(summary));
        paths.add(writeMultiHall(summary));
        paths.add(writeGrossVsNet(summary));
        for (String p : paths) {
            summary.addChart(p);
        }
        return paths;
    }

    private String writeThermalVsGpuCount(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<String> xLabels = pts.stream()
                .map(p -> formatGpuLabel(p.gpuCount(), p.avgWasteHeatMw()))
                .toList();
        List<Double> y = pts.stream().map(p -> p.thermal().annualizedRecoveredGwh()).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Thermal Service vs. GPU Count")
                .xAxisTitle("GPU count — H100-class, plant scales with hall (MW shown)")
                .yAxisTitle(Y_THERMAL_ANNUAL)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.getStyler().setXAxisLabelRotation(30);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("thermal service", xLabels, y);
        return save(chart, "thermal_service_vs_gpu_count.png");
    }

    private String writeThermalByGeneration(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_generation");
        List<String> labels = pts.stream()
                .map(p -> p.forecast() ? p.profileName() + " †" : p.profileName())
                .toList();
        List<Double> y = pts.stream().map(p -> p.thermal().annualizedRecoveredGwh()).toList();
        int gpus = pts.isEmpty() ? 0 : pts.get(0).gpuCount();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Thermal Service by GPU Generation")
                .xAxisTitle("GPU generation (" + String.format(Locale.US, "%,d", gpus) + " GPUs, † = forecast)")
                .yAxisTitle(Y_THERMAL_ANNUAL)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.INDIGO);
        chart.getStyler().setXAxisLabelRotation(35);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("thermal service", labels, y);
        return save(chart, "thermal_by_generation.png");
    }

    private String writeThermalSaturation(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("saturation");
        List<String> labels = pts.stream()
                .map(p -> String.format(Locale.US, "%.1f×\n(%,d GPU-equiv.)",
                        heatMultiplierFromLabel(p.label()), p.gpuCount()))
                .toList();
        List<Double> y = pts.stream().map(p -> p.thermal().recoveredMwh() / 1000.0).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Thermal Saturation — Fixed Plant, Rising Waste Heat")
                .xAxisTitle("Heat multiplier vs. reference hall (capture equipment fixed)")
                .yAxisTitle(Y_THERMAL_7DAY)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.AMBER);
        chart.getStyler().setXAxisLabelRotation(0);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("thermal service", labels, y);
        return save(chart, "thermal_saturation_gpu.png");
    }

    private String writeThermalMultiHall(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("multi_hall");
        List<Integer> halls = pts.stream().map(SweepPoint::halls).toList();
        List<Double> y = pts.stream().map(p -> p.thermal().annualizedRecoveredGwh()).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Campus Thermal Service — Multi-Hall Rollout")
                .xAxisTitle("Number of halls (~25,000 B200 liquid GPUs each)")
                .yAxisTitle(Y_THERMAL_ANNUAL)
                .build();
        ChartStyle.applyXy(chart, ChartStyle.GREEN);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("thermal service", halls, y);
        ChartStyle.applyLineMarkers(chart);
        return save(chart, "thermal_multi_hall.png");
    }

    private String writeThermalLoadSplit(ResultsSummary summary, GpuProfile.GpuProfileRegistry registry) throws IOException {
        SweepPoint ref = summary.bySweep("gpu_generation").stream()
                .filter(p -> registry.referenceProfileId().equals(p.profileId()))
                .findFirst()
                .orElse(summary.bySweep("gpu_generation").isEmpty() ? null
                        : summary.bySweep("gpu_generation").get(0));
        if (ref == null) {
            ref = summary.points().isEmpty() ? null : summary.points().get(0);
        }
        if (ref == null) {
            return "docs/figures/thermal_load_split.png";
        }
        ThermalReport t = ref.thermal();
        List<String> labels = List.of("25k reference hall");
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Thermal Load Split — Reference Hall (DAC priority)")
                .xAxisTitle("Downstream process")
                .yAxisTitle("Annual thermal service (GWh / year)")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.getStyler().setStacked(true);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("DAC", labels, List.of(t.dacMwh() / 1000.0));
        chart.addSeries("Algae", labels, List.of(t.algaeMwh() / 1000.0));
        chart.addSeries("Pool", labels, List.of(t.poolMwh() / 1000.0));
        chart.addSeries("Aquaculture", labels, List.of(t.aquacultureMwh() / 1000.0));
        chart.addSeries("Rejected", labels, List.of(t.rejectedMwh() / 1000.0));
        return save(chart, "thermal_load_split.png");
    }

    private String writeCo2VsGpuCount(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<String> xLabels = pts.stream()
                .map(p -> formatGpuLabel(p.gpuCount(), p.avgWasteHeatMw()))
                .toList();
        List<Double> y = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Net CO₂ Removal vs. GPU Count")
                .xAxisTitle("GPU count — H100-class, plant scales with hall (MW shown)")
                .yAxisTitle(Y_ANNUAL)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.getStyler().setXAxisLabelRotation(30);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net CO₂e", xLabels, y);
        return save(chart, "co2_vs_gpu_count.png");
    }

    private String writeCo2VsGeneration(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_generation");
        List<String> labels = pts.stream()
                .map(p -> p.forecast() ? p.profileName() + " †" : p.profileName())
                .toList();
        List<Double> y = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        int gpus = pts.isEmpty() ? 0 : pts.get(0).gpuCount();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Net CO₂ Removal by GPU Generation")
                .xAxisTitle("GPU generation (" + String.format(Locale.US, "%,d", gpus) + " GPUs, † = forecast)")
                .yAxisTitle(Y_ANNUAL)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.INDIGO);
        chart.getStyler().setXAxisLabelRotation(35);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net CO₂e", labels, y);
        return save(chart, "co2_vs_gpu_generation.png");
    }

    private String writeCo2Saturation(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("saturation");
        List<String> labels = pts.stream()
                .map(p -> String.format(Locale.US, "%.1f×\n(%,d GPU-equiv.)",
                        heatMultiplierFromLabel(p.label()), p.gpuCount()))
                .toList();
        List<Double> y = pts.stream().map(p -> p.netCo2eKg() / 1000.0).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Capture Plant Saturation — Fixed DAC, Rising Waste Heat")
                .xAxisTitle("Heat multiplier vs. reference hall (capture equipment fixed)")
                .yAxisTitle(Y_7DAY)
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.AMBER);
        chart.getStyler().setXAxisLabelRotation(0);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net CO₂e", labels, y);
        return save(chart, "co2_saturation_gpu.png");
    }

    private String writeMultiHall(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("multi_hall");
        List<Integer> halls = pts.stream().map(SweepPoint::halls).toList();
        List<Double> y = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Hyperscale Campus Rollout — Cumulative Net CO₂ Removal")
                .xAxisTitle("Number of halls (~25,000 B200 liquid GPUs each)")
                .yAxisTitle(Y_ANNUAL)
                .build();
        ChartStyle.applyXy(chart, ChartStyle.GREEN);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net CO₂e", halls, y);
        ChartStyle.applyLineMarkers(chart);
        return save(chart, "co2_multi_hall.png");
    }

    private String writeGrossVsNet(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<Integer> x = pts.stream().map(SweepPoint::gpuCount).toList();
        List<Double> gross = pts.stream().map(SweepPoint::annualizedGrossTonnes).toList();
        List<Double> net = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Gross vs. Net CO₂ Removal — Heat-Pump Grid Penalty")
                .xAxisTitle("GPU count (H100-class)")
                .yAxisTitle(Y_GROSS_NET)
                .build();
        ChartStyle.applyXy(chart, ChartStyle.SLATE, ChartStyle.TEAL);
        chart.addSeries("gross captured", x, gross);
        chart.addSeries("net (after electricity)", x, net);
        ChartStyle.applyLineMarkers(chart);
        return save(chart, "gross_vs_net_co2.png");
    }

    private String writeGpuTdpTimeline(GpuProfile.GpuProfileRegistry registry) throws IOException {
        List<Double> years = List.of(2020.0, 2023.0, 2024.0, 2025.0, 2026.0, 2027.0, 2028.0);
        List<Double> watts = List.of(550.0, 950.0, 950.0, 1350.0, 1550.0, 2550.0, 3500.0);
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("System Waste Heat per GPU — NVIDIA Generations")
                .xAxisTitle("Year")
                .yAxisTitle("Waste heat to coolant loop (watts per GPU)")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.INDIGO);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("W/GPU", years, watts);
        ChartStyle.applyLineMarkers(chart);
        BitmapEncoder.saveBitmap(chart, outputDir.resolve("gpu_tdp_timeline").toString(), BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/gpu_tdp_timeline.png";
    }

    private static String formatGpuLabel(int gpus, double mw) {
        return String.format(Locale.US, "%,d\n(~%.0f MW)", gpus, mw);
    }

    private static double heatMultiplierFromLabel(String label) {
        int xIdx = label.indexOf('x');
        if (xIdx > 0) {
            try {
                return Double.parseDouble(label.substring(0, xIdx).trim());
            } catch (NumberFormatException ignored) { }
        }
        return 1.0;
    }

    private String save(CategoryChart chart, String filename) throws IOException {
        BitmapEncoder.saveBitmap(chart, outputDir.resolve(filename.replace(".png", "")).toString(),
                BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }

    private String save(XYChart chart, String filename) throws IOException {
        BitmapEncoder.saveBitmap(chart, outputDir.resolve(filename.replace(".png", "")).toString(),
                BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/" + filename;
    }
}
