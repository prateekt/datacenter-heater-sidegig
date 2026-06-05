package com.heater.model;

/** Colocated MRF / enzymatic PET — direct wash tanks plus heat-pump-boosted hot wash. */
public final class PlasticRecyclingLoad {
    public double directVolume = 80_000.0;
    public double directTemp = 22.0;
    public double directSetpoint = 65.0;
    public double directLossUa = 3500.0;

    public double boostTargetTemp = 85.0;
    public double minSourceTemp = 40.0;
    public double heatPumpCop = 3.2;
    public double hpCapacityW = 4_000_000.0;

    public boolean connected;
    public double boostDutyW;
    public double boostHeatDeliveredJ;
}
