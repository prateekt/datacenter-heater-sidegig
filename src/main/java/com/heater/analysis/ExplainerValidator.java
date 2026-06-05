package com.heater.analysis;

import java.util.ArrayList;
import java.util.List;

public final class ExplainerValidator {

    private ExplainerValidator() {}

    public record ValidationResult(boolean ok, List<String> warnings) {}

    public static ValidationResult validate(String markdown, ResultsSummary summary) {
        List<String> warnings = new ArrayList<>();
        String lower = markdown.toLowerCase();

        if (lower.contains("tbd") || lower.contains("placeholder")) {
            warnings.add("Output contains placeholder text");
        }

        for (String chart : summary.chartPaths()) {
            if (!markdown.contains(chart)) {
                warnings.add("Missing chart embed: " + chart);
            }
        }

        int numbersFound = 0;
        for (SweepPoint p : summary.points()) {
            String gwh = String.format("%.1f", p.thermal().annualizedRecoveredGwh());
            String tonnes = String.format("%.0f", p.annualizedNetTonnes());
            if (markdown.contains(gwh) || markdown.contains(tonnes)) {
                numbersFound++;
            }
        }
        if (numbersFound < Math.min(3, summary.points().size())) {
            warnings.add("Few simulation numbers appear in generated text");
        }

        for (String thermalChart : List.of(
                "thermal_service_vs_gpu_count.png",
                "thermal_by_generation.png",
                "thermal_load_split.png")) {
            if (!markdown.contains(thermalChart)) {
                warnings.add("Missing primary thermal chart: " + thermalChart);
            }
        }

        return new ValidationResult(warnings.isEmpty(), warnings);
    }
}
