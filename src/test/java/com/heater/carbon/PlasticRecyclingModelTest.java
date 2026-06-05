package com.heater.carbon;

import com.heater.model.PlasticRecyclingLoad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlasticRecyclingModelTest {

    @Test
    void directHeatingWhenValveOpen() {
        PlasticRecyclingLoad load = new PlasticRecyclingLoad();
        load.connected = true;
        load.directTemp = 40.0;
        load.directSetpoint = 65.0;
        load.directVolume = 10_000.0;
        load.directLossUa = 100.0;

        var r = PlasticRecyclingModel.integrate(load, 50.0, 5.0, true, 20.0, 4186, 3600.0);
        assertTrue(r.qDirectW() > 0);
        assertTrue(load.directTemp > 40.0);
    }

    @Test
    void boostStallsBelowMinSourceTemp() {
        PlasticRecyclingLoad load = new PlasticRecyclingLoad();
        load.connected = true;
        load.minSourceTemp = 40.0;
        load.hpCapacityW = 500_000;

        var r = PlasticRecyclingModel.integrate(load, 35.0, 6.0, true, 20.0, 4186, 1.0);
        assertEquals(0, r.qBoostW(), 1e-6);
        assertEquals(0, r.electricW(), 1e-6);
    }

    @Test
    void boostDrawsWhenBufferHotEnough() {
        PlasticRecyclingLoad load = new PlasticRecyclingLoad();
        load.connected = true;
        load.minSourceTemp = 40.0;
        load.hpCapacityW = 500_000;
        load.heatPumpCop = 3.0;

        var r = PlasticRecyclingModel.integrate(load, 52.0, 8.0, true, 20.0, 4186, 3600.0);
        assertTrue(r.qBoostW() > 0);
        assertTrue(r.electricW() > 0);
        assertTrue(load.boostHeatDeliveredJ > 0);
    }

    @Test
    void noHeatWhenDisconnected() {
        PlasticRecyclingLoad load = new PlasticRecyclingLoad();
        load.connected = false;
        load.directTemp = 30.0;
        load.directLossUa = 0.0;

        var r = PlasticRecyclingModel.integrate(load, 55.0, 8.0, true, 20.0, 4186, 3600.0);
        assertEquals(0, r.qDirectW(), 1e-6);
        assertEquals(0, r.qBoostW(), 1e-6);
        assertEquals(30.0, load.directTemp, 0.1);
    }
}
