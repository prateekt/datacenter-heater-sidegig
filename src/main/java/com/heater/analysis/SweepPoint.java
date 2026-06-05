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
        boolean forecast,
        ThermalReport thermal
) {
    public SweepPoint withHalls(int newHalls) {
        if (newHalls == halls) return this;
        double scale = (double) newHalls / halls;
        return new SweepPoint(
                sweepId,
                label,
                gpuCount,
                profileId,
                profileName,
                newHalls,
                avgWasteHeatMw * scale,
                netCo2eKg * scale,
                grossCo2Kg * scale,
                annualizedNetTonnes * scale,
                annualizedGrossTonnes * scale,
                forecast,
                thermal.multiply(scale)
        );
    }

    public SweepPoint withLabel(String newLabel) {
        return new SweepPoint(
                sweepId, newLabel, gpuCount, profileId, profileName, halls,
                avgWasteHeatMw, netCo2eKg, grossCo2Kg, annualizedNetTonnes,
                annualizedGrossTonnes, forecast, thermal
        );
    }
}
