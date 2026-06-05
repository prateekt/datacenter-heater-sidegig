package com.heater.robot;

public record RouterContext(
        double simTime,
        double poolTemp,
        double poolSetpoint,
        double aquacultureTemp,
        double aquacultureSetpoint,
        double houseTemp,
        double houseSetpoint,
        double bufferTemp,
        double algaeTemp,
        double algaeOptimalTemp,
        double ambientTemp,
        boolean primarySafe,
        boolean ccsCanRun,
        double plasticDirectTemp,
        double plasticDirectSetpoint,
        boolean plasticBoostCanRun
) {}
