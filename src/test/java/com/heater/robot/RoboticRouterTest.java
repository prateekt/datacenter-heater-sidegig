package com.heater.robot;

import com.heater.model.LoadTarget;
import com.heater.model.RouterState;
import com.heater.model.SystemState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoboticRouterTest {

    @Test
    void climatePrioritySelectsHouseWhenCold() {
        RouterConfig cfg = new RouterConfig();
        cfg.decisionInterval = 0;
        cfg.connectDuration = 5;
        RoboticRouter router = new RoboticRouter(cfg);
        SystemState s = new SystemState();
        s.ambientTemp = 2;
        s.house.temperature = 18;

        for (int i = 0; i < 20; i++) {
            router.tick(s, 1, true);
            router.applyToSystem(s);
        }
        assertEquals(LoadTarget.HOUSE, router.connectedLoad);
        assertTrue(s.house.connected);
    }

    @Test
    void plasticPrioritySelectsPlasticLoad() {
        RouterConfig cfg = new RouterConfig();
        cfg.decisionInterval = 0;
        cfg.connectDuration = 5;
        cfg.priority = List.of(LoadTarget.PLASTIC_RECYCLING, LoadTarget.CARBON_CAPTURE);
        RoboticRouter router = new RoboticRouter(cfg);
        SystemState s = new SystemState();
        s.plasticRecycling.directTemp = 40.0;
        s.plasticRecycling.directSetpoint = 65.0;
        s.buffer.temperature = 50.0;
        s.plasticRecycling.minSourceTemp = 40.0;

        for (int i = 0; i < 20; i++) {
            router.tick(s, 1, true);
            router.applyToSystem(s);
        }
        assertEquals(LoadTarget.PLASTIC_RECYCLING, router.connectedLoad);
        assertTrue(s.plasticRecycling.connected);
    }

    @Test
    void faultSetsFaultState() {
        RoboticRouter router = new RoboticRouter(new RouterConfig());
        router.connectedLoad = LoadTarget.CARBON_CAPTURE;
        router.state = RouterState.CONNECTED;
        router.injectFaultDisconnect(100);
        assertEquals(RouterState.FAULT, router.state);
        assertEquals(LoadTarget.NONE, router.connectedLoad);
    }
}
