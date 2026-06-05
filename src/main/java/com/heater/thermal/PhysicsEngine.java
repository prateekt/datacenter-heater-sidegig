package com.heater.thermal;

import com.heater.carbon.AlgaeGrowthModel;
import com.heater.carbon.CarbonCaptureModel;
import com.heater.carbon.ConvectionCaptureModel;
import com.heater.carbon.PlasticRecyclingModel;
import com.heater.model.ActuatorState;
import com.heater.model.SystemState;

public final class PhysicsEngine {

    private final PhysicsConfig cfg;

    public PhysicsEngine(PhysicsConfig cfg) {
        this.cfg = cfg;
    }

    public void applyHxAndLoads(SystemState state, ActuatorState actuators, double dt) {
        double cp = cfg.cpWater;

        double rejectFrac = Math.max(0.0, Math.min(1.0, actuators.rejectFraction));
        double qReject = Math.min(rejectFrac * cfg.rejectCapacity, state.primary.qWaste);

        double tPrimaryPreHx = state.primary.tIn;
        if (state.primary.mdot > 0) {
            tPrimaryPreHx = state.primary.tIn + state.primary.qWaste / (state.primary.mdot * cp);
        }

        double mdotSec = actuators.secondaryFlowKgS;
        HeatExchangerCalc.HxResult hx = HeatExchangerCalc.compute(
                tPrimaryPreHx,
                state.buffer.temperature,
                state.primary.mdot,
                mdotSec,
                cfg.uaHx,
                cp
        );

        state.primary.tOut = hx.tPrimaryOut();
        state.hx.qTransfer = hx.qTransferW();
        state.hx.tPrimaryOutHx = hx.tPrimaryOut();
        state.hx.tSecondaryOut = hx.tSecondaryOut();

        updateBuffer(state, hx.qTransferW(), mdotSec, cp, dt);
        double qPool = updatePool(state, actuators, cp, dt);
        double qAqua = updateAquaculture(state, actuators, cp, dt);
        double qHouse = updateHouse(state, actuators, cp, dt);
        AlgaeGrowthModel.updateThermal(state.algae, state.ambientTemp, actuators, cp, dt);

        PlasticRecyclingModel.IntegrationResult plastic = PlasticRecyclingModel.integrate(
                state.plasticRecycling,
                state.buffer.temperature,
                mdotSec,
                actuators.plasticValveOpen,
                state.ambientTemp,
                cp,
                dt
        );
        PlasticRecyclingModel.accumulateElectric(state, plastic.electricW(), dt);

        double dacRateBefore = state.carbonCapture.currentCaptureRateKgS;
        CarbonCaptureModel.CaptureResult capture;
        ConvectionCaptureModel.IntegrationResult convection = ConvectionCaptureModel.integrate(
                state, cfg.convection, qReject, actuators, cp, dt
        );

        if (cfg.convection.convectionHybrid()) {
            capture = new CarbonCaptureModel.CaptureResult(
                    convection.qRegenW(), convection.qRegenW(), convection.regenElectricW()
            );
            state.convectionCo2CapturedKg = state.passiveConvection.co2CapturedKg;
            if (state.passiveConvection.phase == com.heater.carbon.ConvectionCaptureCycle.Phase.ADSORB) {
                state.carbonCapture.currentCaptureRateKgS = convection.captureRateKgS();
            }
        } else {
            capture = CarbonCaptureModel.integrate(
                    state.carbonCapture,
                    state.buffer.temperature,
                    mdotSec,
                    actuators.ccsValveOpen,
                    cp,
                    dt
            );
            CarbonCaptureModel.accumulateElectric(state, capture.electricW(), dt);
        }

        AlgaeGrowthModel.integrateGrowth(
                state.algae,
                state.time,
                dacRateBefore > 0 ? dacRateBefore : state.carbonCapture.currentCaptureRateKgS,
                dt
        );

        double qAlgae = 0.0;
        if (state.algae.connected && actuators.algaeValveOpen && mdotSec > 0) {
            qAlgae = mdotSec * 0.35 * cp * Math.max(0.0, state.algae.optimalTemp - state.algae.temperature);
        }
        double qDac = state.carbonCapture.connected && actuators.ccsValveOpen && mdotSec > 0
                ? capture.qSourceDrawW() : 0.0;
        double qPlasticDirect = state.plasticRecycling.connected && actuators.plasticValveOpen
                ? plastic.qDirectW() : 0.0;
        double qPlasticBoost = state.plasticRecycling.connected && actuators.plasticValveOpen
                ? plastic.qSourceDrawW() : 0.0;

        state.energyPoolJ += qPool * dt;
        state.energyAquacultureJ += qAqua * dt;
        state.energyHouseJ += qHouse * dt;
        state.energyAlgaeJ += qAlgae * dt;
        state.energyDacJ += qDac * dt;
        state.energyPlasticDirectJ += qPlasticDirect * dt;
        state.energyPlasticBoostJ += qPlasticBoost * dt;

        double qDelivered = qPool + qAqua + qHouse + qAlgae + qDac + qPlasticDirect + qPlasticBoost;
        qDelivered = Math.min(qDelivered, hx.qTransferW() + capture.qSourceDrawW() + plastic.qSourceDrawW());
        state.energyRecoveredJ += qDelivered * dt;
        // First-law balance: do not count dry-cooler duty that overlaps with delivered service.
        double qRejected = Math.max(0.0, Math.min(qReject, state.primary.qWaste - qDelivered));
        state.energyRejectedJ += qRejected * dt;

        if (state.primary.tOut > cfg.primaryTMax) {
            state.primaryUnsafeTimeS += dt;
        }
        boolean convectionActive = cfg.convection.convectionHybrid()
                && state.passiveConvection.enabled
                && state.passiveConvection.currentCaptureRateKgS > 0;
        if ((state.carbonCapture.connected && state.carbonCapture.currentCaptureRateKgS > 0)
                || convectionActive
                || (state.algae.connected && state.algae.currentGrowthRateKgS > 0)) {
            state.ccsActiveSteps++;
        }
    }

    private void updateBuffer(SystemState state, double qIn, double mdot, double cp, double dt) {
        double mass = state.buffer.volume;
        if (mass <= 0) return;
        double energy = qIn * dt;
        if (mdot > 0) {
            double tMix = state.buffer.temperature + qIn / (mdot * cp);
            state.buffer.temperature += energy / (mass * cp);
            state.buffer.temperature = 0.5 * state.buffer.temperature + 0.5 * tMix;
        } else {
            state.buffer.temperature += energy / (mass * cp);
        }
    }

    private double updatePool(SystemState state, ActuatorState actuators, double cp, double dt) {
        var pool = state.pool;
        double qLoss = pool.lossUa * (pool.temperature - state.ambientTemp);
        double qGain = 0.0;
        if (pool.connected && actuators.poolValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.3 * cp
                    * Math.max(0.0, pool.setpoint - pool.temperature);
        }
        pool.temperature += (qGain - qLoss) * dt / (pool.volume * cp);
        return qGain;
    }

    private double updateAquaculture(SystemState state, ActuatorState actuators, double cp, double dt) {
        var aqua = state.aquaculture;
        double qLoss = aqua.lossUa * (aqua.temperature - state.ambientTemp);
        double qGain = 0.0;
        if (aqua.connected && actuators.poolValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.35 * cp
                    * Math.max(0.0, aqua.setpoint - aqua.temperature);
        }
        aqua.temperature += (qGain - qLoss) * dt / (aqua.volume * cp);
        return qGain;
    }

    private double updateHouse(SystemState state, ActuatorState actuators, double cp, double dt) {
        var house = state.house;
        double qLoss = house.lossUa * (house.temperature - state.ambientTemp);
        double qGain = 0.0;
        if (house.connected && actuators.houseValveOpen && actuators.secondaryFlowKgS > 0) {
            qGain = actuators.secondaryFlowKgS * 0.4 * cp
                    * Math.max(0.0, house.setpoint - house.temperature);
        }
        double thermalMassJK = house.thermalMass * 1000.0;
        house.temperature += (qGain - qLoss) * dt / thermalMassJK;
        return qGain;
    }
}
