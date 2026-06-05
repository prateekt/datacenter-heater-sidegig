package com.heater.analysis;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResultsSummary {

    private final String generatedAt;
    private final List<SweepPoint> points = new ArrayList<>();
    private final List<String> chartPaths = new ArrayList<>();
    private final Map<String, Object> metadata = new LinkedHashMap<>();

    public ResultsSummary() {
        this.generatedAt = Instant.now().toString();
    }

    public void addPoint(SweepPoint point) {
        points.add(point);
    }

    public void addChart(String path) {
        chartPaths.add(path);
    }

    public void putMetadata(String key, Object value) {
        metadata.put(key, value);
    }

    public List<SweepPoint> points() {
        return List.copyOf(points);
    }

    public List<String> chartPaths() {
        return List.copyOf(chartPaths);
    }

    public String generatedAt() {
        return generatedAt;
    }

    public List<SweepPoint> bySweep(String sweepId) {
        return points.stream().filter(p -> p.sweepId().equals(sweepId)).toList();
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"generated_at\": ").append(jsonString(generatedAt)).append(",\n");
        sb.append("  \"metadata\": ").append(mapToJson(metadata)).append(",\n");
        sb.append("  \"charts\": [");
        for (int i = 0; i < chartPaths.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(jsonString(chartPaths.get(i)));
        }
        sb.append("],\n");
        sb.append("  \"points\": [\n");
        for (int i = 0; i < points.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(pointToJson(points.get(i)));
        }
        sb.append("\n  ]\n}");
        return sb.toString();
    }

    private static String pointToJson(SweepPoint p) {
        return "    {" +
                "\"sweep_id\":" + jsonString(p.sweepId()) + "," +
                "\"label\":" + jsonString(p.label()) + "," +
                "\"gpu_count\":" + p.gpuCount() + "," +
                "\"profile_id\":" + jsonString(p.profileId()) + "," +
                "\"profile_name\":" + jsonString(p.profileName()) + "," +
                "\"halls\":" + p.halls() + "," +
                "\"avg_waste_heat_mw\":" + fmt(p.avgWasteHeatMw()) + "," +
                "\"net_co2e_kg\":" + fmt(p.netCo2eKg()) + "," +
                "\"gross_co2_kg\":" + fmt(p.grossCo2Kg()) + "," +
                "\"annualized_net_tonnes\":" + fmt(p.annualizedNetTonnes()) + "," +
                "\"annualized_gross_tonnes\":" + fmt(p.annualizedGrossTonnes()) + "," +
                "\"forecast\":" + p.forecast() +
                "}";
    }

    private static String mapToJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (i++ > 0) sb.append(", ");
            sb.append(jsonString(e.getKey())).append(": ");
            Object v = e.getValue();
            if (v instanceof Number n) {
                sb.append(fmt(n.doubleValue()));
            } else if (v instanceof Boolean b) {
                sb.append(b);
            } else {
                sb.append(jsonString(String.valueOf(v)));
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        if (Math.abs(v) >= 1_000_000) return String.format("%.1f", v);
        if (Math.abs(v) >= 100) return String.format("%.2f", v);
        return String.format("%.4f", v);
    }
}
