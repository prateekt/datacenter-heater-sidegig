package com.heater.carbon;

import com.heater.model.PlasticRecyclingLoad;
import com.heater.model.SystemState;

public final class PlasticRecyclingModel {

    private PlasticRecyclingModel() {}

    public record IntegrationResult(
            double qDirectW,
            double qBoostW,
            double qSourceDrawW,
            double electricW
    ) {}

    public static IntegrationResult integrate(
            PlasticRecyclingLoad load,
            double sourceTemp,
            double secondaryFlowKgS,
            boolean valveOpen,
            double ambientTemp,
            double cp,
            double dt
    ) {
        double qLoss = load.directLossUa * (load.directTemp - ambientTemp);
        double qDirect = 0.0;
        if (load.connected && valveOpen && secondaryFlowKgS > 0) {
            qDirect = secondaryFlowKgS * 0.3 * cp
                    * Math.max(0.0, load.directSetpoint - load.directTemp);
        }
        load.directTemp += (qDirect - qLoss) * dt / (load.directVolume * cp);

        double qBoost = 0.0;
        double qSourceDraw = 0.0;
        double electricW = 0.0;
        if (load.connected && valveOpen && secondaryFlowKgS > 0 && sourceTemp >= load.minSourceTemp) {
            double qAvailable = secondaryFlowKgS * cp * Math.max(0.0, sourceTemp - load.minSourceTemp);
            qBoost = Math.min(qAvailable, load.hpCapacityW);
            qSourceDraw = qBoost;
            electricW = load.heatPumpCop > 0 ? qBoost / load.heatPumpCop : qBoost;
            load.boostDutyW = qBoost;
            load.boostHeatDeliveredJ += qBoost * dt;
        } else {
            load.boostDutyW = 0.0;
        }

        return new IntegrationResult(qDirect, qBoost, qSourceDraw, electricW);
    }

    public static void accumulateElectric(SystemState state, double electricW, double dt) {
        state.heatPumpElectricJ += electricW * dt;
    }
}
