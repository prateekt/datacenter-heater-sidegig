package com.heater.analysis;

public record AcousticSweepPoint(
        String sweepId,
        String label,
        double linerDepthMm,
        double waterFlowLS,
        int diffusionSteps,
        String couplingMode,
        double baselineDba,
        double fenceLineDba,
        double reductionDba,
        double soundscapeQualityIndex,
        double tonalProminenceDb,
        double addedFanPowerW,
        double spectralDistance,
        double harmonicity,
        double volumeFlowM3S
) {}
