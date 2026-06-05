package com.heater.analysis;

public record HeatApplicationPoint(
        String scenarioId,
        String label,
        String robotPriority,
        String routingNote,
        double netCo2eTonnesPerYear,
        double heatPoolMwh,
        double heatAquacultureMwh,
        double heatAlgaeMwh,
        double heatDacMwh,
        double heatPlasticMwh,
        double heatTotalMwh,
        double olympicPoolsEquivalent,
        double communityPoolsEquivalent,
        double aquacultureRacewaysEquivalent,
        double fishProductionKgPerYear,
        double algaeHectaresEquivalent,
        double petTonnesEquivalent,
        double homesHeatedEquivalent,
        double hotShowersEquivalent,
        double poolSatisfactionPct
) {}
