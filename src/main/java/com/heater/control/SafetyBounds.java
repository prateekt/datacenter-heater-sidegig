package com.heater.control;

public final class SafetyBounds {
    public double rejectFractionMin;
    public double rejectFractionMax = 1.0;
    public double secondaryPumpMax = 1.0;
    public boolean allowSecondary = true;
    public boolean allowPoolValve = true;
    public boolean allowHouseValve = true;
    public boolean allowCcsValve = true;
    public boolean allowAlgaeValve = true;
    public boolean allowPlasticValve = true;
    public boolean forceFullReject;
    public String alarm;
}
