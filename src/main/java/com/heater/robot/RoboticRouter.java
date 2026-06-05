package com.heater.robot;

import com.heater.model.LoadTarget;
import com.heater.model.RouterState;
import com.heater.model.SystemState;
import com.heater.model.TaskType;

import java.util.ArrayList;
import java.util.List;

public final class RoboticRouter {

    public final RouterConfig config;
    public RouterState state = RouterState.IDLE;
    public LoadTarget connectedLoad = LoadTarget.NONE;
    public RobotTask currentTask;
    public double timeSinceDecision;
    public final List<RobotTaskLog> taskLog = new ArrayList<>();

    public RoboticRouter(RouterConfig config) {
        this.config = config;
    }

    public double taskElapsed() {
        return currentTask != null ? currentTask.elapsed : 0.0;
    }

    public LoadTarget tick(SystemState system, double dt, boolean primarySafe) {
        double simTime = system.time;

        if (currentTask != null) {
            currentTask.tick(dt);
            if (currentTask.isComplete()) {
                finishTask(simTime);
            }
            return effectiveTarget();
        }

        timeSinceDecision += dt;
        if (timeSinceDecision < config.decisionInterval) {
            return effectiveTarget();
        }
        timeSinceDecision = 0.0;

        boolean ccsCanRun = system.buffer.temperature >= system.carbonCapture.minSourceTemp;
        boolean plasticBoostCanRun = system.buffer.temperature >= system.plasticRecycling.minSourceTemp;
        RouterContext ctx = new RouterContext(
                simTime,
                system.pool.temperature,
                system.pool.setpoint,
                system.aquaculture.temperature,
                system.aquaculture.setpoint,
                system.house.temperature,
                system.house.setpoint,
                system.buffer.temperature,
                system.algae.temperature,
                system.algae.optimalTemp,
                system.ambientTemp,
                primarySafe,
                ccsCanRun,
                system.plasticRecycling.directTemp,
                system.plasticRecycling.directSetpoint,
                plasticBoostCanRun
        );
        LoadTarget desired = LoadSelector.selectDesiredLoad(ctx, config);

        if (desired == connectedLoad) {
            return effectiveTarget();
        }

        if (connectedLoad == LoadTarget.NONE) {
            startConnect(simTime, desired);
        } else if (desired == LoadTarget.NONE) {
            startDisconnect(simTime);
        } else {
            startSwitch(simTime, desired);
        }

        return effectiveTarget();
    }

    public void injectFaultDisconnect(double simTime) {
        if (connectedLoad != LoadTarget.NONE) {
            taskLog.add(new RobotTaskLog(simTime, "fault", connectedLoad, "load_disconnected"));
        }
        state = RouterState.FAULT;
        connectedLoad = LoadTarget.NONE;
        currentTask = null;
    }

    public LoadTarget effectiveTarget() {
        if (state == RouterState.CONNECTING || state == RouterState.SWITCHING) {
            return LoadTarget.NONE;
        }
        if (state == RouterState.FAULT) {
            return LoadTarget.NONE;
        }
        return connectedLoad;
    }

    private void startConnect(double simTime, LoadTarget target) {
        if (target == LoadTarget.NONE) return;
        state = RouterState.CONNECTING;
        currentTask = new RobotTask(TaskType.CONNECT, target, config.connectDuration);
        taskLog.add(new RobotTaskLog(simTime, "connect_start", target));
    }

    private void startDisconnect(double simTime) {
        state = RouterState.SWITCHING;
        currentTask = new RobotTask(TaskType.DISCONNECT, LoadTarget.NONE, config.connectDuration, connectedLoad);
        taskLog.add(new RobotTaskLog(simTime, "disconnect_start", connectedLoad));
    }

    private void startSwitch(double simTime, LoadTarget target) {
        state = RouterState.SWITCHING;
        currentTask = new RobotTask(TaskType.SWITCH, target, config.switchDuration, connectedLoad);
        taskLog.add(new RobotTaskLog(simTime, "switch_start", target, "from=" + connectedLoad.name().toLowerCase()));
    }

    private void finishTask(double simTime) {
        if (currentTask == null) return;

        switch (currentTask.taskType) {
            case CONNECT -> {
                connectedLoad = currentTask.target;
                state = RouterState.CONNECTED;
                taskLog.add(new RobotTaskLog(simTime, "connect_done", currentTask.target));
            }
            case DISCONNECT -> {
                connectedLoad = LoadTarget.NONE;
                state = RouterState.IDLE;
                taskLog.add(new RobotTaskLog(simTime, "disconnect_done", LoadTarget.NONE));
            }
            case SWITCH -> {
                connectedLoad = currentTask.target;
                state = RouterState.CONNECTED;
                taskLog.add(new RobotTaskLog(simTime, "switch_done", currentTask.target));
            }
        }
        currentTask = null;
    }

    public void applyToSystem(SystemState system) {
        system.pool.connected = connectedLoad == LoadTarget.POOL;
        system.aquaculture.connected = connectedLoad == LoadTarget.AQUACULTURE;
        system.house.connected = connectedLoad == LoadTarget.HOUSE;
        system.carbonCapture.connected = connectedLoad == LoadTarget.CARBON_CAPTURE;
        system.algae.connected = connectedLoad == LoadTarget.ALGAE;
        system.plasticRecycling.connected = connectedLoad == LoadTarget.PLASTIC_RECYCLING;
    }
}
