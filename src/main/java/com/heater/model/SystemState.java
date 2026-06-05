package com.heater.model;

public final class SystemState {
    public double time;
    public double ambientTemp = 5.0;
    public final PrimaryLoop primary = new PrimaryLoop();
    public final HeatExchanger hx = new HeatExchanger();
    public final BufferTank buffer = new BufferTank();
    public final PoolLoad pool = new PoolLoad();
    public final AquacultureLoad aquaculture = new AquacultureLoad();
    public final HouseLoad house = new HouseLoad();
    public final CarbonCaptureLoad carbonCapture = new CarbonCaptureLoad();
    public final AlgaeLoad algae = new AlgaeLoad();
    public final PlasticRecyclingLoad plasticRecycling = new PlasticRecyclingLoad();
    public final PassiveConvectionLoad passiveConvection = new PassiveConvectionLoad();
    public double energyRecoveredJ;
    public double energyPoolJ;
    public double energyAquacultureJ;
    public double energyHouseJ;
    public double energyAlgaeJ;
    public double energyDacJ;
    public double energyPlasticDirectJ;
    public double energyPlasticBoostJ;
    public double energyRejectedJ;
    public double heatPumpElectricJ;
    public double fanElectricJ;
    public double convectionCo2CapturedKg;
    public double primaryUnsafeTimeS;
    public int ccsActiveSteps;
    public int totalSteps;
}
