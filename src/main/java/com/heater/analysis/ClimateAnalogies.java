package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public final class ClimateAnalogies {

    private final double carTonnesPerYear;
    private final double householdTonnesPerYear;
    private final double perCapitaTonnesPerYear;
    private final double flightTonnes;
    private final double treeKgPerYear;
    private final double milesPerTonnesCar;

    public ClimateAnalogies(
            double carTonnesPerYear,
            double householdTonnesPerYear,
            double perCapitaTonnesPerYear,
            double flightTonnes,
            double treeKgPerYear,
            double milesPerTonnesCar
    ) {
        this.carTonnesPerYear = carTonnesPerYear;
        this.householdTonnesPerYear = householdTonnesPerYear;
        this.perCapitaTonnesPerYear = perCapitaTonnesPerYear;
        this.flightTonnes = flightTonnes;
        this.treeKgPerYear = treeKgPerYear;
        this.milesPerTonnesCar = milesPerTonnesCar;
    }

    public static ClimateAnalogies loadDefault() throws IOException {
        return load("config/impact_analogies.yaml");
    }

    public static ClimateAnalogies load(String path) throws IOException {
        Map<String, Object> cfg = ConfigLoader.load(path);
        return new ClimateAnalogies(
                ConfigLoader.d(cfg, "us_car_tonnes_per_year", 4.6),
                ConfigLoader.d(cfg, "us_household_tonnes_per_year", 8.5),
                ConfigLoader.d(cfg, "us_per_capita_tonnes_per_year", 16.0),
                ConfigLoader.d(cfg, "flight_nyc_london_tonnes", 1.0),
                ConfigLoader.d(cfg, "tree_kg_co2_per_year", 22.0),
                ConfigLoader.d(cfg, "miles_per_tonne_car_equiv", 11_386.0)
        );
    }

    public double carTonnesPerYear() {
        return carTonnesPerYear;
    }

    public double carsFromAnnualTonnes(double tonnesPerYear) {
        return tonnesPerYear / carTonnesPerYear;
    }

    public double carsFromKg(double kg, double simDurationS) {
        double annualTonnes = annualizeKg(kg, simDurationS);
        return carsFromAnnualTonnes(annualTonnes);
    }

    public double annualizeKg(double kg, double simDurationS) {
        if (simDurationS <= 0) return 0;
        return kg * (365.0 * 86400.0 / simDurationS) / 1000.0;
    }

    public String formatCars(double cars) {
        if (cars >= 1_000_000) {
            return String.format(Locale.US, "~%.1f million cars", cars / 1_000_000.0);
        }
        if (cars >= 10_000) {
            return String.format(Locale.US, "~%,.0f cars", cars);
        }
        if (cars >= 100) {
            return String.format(Locale.US, "~%,.0f cars", cars);
        }
        return String.format(Locale.US, "~%.0f cars", cars);
    }

    public String formatTonnesWithCars(double tonnesPerYear) {
        double cars = carsFromAnnualTonnes(tonnesPerYear);
        return String.format(Locale.US,
                "**%,.0f tonnes CO₂/year** — the same climate impact as taking **%s off U.S. roads for a year** "
                        + "(each car ≈ %.1f tonnes/year, EPA average)",
                tonnesPerYear, formatCars(cars), carTonnesPerYear);
    }

    public String explainRemoval(double tonnesPerYear) {
        double cars = carsFromAnnualTonnes(tonnesPerYear);
        double households = tonnesPerYear / householdTonnesPerYear;
        double flights = tonnesPerYear / flightTonnes;
        long miles = Math.round(tonnesPerYear * milesPerTonnesCar);

        return String.format(Locale.US,
                "That's **%s off U.S. roads for one year** — the same climate benefit as if those cars' tailpipes emitted nothing "
                        + "(≈ %.1f tonnes CO₂ per car, EPA average). "
                        + "Scientifically that is **%,.0f tonnes of CO₂ pulled from the air annually**. "
                        + "Also comparable to **~%,.0f homes'** yearly energy emissions, "
                        + "**~%,.0f NYC–London round-trip flights**, or **~%.1f million miles** of driving avoided.",
                formatCars(cars), carTonnesPerYear, tonnesPerYear,
                households, flights, miles / 1_000_000.0);
    }

    public String chartSubtitle() {
        return String.format(Locale.US,
                "Y-axis: cars off the road (1 car ≈ %.1f tonnes CO₂/year, U.S. EPA)", carTonnesPerYear);
    }
}
