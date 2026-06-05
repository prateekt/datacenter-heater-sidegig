package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public record GpuProfile(
        String id,
        String displayName,
        String era,
        double gpuTdpW,
        double systemWasteWPerGpu,
        String cooling,
        boolean forecast,
        String sourceNote
) {
    public int gpuCountForWasteHeat(double wasteHeatW) {
        return (int) (wasteHeatW / systemWasteWPerGpu);
    }

    public double wasteHeatW(int gpuCount, double utilizationFactor) {
        return gpuCount * systemWasteWPerGpu * utilizationFactor;
    }

    public double avgWasteHeatMw(int gpuCount) {
        return gpuCount * systemWasteWPerGpu / 1_000_000.0;
    }

    public static GpuProfileRegistry load(String path) throws IOException {
        Map<String, Object> root = ConfigLoader.load(path);
        Map<String, Object> refHall = ConfigLoader.map(root, "reference_hall");
        Map<String, Object> util = ConfigLoader.map(root, "utilization");

        String refProfileId = refHall.getOrDefault("profile_id", "B200_LC").toString();
        int refGpuCount = (int) ConfigLoader.d(refHall, "gpu_count", 37_000);
        double baseFactor = ConfigLoader.d(util, "base_factor", 0.901);
        double peakFactor = ConfigLoader.d(util, "peak_factor", 1.442);

        List<Map<String, Object>> profiles = (List<Map<String, Object>>) root.get("profiles");
        Map<String, GpuProfile> byId = new LinkedHashMap<>();
        if (profiles != null) {
            for (Map<String, Object> p : profiles) {
                GpuProfile gp = new GpuProfile(
                        p.get("id").toString(),
                        p.getOrDefault("display_name", p.get("id")).toString(),
                        p.getOrDefault("era", "deployed").toString(),
                        ConfigLoader.d(p, "gpu_tdp_w", 0),
                        ConfigLoader.d(p, "system_waste_w_per_gpu", 0),
                        p.getOrDefault("cooling", "liquid").toString(),
                        Boolean.TRUE.equals(p.get("forecast")),
                        p.getOrDefault("source_note", "").toString()
                );
                byId.put(gp.id(), gp);
            }
        }
        return new GpuProfileRegistry(byId, refProfileId, refGpuCount, baseFactor, peakFactor);
    }

    public record GpuProfileRegistry(
            Map<String, GpuProfile> byId,
            String referenceProfileId,
            int referenceGpuCount,
            double baseUtilizationFactor,
            double peakUtilizationFactor
    ) {
        public GpuProfile require(String id) {
            GpuProfile p = byId.get(id);
            if (p == null) {
                throw new IllegalArgumentException("Unknown GPU profile: " + id);
            }
            return p;
        }

        public GpuProfile referenceProfile() {
            return require(referenceProfileId);
        }
    }
}
