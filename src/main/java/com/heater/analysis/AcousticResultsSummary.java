package com.heater.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AcousticResultsSummary {

    private final String generatedAt;
    private final List<AcousticSweepPoint> points = new ArrayList<>();
    private final List<String> chartPaths = new ArrayList<>();
    private final List<String> audioPaths = new ArrayList<>();
    private AcousticSweepPoint referencePoint;
    private AcousticReferenceRuns referenceRuns;

    public record AcousticReferenceRuns(
            double baselineDba,
            double mseDecoupledDba,
            double mseCoupledDba,
            double mdmgSpectralDistance,
            double mdmgHarmonicity,
            int mdmgSteps
    ) {}

    public AcousticResultsSummary() {
        this.generatedAt = Instant.now().toString();
    }

    public void addPoint(AcousticSweepPoint point) {
        points.add(point);
    }

    public void setReferencePoint(AcousticSweepPoint point) {
        this.referencePoint = point;
    }

    public void setReferenceRuns(AcousticReferenceRuns runs) {
        this.referenceRuns = runs;
    }

    public void addChart(String path) {
        chartPaths.add(path);
    }

    public void addAudio(String path) {
        audioPaths.add(path);
    }

    public List<AcousticSweepPoint> points() {
        return List.copyOf(points);
    }

    public List<String> chartPaths() {
        return List.copyOf(chartPaths);
    }

    public List<String> audioPaths() {
        return List.copyOf(audioPaths);
    }

    public AcousticSweepPoint referencePoint() {
        return referencePoint;
    }

    public AcousticReferenceRuns referenceRuns() {
        return referenceRuns;
    }

    public String generatedAt() {
        return generatedAt;
    }

    public List<AcousticSweepPoint> bySweep(String sweepId) {
        return points.stream().filter(p -> p.sweepId().equals(sweepId)).toList();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"generated_at\": \"").append(generatedAt).append("\",\n");
        sb.append("  \"charts\": ").append(jsonArray(chartPaths)).append(",\n");
        sb.append("  \"audio\": ").append(jsonArray(audioPaths)).append(",\n");
        if (referenceRuns != null) {
            sb.append("  \"reference_runs\": {");
            sb.append("\"baseline_dba\":").append(fmt(referenceRuns.baselineDba())).append(",");
            sb.append("\"mse_decoupled_dba\":").append(fmt(referenceRuns.mseDecoupledDba())).append(",");
            sb.append("\"mse_coupled_dba\":").append(fmt(referenceRuns.mseCoupledDba())).append(",");
            sb.append("\"mdmg_spectral_distance\":").append(fmt(referenceRuns.mdmgSpectralDistance())).append(",");
            sb.append("\"mdmg_harmonicity\":").append(fmt(referenceRuns.mdmgHarmonicity())).append(",");
            sb.append("\"mdmg_steps\":").append(referenceRuns.mdmgSteps());
            sb.append("},\n");
        }
        sb.append("  \"points\": [\n");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",\n");
            AcousticSweepPoint p = points.get(i);
            sb.append("    {");
            sb.append("\"sweep_id\":\"").append(p.sweepId()).append("\",");
            sb.append("\"label\":\"").append(escape(p.label())).append("\",");
            sb.append("\"liner_depth_mm\":").append(fmt(p.linerDepthMm())).append(",");
            sb.append("\"water_flow_l_s\":").append(fmt(p.waterFlowLS())).append(",");
            sb.append("\"diffusion_steps\":").append(p.diffusionSteps()).append(",");
            sb.append("\"coupling_mode\":\"").append(p.couplingMode()).append("\",");
            sb.append("\"baseline_dba\":").append(fmt(p.baselineDba())).append(",");
            sb.append("\"fence_line_dba\":").append(fmt(p.fenceLineDba())).append(",");
            sb.append("\"reduction_dba\":").append(fmt(p.reductionDba())).append(",");
            sb.append("\"soundscape_quality_index\":").append(fmt(p.soundscapeQualityIndex())).append(",");
            sb.append("\"tonal_prominence_db\":").append(fmt(p.tonalProminenceDb())).append(",");
            sb.append("\"added_fan_power_w\":").append(fmt(p.addedFanPowerW())).append(",");
            sb.append("\"spectral_distance\":").append(fmt(p.spectralDistance())).append(",");
            sb.append("\"harmonicity\":").append(fmt(p.harmonicity())).append(",");
            sb.append("\"volume_flow_m3_s\":").append(fmt(p.volumeFlowM3S()));
            sb.append("}");
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static String jsonArray(List<String> paths) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < paths.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(paths.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String fmt(double v) {
        return String.format("%.4f", v);
    }
}
