package com.heater.analysis;

public record SweepPoint(
        String sweepId,
        String label,
        int gpuCount,
        String profileId,
        String profileName,
        int halls,
        double avgWasteHeatMw,
        double netCo2eKg,
        double grossCo2Kg,
        double annualizedNetTonnes,
        double annualizedGrossTonnes,
        boolean forecast
) {}
