package com.heater.config;

import com.heater.carbon.ConvectionCaptureConfig;
import com.heater.model.SystemState;

import java.util.Map;

public final class StateFactory {

    private StateFactory() {}

    public static SystemState buildInitialState(Map<String, Object> config) {
        Map<String, Object> phys = ConfigLoader.map(config, "physics");
        Map<String, Object> primaryCfg = ConfigLoader.map(config, "primary_loop");
        Map<String, Object> tankCfg = ConfigLoader.map(config, "buffer_tank");
        Map<String, Object> loads = ConfigLoader.map(config, "loads");
        Map<String, Object> scenarios = ConfigLoader.map(config, "scenarios");
        String bootstrap = config.get("bootstrap_scenario") instanceof String s ? s : "winter_day";
        Map<String, Object> bootstrapScenario = ConfigLoader.map(scenarios, bootstrap);
        if (bootstrapScenario.isEmpty()) {
            bootstrapScenario = ConfigLoader.map(scenarios, "winter_day");
        }

        SystemState s = new SystemState();
        s.ambientTemp = ConfigLoader.d(phys, "ambient_temp", 5.0);

        s.primary.tIn = ConfigLoader.d(primaryCfg, "t_in", 35.0);
        s.primary.tOut = s.primary.tIn + 10.0;
        s.primary.mdot = ConfigLoader.d(primaryCfg, "mdot", 12.0);
        s.primary.qWaste = ConfigLoader.d(bootstrapScenario, "q_waste_base", 200_000.0);

        s.buffer.volume = ConfigLoader.d(tankCfg, "volume", 5000.0);
        s.buffer.temperature = ConfigLoader.d(tankCfg, "initial_temp", 30.0);

        Map<String, Object> pool = ConfigLoader.map(loads, "pool");
        s.pool.volume = ConfigLoader.d(pool, "volume", 80_000.0);
        s.pool.temperature = ConfigLoader.d(pool, "initial_temp", 22.0);
        s.pool.setpoint = ConfigLoader.d(pool, "setpoint", 28.0);
        s.pool.lossUa = ConfigLoader.d(pool, "loss_ua", 800.0);

        Map<String, Object> aqua = ConfigLoader.map(loads, "aquaculture");
        if (!aqua.isEmpty()) {
            s.aquaculture.volume = ConfigLoader.d(aqua, "volume_m3", 50_000.0);
            s.aquaculture.temperature = ConfigLoader.d(aqua, "initial_temp", 18.0);
            s.aquaculture.setpoint = ConfigLoader.d(aqua, "setpoint", 22.0);
            s.aquaculture.lossUa = ConfigLoader.d(aqua, "loss_ua", 2500.0);
        }

        Map<String, Object> house = ConfigLoader.map(loads, "house");
        s.house.thermalMass = ConfigLoader.d(house, "thermal_mass", 50_000.0);
        s.house.temperature = ConfigLoader.d(house, "initial_temp", 18.0);
        s.house.setpoint = ConfigLoader.d(house, "setpoint", 40.0);
        s.house.lossUa = ConfigLoader.d(house, "loss_ua", 1200.0);

        Map<String, Object> ccs = ConfigLoader.map(loads, "carbon_capture");
        s.carbonCapture.regenerationTemp = ConfigLoader.d(ccs, "regeneration_temp", 90.0);
        s.carbonCapture.minSourceTemp = ConfigLoader.d(ccs, "min_source_temp", 40.0);
        s.carbonCapture.heatPumpCop = ConfigLoader.d(ccs, "heat_pump_cop", 3.0);
        s.carbonCapture.specificHeatDutyJPerKg = ConfigLoader.d(ccs, "specific_heat_duty_j_per_kg", 5_500_000.0);
        s.carbonCapture.hpCapacityW = ConfigLoader.d(ccs, "hp_capacity_w", 150_000.0);

        Map<String, Object> algae = ConfigLoader.map(loads, "algae");
        s.algae.optimalTemp = ConfigLoader.d(algae, "optimal_temp", 28.0);
        s.algae.minTemp = ConfigLoader.d(algae, "min_temp", 15.0);
        s.algae.maxTemp = ConfigLoader.d(algae, "max_temp", 38.0);
        s.algae.volumeM3 = ConfigLoader.d(algae, "volume_m3", 200.0);
        s.algae.surfaceAreaM2 = ConfigLoader.d(algae, "surface_area_m2", 500.0);
        s.algae.maxGrowthGM2Day = ConfigLoader.d(algae, "max_growth_g_m2_day", 40.0);
        s.algae.co2PerKgBiomass = ConfigLoader.d(algae, "co2_per_kg_biomass", 1.83);
        s.algae.harvestFraction = ConfigLoader.d(algae, "harvest_fraction", 0.80);
        s.algae.lossUa = ConfigLoader.d(algae, "loss_ua", 600.0);
        s.algae.spargeEfficiency = ConfigLoader.d(algae, "sparge_efficiency", 0.3);
        s.algae.temperature = ConfigLoader.d(algae, "initial_temp", 20.0);

        Map<String, Object> plastic = ConfigLoader.map(loads, "plastic_recycling");
        if (!plastic.isEmpty()) {
            s.plasticRecycling.directVolume = ConfigLoader.d(plastic, "direct_volume_m3", 80_000.0);
            s.plasticRecycling.directTemp = ConfigLoader.d(plastic, "initial_temp", 22.0);
            s.plasticRecycling.directSetpoint = ConfigLoader.d(plastic, "direct_setpoint_c", 65.0);
            s.plasticRecycling.directLossUa = ConfigLoader.d(plastic, "loss_ua", 3500.0);
            s.plasticRecycling.boostTargetTemp = ConfigLoader.d(plastic, "boost_target_c", 85.0);
            s.plasticRecycling.minSourceTemp = ConfigLoader.d(plastic, "min_source_temp", 40.0);
            s.plasticRecycling.heatPumpCop = ConfigLoader.d(plastic, "heat_pump_cop", 3.2);
            s.plasticRecycling.hpCapacityW = ConfigLoader.d(plastic, "hp_capacity_w", 4_000_000.0);
        }

        ConvectionCaptureConfig convection = ConvectionCaptureConfig.fromYaml(config);
        s.passiveConvection.enabled = convection.convectionHybrid();
        if (convection.convectionHybrid()) {
            s.carbonCapture.regenerationTemp = convection.regenerationTemp;
            s.carbonCapture.minSourceTemp = convection.minSourceTemp;
            s.carbonCapture.heatPumpCop = convection.heatPumpCop;
            s.carbonCapture.specificHeatDutyJPerKg = convection.specificHeatDutyJPerKg;
            s.carbonCapture.hpCapacityW = convection.hpCapacityW;
        }

        return s;
    }
}
