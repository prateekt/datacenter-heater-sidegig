package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class AcousticAnalogies {

    public final String oneLiner;
    public final String disclaimer;
    public final Map<String, String> concepts;
    public final List<QuestionAnswer> kidQuestions;
    public final List<String> steps;
    public final List<String> honestLimits;

    public record QuestionAnswer(String question, String answer) {}

    private AcousticAnalogies(
            String oneLiner, String disclaimer, Map<String, String> concepts,
            List<QuestionAnswer> kidQuestions, List<String> steps, List<String> honestLimits
    ) {
        this.oneLiner = oneLiner;
        this.disclaimer = disclaimer;
        this.concepts = concepts;
        this.kidQuestions = kidQuestions;
        this.steps = steps;
        this.honestLimits = honestLimits;
    }

    public static AcousticAnalogies load(String path) throws IOException {
        Map<String, Object> root = ConfigLoader.load(path);
        Map<String, String> concepts = new LinkedHashMap<>();
        Object conceptsObj = root.get("concepts");
        if (conceptsObj instanceof Map<?, ?> m) {
            m.forEach((k, v) -> concepts.put(k.toString(), v.toString()));
        }
        List<QuestionAnswer> questions = new ArrayList<>();
        Object qObj = root.get("kid_questions");
        if (qObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    questions.add(new QuestionAnswer(String.valueOf(m.get("q")), String.valueOf(m.get("a"))));
                }
            }
        }
        return new AcousticAnalogies(
                String.valueOf(root.getOrDefault("one_liner", "")),
                String.valueOf(root.getOrDefault("disclaimer", "")),
                concepts,
                questions,
                stringList(root, "steps"),
                stringList(root, "honest_limits")
        );
    }

    private static List<String> stringList(Map<String, Object> root, String key) {
        Object v = root.get(key);
        if (v instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
