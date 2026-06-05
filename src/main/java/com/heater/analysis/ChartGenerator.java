package com.heater.analysis;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.style.Styler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChartGenerator {

    private final Path outputDir;

    public ChartGenerator(Path outputDir) {
        this.outputDir = outputDir;
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

    private String writeCo2VsGpuCount(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<Integer> x = pts.stream().map(SweepPoint::gpuCount).toList();
        List<Double> y = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(900).height(550)
                .title("Net CO₂ Removal vs GPU Count (H100, proportional plant)")
                .xAxisTitle("Number of GPUs")
                .yAxisTitle("Annualized net CO₂e (tonnes/year)")
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.addSeries("net", x.stream().map(String::valueOf).toList(), y);
        return save(chart, "co2_vs_gpu_count.png");
    }

    private String writeCo2VsGeneration(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_generation");
        List<String> labels = new ArrayList<>();
        List<Double> y = new ArrayList<>();
        for (SweepPoint p : pts) {
            labels.add(p.forecast() ? p.profileName() + " (forecast)" : p.profileName());
            y.add(p.annualizedNetTonnes());
        }
        CategoryChart chart = new CategoryChartBuilder()
                .width(900).height(550)
                .title("Net CO₂ Removal by GPU Generation (37,000 GPUs)")
                .xAxisTitle("GPU generation")
                .yAxisTitle("Annualized net CO₂e (tonnes/year)")
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.addSeries("net", labels, y);
        return save(chart, "co2_vs_gpu_generation.png");
    }

    private String writeCo2Saturation(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("saturation");
        List<String> labels = pts.stream().map(SweepPoint::label).toList();
        List<Double> y = pts.stream().map(SweepPoint::netCo2eKg).map(kg -> kg / 1000.0).toList();
        CategoryChart chart = new CategoryChartBuilder()
                .width(900).height(550)
                .title("CO₂ Removal Saturation (fixed plant, rising heat)")
                .xAxisTitle("Heat multiplier (GPU-equivalent)")
                .yAxisTitle("7-day net CO₂e (tonnes)")
                .build();
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setXAxisLabelRotation(45);
        chart.addSeries("net", labels, y);
        return save(chart, "co2_saturation_gpu.png");
    }

    private String writeMultiHall(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("multi_hall");
        List<Integer> halls = pts.stream().map(SweepPoint::halls).toList();
        List<Double> y = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        XYChart chart = new XYChartBuilder()
                .width(900).height(550)
                .title("Campus Rollout: Net CO₂ vs Number of Halls (B200)")
                .xAxisTitle("Number of halls")
                .yAxisTitle("Annualized net CO₂e (tonnes/year)")
                .build();
        chart.getStyler().setLegendPosition(Styler.LegendPosition.InsideNE);
        chart.addSeries("net", halls, y);
        return save(chart, "co2_multi_hall.png");
    }

    private String writeGrossVsNet(ResultsSummary summary) throws IOException {
        List<SweepPoint> pts = summary.bySweep("gpu_count_ramp");
        List<Integer> x = pts.stream().map(SweepPoint::gpuCount).toList();
        List<Double> gross = pts.stream().map(SweepPoint::annualizedGrossTonnes).toList();
        List<Double> net = pts.stream().map(SweepPoint::annualizedNetTonnes).toList();
        XYChart chart = new XYChartBuilder()
                .width(900).height(550)
                .title("Gross vs Net CO₂ Removal (H100 ramp)")
                .xAxisTitle("Number of GPUs")
                .yAxisTitle("Annualized CO₂ (tonnes/year)")
                .build();
        chart.addSeries("gross", x, gross);
        chart.addSeries("net", x, net);
        return save(chart, "gross_vs_net_co2.png");
    }

    private String writeGpuTdpTimeline(GpuProfile.GpuProfileRegistry registry) throws IOException {
        List<Double> years = List.of(2020.0, 2023.0, 2024.0, 2025.0, 2026.0, 2027.0, 2028.0);
        List<Double> watts = List.of(550.0, 950.0, 950.0, 1350.0, 1550.0, 2550.0, 3500.0);
        XYChart chart = new XYChartBuilder()
                .width(900).height(550)
                .title("System Waste Heat per GPU Over Time (representative)")
                .xAxisTitle("Year")
                .yAxisTitle("System waste heat (W/GPU)")
                .build();
        chart.addSeries("W/GPU", years, watts);
        chart.getStyler().setLegendVisible(false);
        BitmapEncoder.saveBitmap(chart, outputDir.resolve("gpu_tdp_timeline").toString(), BitmapEncoder.BitmapFormat.PNG);
        return "docs/figures/gpu_tdp_timeline.png";
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
