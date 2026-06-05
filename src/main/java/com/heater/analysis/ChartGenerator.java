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

    private final Path outputDir;
    private final ClimateAnalogies analogies;
    private final double simDurationS;

    public ChartGenerator(Path outputDir, ClimateAnalogies analogies, double simDurationS) {
        this.outputDir = outputDir;
        this.analogies = analogies;
        this.simDurationS = simDurationS;
    }

    public List<String> generateAll(ResultsSummary summary, GpuProfile.GpuProfileRegistry registry) throws IOException {
        Files.createDirectories(outputDir);
        List<String> paths = new ArrayList<>();
        paths.add(writeCo2VsGpuCount(summary));
        paths.add(writeCo2VsGeneration(summary));
        paths.add(writeCo2Saturation(summary));
        paths.add(writeMultiHall(summary));
        paths.add(writeGrossVsNet(summary));
        paths.add(writeGpuTdpTimeline(registry));
        for (String p : paths) {
            summary.addChart(p);
        }
        return paths;
    }

    private String carsAxisLabel() {
        return "Cars off the road for one year (U.S. avg ≈ "
                + String.format(Locale.US, "%.1f", analogies.carTonnesPerYear()) + " t CO₂/car)";
    }

    private String writeCo2VsGpuCount(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<String> xLabels = pts.stream()
                .map(p -> formatGpuLabel(p.gpuCount(), p.avgWasteHeatMw()))
                .toList();
        List<Double> cars = pts.stream()
                .map(p -> analogies.carsFromAnnualTonnes(p.annualizedNetTonnes()))
                .toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("More GPUs → More CO₂ Pulled From the Air")
                .xAxisTitle("GPU count (H100-class hall, plant scales with size)")
                .yAxisTitle(carsAxisLabel())
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.TEAL);
        chart.getStyler().setXAxisLabelRotation(30);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net removal", xLabels, cars);
        return save(chart, "co2_vs_gpu_count.png");
    }

    private String writeCo2VsGeneration(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_generation");
        List<String> labels = pts.stream()
                .map(p -> p.forecast() ? p.profileName() + " †" : p.profileName())
                .toList();
        List<Double> cars = pts.stream()
                .map(p -> analogies.carsFromAnnualTonnes(p.annualizedNetTonnes()))
                .toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Newer Chips Run Hotter → More CO₂ Removal (same hall size)")
                .xAxisTitle("GPU generation (" + pts.get(0).gpuCount() + " GPUs per run, † = forecast)")
                .yAxisTitle(carsAxisLabel())
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.INDIGO);
        chart.getStyler().setXAxisLabelRotation(35);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net removal", labels, cars);
        return save(chart, "co2_vs_gpu_generation.png");
    }

    private String writeCo2Saturation(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("saturation");
        List<String> labels = pts.stream()
                .map(p -> String.format(Locale.US, "%.0f× heat\n(%,d GPUs)", 
                        heatMultiplierFromLabel(p.label()), p.gpuCount()))
                .toList();
        List<Double> cars = pts.stream()
                .map(p -> analogies.carsFromKg(p.netCo2eKg(), simDurationS))
                .toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(W).height(H)
                .title("Fixed Capture Plant — Extra Heat Stops Helping (saturation)")
                .xAxisTitle("Waste heat vs. reference hall (capture equipment held constant)")
                .yAxisTitle(carsAxisLabel() + " — annualized from 7-day sim")
                .build();
        ChartStyle.applyCategory(chart, ChartStyle.AMBER);
        chart.getStyler().setXAxisLabelRotation(0);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net removal", labels, cars);
        return save(chart, "co2_saturation_gpu.png");
    }

    private String writeMultiHall(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("multi_hall");
        List<Integer> halls = pts.stream().map(SweepPoint::halls).toList();
        List<Double> cars = pts.stream()
                .map(p -> analogies.carsFromAnnualTonnes(p.annualizedNetTonnes()))
                .toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Campus Rollout — Each Hall Adds More Cars-Off-Road Impact")
                .xAxisTitle("Number of B200 halls (~25,000 GPUs each)")
                .yAxisTitle(carsAxisLabel())
                .build();
        ChartStyle.applyXy(chart, ChartStyle.GREEN);
        chart.getStyler().setYAxisDecimalPattern("#,###");
        chart.addSeries("net removal", halls, cars);
        ChartStyle.applyLineMarkers(chart);
        return save(chart, "co2_multi_hall.png");
    }

    private String writeGrossVsNet(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<Integer> x = pts.stream().map(SweepPoint::gpuCount).toList();
        List<Double> grossCars = pts.stream()
                .map(p -> analogies.carsFromAnnualTonnes(p.annualizedGrossTonnes()))
                .toList();
        List<Double> netCars = pts.stream()
                .map(p -> analogies.carsFromAnnualTonnes(p.annualizedNetTonnes()))
                .toList();
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Gross vs. Net Removal — Heat Pumps Cost Some CO₂")
                .xAxisTitle("GPU count (H100-class)")
                .yAxisTitle(carsAxisLabel())
                .build();
        ChartStyle.applyXy(chart, ChartStyle.SLATE, ChartStyle.TEAL);
        chart.addSeries("gross captured", x, grossCars);
        chart.addSeries("net (after electricity)", x, netCars);
        ChartStyle.applyLineMarkers(chart);
        return save(chart, "gross_vs_net_co2.png");
    }

    private String writeGpuTdpTimeline(GpuProfile.GpuProfileRegistry registry) throws IOException {
        List<Double> years = List.of(2020.0, 2023.0, 2024.0, 2025.0, 2026.0, 2027.0, 2028.0);
        List<Double> bulbs = List.of(5.5, 9.5, 9.5, 13.5, 15.5, 25.5, 35.0);
        XYChart chart = new XYChartBuilder()
                .width(W).height(H)
                .title("Each GPU Generation Runs Hotter (100 W bulb = 1 bulb unit)")
                .xAxisTitle("Year")
                .yAxisTitle("Equivalent 100 W light bulbs running 24/7 per GPU")
                .build();
        ChartStyle.applyXy(chart, ChartStyle.INDIGO);
        chart.getStyler().setYAxisDecimalPattern("#0");
        chart.addSeries("heat per GPU", years, bulbs);
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
