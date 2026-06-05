package com.heater.thermal;

import com.heater.carbon.ClimateImpactCalculator;
import com.heater.config.ConfigLoader;
import com.heater.config.StateFactory;
import com.heater.control.AutomationController;
import com.heater.control.PidGains;
import com.heater.control.SafetyLimits;
import com.heater.model.ActuatorState;
import com.heater.model.LoadTarget;
import com.heater.model.SensorSnapshot;
import com.heater.model.SystemState;
import com.heater.robot.RoboticRouter;
import com.heater.robot.RouterConfig;
import com.heater.robot.RouterThresholds;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class Simulator {

    private final Map<String, Object> config;
    public final SystemState state;
    private final PhysicsEngine physics;
    private final AutomationController automation;
    private final RoboticRouter router;
    private final ClimateImpactCalculator climateCalc;
    private final double dt;
    private final double primaryTMax;

    private int poolInBandSteps;
    private int houseInBandSteps;
    private int totalSteps;
    private boolean faultInjected;
    private double lastSimDuration;
    private double sumBufferTemp;
    private double sumPoolTemp;
    private double sumAquacultureTemp;
    private double sumAlgaeTemp;
    private double sumPrimaryTOut;

    public Simulator(Map<String, Object> config) {
        this.config = config;
        this.state = StateFactory.buildInitialState(config);
        this.physics = new PhysicsEngine(physicsConfigFromYaml(config));

        Map<String, Object> ctrl = ConfigLoader.map(config, "control");
        Map<String, Object> safetyCfg = ConfigLoader.map(config, "safety");
        Map<String, Object> primaryCfg = ConfigLoader.map(config, "primary_loop");
        Map<String, Object> pid = ConfigLoader.map(ctrl, "pid");
        Map<String, Object> pidPrimary = ConfigLoader.map(pid, "primary");
        Map<String, Object> pidSecondary = ConfigLoader.map(pid, "secondary");

        this.automation = new AutomationController(
                new PidGains(
                        ConfigLoader.d(pidPrimary, "kp", 0.015),
                        ConfigLoader.d(pidPrimary, "ki", 0.002),
                        ConfigLoader.d(pidPrimary, "kd", 0.001)
                ),
                new PidGains(
                        ConfigLoader.d(pidSecondary, "kp", 0.02),
                        ConfigLoader.d(pidSecondary, "ki", 0.003),
                        ConfigLoader.d(pidSecondary, "kd", 0.0)
                ),
                ConfigLoader.d(primaryCfg, "t_in", 35.0) + 20.0,
                ConfigLoader.d(ctrl, "max_secondary_flow", 6.0),
                new SafetyLimits(
                        ConfigLoader.d(safetyCfg, "primary_t_trip", 63.0),
                        ConfigLoader.d(primaryCfg, "t_max", 65.0),
                        ConfigLoader.d(primaryCfg, "t_min_flow", 8.0),
                        ConfigLoader.d(safetyCfg, "secondary_dt_max", 25.0),
                        ConfigLoader.d(safetyCfg, "robot_task_timeout", 120.0)
                )
        );

        this.router = new RoboticRouter(routerConfigFromYaml(config, ctrl));
        Map<String, Object> climate = ConfigLoader.map(config, "climate");
        this.climateCalc = new ClimateImpactCalculator(
                ConfigLoader.d(climate, "grid_co2_kg_per_kwh", 0.4),
                ConfigLoader.d(climate, "climate_sensitivity", 0.8)
        );
        this.dt = ConfigLoader.d(ctrl, "dt", 1.0);
        this.primaryTMax = ConfigLoader.d(primaryCfg, "t_max", 65.0);
    }

    public ActuatorState step(ScenarioProfile scenario) {
        double hour = state.time / 3600.0;
        state.ambientTemp = ScenarioUtil.interpolateOutsideTemp(hour, scenario.outsideTemps());
        state.primary.qWaste = ScenarioUtil.qWasteAtTime(
                state.time, scenario.qWasteBase(), scenario.qWastePeak()
        );

        boolean primarySafe = state.primary.tOut < primaryTMax;
        SensorSnapshot sensors = SensorSnapshot.fromSystem(state, router.state);

        LoadTarget target = router.tick(state, dt, primarySafe);
        router.applyToSystem(state);

        double setpoint = setpointForTarget(target);
        ActuatorState actuators = automation.step(
                state, sensors, target, setpoint, dt, router.taskElapsed()
        );

        physics.applyHxAndLoads(state, actuators, dt);
        trackSatisfaction();
        trackTemperatures();
        state.time += dt;
        state.totalSteps++;
        totalSteps++;

        return actuators;
    }

    public SimulationMetrics run(ScenarioProfile scenario, Double faultAt) {
        lastSimDuration = scenario.duration();
        int steps = (int) (scenario.duration() / dt);
        for (int i = 0; i < steps; i++) {
            if (faultAt != null && !faultInjected && state.time >= faultAt) {
                router.injectFaultDisconnect(state.time);
                router.applyToSystem(state);
                faultInjected = true;
            }
            step(scenario);
        }
        return metrics();
    }

    public SimulationMetrics metrics() {
        int total = Math.max(1, totalSteps);
        return new SimulationMetrics(
                state.energyRecoveredJ / 3_600_000.0,
                state.energyRejectedJ / 3_600_000.0,
                state.primaryUnsafeTimeS,
                100.0 * poolInBandSteps / total,
                100.0 * houseInBandSteps / total,
                totalSteps,
                List.copyOf(router.taskLog),
                climateCalc.report(state, lastSimDuration > 0 ? lastSimDuration : state.time)
        );
    }

    private double setpointForTarget(LoadTarget target) {
        return switch (target) {
            case POOL -> state.pool.setpoint;
            case AQUACULTURE -> state.aquaculture.setpoint;
            case HOUSE -> state.house.setpoint;
            case BUFFER -> state.buffer.temperature + 5.0;
            case CARBON_CAPTURE -> state.carbonCapture.regenerationTemp;
            case ALGAE -> state.algae.optimalTemp;
            default -> 0.0;
        };
    }

    private void trackSatisfaction() {
        if (Math.abs(state.pool.temperature - state.pool.setpoint) <= 1.0) {
            poolInBandSteps++;
        }
        if (Math.abs(state.house.temperature - state.house.setpoint) <= 2.0) {
            houseInBandSteps++;
        }
    }

    private void trackTemperatures() {
        sumBufferTemp += state.buffer.temperature;
        sumPoolTemp += state.pool.temperature;
        sumAquacultureTemp += state.aquaculture.temperature;
        sumAlgaeTemp += state.algae.temperature;
        sumPrimaryTOut += state.primary.tOut;
    }

    public double meanBufferTempC() {
        return totalSteps > 0 ? sumBufferTemp / totalSteps : 0.0;
    }

    public double meanPoolTempC() {
        return totalSteps > 0 ? sumPoolTemp / totalSteps : 0.0;
    }

    public double meanAquacultureTempC() {
        return totalSteps > 0 ? sumAquacultureTemp / totalSteps : 0.0;
    }

    public double meanAlgaeTempC() {
        return totalSteps > 0 ? sumAlgaeTemp / totalSteps : 0.0;
    }

    public double meanPrimaryTOutC() {
        return totalSteps > 0 ? sumPrimaryTOut / totalSteps : 0.0;
    }

    private static PhysicsConfig physicsConfigFromYaml(Map<String, Object> config) {
        PhysicsConfig cfg = new PhysicsConfig();
        cfg.cpWater = ConfigLoader.d(ConfigLoader.map(config, "physics"), "cp_water", 4186.0);
        cfg.uaHx = ConfigLoader.d(ConfigLoader.map(config, "heat_exchanger"), "ua", 80_000.0);
        cfg.rejectCapacity = ConfigLoader.d(ConfigLoader.map(config, "primary_loop"), "reject_capacity", 500_000.0);
        cfg.primaryTMax = ConfigLoader.d(ConfigLoader.map(config, "primary_loop"), "t_max", 65.0);
        return cfg;
    }

    private static RouterConfig routerConfigFromYaml(Map<String, Object> config, Map<String, Object> ctrl) {
        Map<String, Object> robotCfg = ConfigLoader.map(config, "robot");
        Map<String, Object> th = ConfigLoader.map(robotCfg, "thresholds");
        RouterConfig rc = new RouterConfig();
        rc.connectDuration = ConfigLoader.d(robotCfg, "connect_duration", 15.0);
        rc.switchDuration = ConfigLoader.d(robotCfg, "switch_duration", 30.0);
        rc.houseOutsideThreshold = ConfigLoader.d(robotCfg, "house_outside_threshold", 10.0);
        rc.decisionInterval = ConfigLoader.d(ctrl, "robot_interval", 30.0);
        rc.thresholds = new RouterThresholds(
                ConfigLoader.d(th, "house_temp_delta", 1.0),
                ConfigLoader.d(th, "pool_temp_delta", 0.5),
                ConfigLoader.d(th, "buffer_charge_below", 45.0),
                ConfigLoader.d(th, "algae_temp_delta", 1.0)
        );
        List<String> priorityNames = ConfigLoader.stringList(robotCfg, "priority");
        if (!priorityNames.isEmpty()) {
            rc.priority = priorityNames.stream()
                    .map(com.heater.model.LoadTarget::fromString)
                    .filter(t -> t != LoadTarget.NONE || priorityNames.contains("none"))
                    .toList();
        }
        return rc;
    }
}
