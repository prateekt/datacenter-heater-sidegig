package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

public final class ClimateAnalogies {

    private final double iceCarTonnesPerYear;
    private final double evCarTonnesPerYear;
    private final double householdTonnesPerYear;
    private final double coverCropAcreTonnesPerYear;
    private final double usFarmAvgAcres;
    private final double usAgSectorMillionTonnes;
    private final double usTotalMillionTonnes;
    private final double globalBillionTonnes;
    private final double flightTonnes;
    private final double milesPerTonneIceCar;

    public ClimateAnalogies(
            double iceCarTonnesPerYear,
            double evCarTonnesPerYear,
            double householdTonnesPerYear,
            double coverCropAcreTonnesPerYear,
            double usFarmAvgAcres,
            double usAgSectorMillionTonnes,
            double usTotalMillionTonnes,
            double globalBillionTonnes,
            double flightTonnes,
            double milesPerTonneIceCar
    ) {
        this.iceCarTonnesPerYear = iceCarTonnesPerYear;
        this.evCarTonnesPerYear = evCarTonnesPerYear;
        this.householdTonnesPerYear = householdTonnesPerYear;
        this.coverCropAcreTonnesPerYear = coverCropAcreTonnesPerYear;
        this.usFarmAvgAcres = usFarmAvgAcres;
        this.usAgSectorMillionTonnes = usAgSectorMillionTonnes;
        this.usTotalMillionTonnes = usTotalMillionTonnes;
        this.globalBillionTonnes = globalBillionTonnes;
        this.flightTonnes = flightTonnes;
        this.milesPerTonneIceCar = milesPerTonneIceCar;
    }

    public static ClimateAnalogies loadDefault() throws IOException {
        return load("config/impact_analogies.yaml");
    }

    public static ClimateAnalogies load(String path) throws IOException {
        Map<String, Object> cfg = ConfigLoader.load(path);
        return new ClimateAnalogies(
                ConfigLoader.d(cfg, "us_ice_car_tonnes_per_year", 4.6),
                ConfigLoader.d(cfg, "us_ev_car_tonnes_per_year", 2.0),
                ConfigLoader.d(cfg, "us_household_tonnes_per_year", 8.5),
                ConfigLoader.d(cfg, "cover_crop_acre_tonnes_per_year", 0.5),
                ConfigLoader.d(cfg, "us_farm_avg_acres", 445),
                ConfigLoader.d(cfg, "us_ag_sector_million_tonnes_per_year", 598),
                ConfigLoader.d(cfg, "us_total_million_tonnes_per_year", 5000),
                ConfigLoader.d(cfg, "global_billion_tonnes_per_year", 36),
                ConfigLoader.d(cfg, "flight_nyc_london_tonnes", 1.0),
                ConfigLoader.d(cfg, "miles_per_tonne_ice_car", 11_386.0)
        );
    }

    public double iceCarsFromTonnes(double tonnes) {
        return tonnes / iceCarTonnesPerYear;
    }

    public double evCarsFromTonnes(double tonnes) {
        return tonnes / evCarTonnesPerYear;
    }

    public double annualizeKg(double kg, double simDurationS) {
        if (simDurationS <= 0) return 0;
        return kg * (365.0 * 86400.0 / simDurationS) / 1000.0;
    }

    public String formatTonnes(double tonnes) {
        if (tonnes >= 1_000_000) {
            return String.format(Locale.US, "%.2f million tonnes CO₂e/year", tonnes / 1_000_000.0);
        }
        if (tonnes >= 1_000) {
            return String.format(Locale.US, "%,.0f tonnes CO₂e/year", tonnes);
        }
        return String.format(Locale.US, "%.0f tonnes CO₂e/year", tonnes);
    }

    public String formatCars(double cars) {
        if (cars >= 1_000_000) {
            return String.format(Locale.US, "~%.1f million cars", cars / 1_000_000.0);
        }
        if (cars >= 1_000) {
            return String.format(Locale.US, "~%,.0f cars", cars);
        }
        return String.format(Locale.US, "~%.0f cars", cars);
    }

    /**
     * Rich scale narrative: tonnes first, then layered analogies for intuition.
     * Cars framed with electrification caveat; includes agriculture and national context.
     */
    public String scaleNarrative(double tonnesPerYear) {
        double iceCars = iceCarsFromTonnes(tonnesPerYear);
        double evCars = evCarsFromTonnes(tonnesPerYear);
        double households = tonnesPerYear / householdTonnesPerYear;
        double coverCropAcres = tonnesPerYear / coverCropAcreTonnesPerYear;
        double avgFarms = coverCropAcres / usFarmAvgAcres;
        double pctUs = 100.0 * tonnesPerYear / (usTotalMillionTonnes * 1_000_000.0);
        double pctUsAg = 100.0 * tonnesPerYear / (usAgSectorMillionTonnes * 1_000_000.0);
        double pctGlobal = 100.0 * tonnesPerYear / (globalBillionTonnes * 1_000_000_000.0);
        double flights = tonnesPerYear / flightTonnes;

        return String.format(Locale.US,
                "**%,.0f tonnes CO₂e per year** net removed. "
                        + "Scale: **%.3f%%** of U.S. emissions, **%.2f%%** of U.S. agriculture sector emissions, "
                        + "**%.4f%%** of global anthropogenic CO₂. "
                        + "Transport intuition (declining relevance as grids electrify): equivalent to **%s** "
                        + "gasoline cars parked for a year, or **%s** EVs on today's U.S. grid. "
                        + "Agriculture intuition: like running a **cover-crop carbon program on ~%,.0f acres** "
                        + "(~%.0f average-sized U.S. farms at ~%.0f acres each). "
                        + "Also **~%,.0f homes'** annual energy emissions, or **~%,.0f** NYC–London round-trip flights.",
                tonnesPerYear, pctUs, pctUsAg, pctGlobal,
                formatCars(iceCars), formatCars(evCars),
                coverCropAcres, avgFarms, usFarmAvgAcres,
                households, flights);
    }

    public String chartSubtitleTonnes() {
        return "Y-axis: net CO₂e removed (metric tonnes per year, annualized from simulation)";
    }

    public String electrificationNote() {
        return String.format(Locale.US,
                "As the U.S. grid decarbonizes, **GPU operational CO₂ falls** but **waste heat remains** — "
                        + "DAC's job is still to use that heat. "
                        + "Gasoline-car analogies (%.1f t/car) overstate the future; "
                        + "EV analogies (%.1f t/car on today's grid) are a better tailpipe mental model. "
                        + "**Tonnes and %% recovery** stay the right metrics either way.",
                iceCarTonnesPerYear, evCarTonnesPerYear);
    }
}
