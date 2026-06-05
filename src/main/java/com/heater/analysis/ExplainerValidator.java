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
            String tonnes = String.format("%.0f", p.annualizedNetTonnes());
            if (markdown.contains(tonnes) || markdown.contains(String.format("%.1f", p.annualizedNetTonnes()))) {
                numbersFound++;
            }
        }
        if (numbersFound < Math.min(3, summary.points().size())) {
            warnings.add("Few simulation numbers appear in generated text");
        }

        return new ValidationResult(warnings.isEmpty(), warnings);
    }
}
