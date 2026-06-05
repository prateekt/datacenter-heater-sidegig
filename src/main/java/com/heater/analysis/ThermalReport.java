package com.heater.analysis;

import com.heater.model.SystemState;
import com.heater.thermal.ScenarioProfile;
import com.heater.thermal.Simulator;

public record ThermalReport(
        double wasteHeatAvgMw,
        double wasteHeatAnnualGwh,
        double recoveredMwh,
        double rejectedMwh,
        double poolMwh,
        double aquacultureMwh,
        double houseMwh,
        double algaeMwh,
        double dacMwh,
        double heatPumpElectricMwh,
        double meanBufferTempC,
        double meanPoolTempC,
        double meanAquacultureTempC,
        double meanAlgaeTempC,
        double meanPrimaryTOutC
) {

    public double annualizedRecoveredGwh() {
        return recoveredMwh / 1000.0;
    }

    public ThermalReport multiply(double factor) {
        if (factor == 1.0) return this;
        return new ThermalReport(
                wasteHeatAvgMw * factor,
                wasteHeatAnnualGwh * factor,
                recoveredMwh * factor,
                rejectedMwh * factor,
                poolMwh * factor,
                aquacultureMwh * factor,
                houseMwh * factor,
                algaeMwh * factor,
                dacMwh * factor,
                heatPumpElectricMwh * factor,
                meanBufferTempC,
                meanPoolTempC,
                meanAquacultureTempC,
                meanAlgaeTempC,
                meanPrimaryTOutC
        );
    }

    public static ThermalReport fromSimulator(
            Simulator sim,
            ScenarioProfile scenario,
            double simDurationS,
            double wasteHeatAvgMw,
            int halls
    ) {
        double annualFactor = simDurationS > 0 ? (365.0 * 86400.0 / simDurationS) : 0.0;
        SystemState s = sim.state;
        ThermalReport base = new ThermalReport(
                wasteHeatAvgMw,
                wasteHeatAvgMw * 8760.0 / 1000.0,
                joulesToAnnualMwh(s.energyRecoveredJ, annualFactor),
                joulesToAnnualMwh(s.energyRejectedJ, annualFactor),
                joulesToAnnualMwh(s.energyPoolJ, annualFactor),
                joulesToAnnualMwh(s.energyAquacultureJ, annualFactor),
                joulesToAnnualMwh(s.energyHouseJ, annualFactor),
                joulesToAnnualMwh(s.energyAlgaeJ, annualFactor),
                joulesToAnnualMwh(s.energyDacJ, annualFactor),
                joulesToAnnualMwh(s.heatPumpElectricJ, annualFactor),
                sim.meanBufferTempC(),
                sim.meanPoolTempC(),
                sim.meanAquacultureTempC(),
                sim.meanAlgaeTempC(),
                sim.meanPrimaryTOutC()
        );
        return halls == 1 ? base : base.multiply(halls);
    }

    private static double joulesToAnnualMwh(double joules, double annualFactor) {
        return joules / 3_600_000_000.0 * annualFactor;
    }
}
