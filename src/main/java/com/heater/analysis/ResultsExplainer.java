package com.heater.analysis;

import com.heater.config.ConfigLoader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class ResultsExplainer {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final Path explainPromptPath;

    public ResultsExplainer() {
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.baseUrl = envOr("OPENAI_BASE_URL", "https://api.openai.com/v1");
        this.model = envOr("OPENAI_MODEL", "gpt-4o-mini");
        this.explainPromptPath = Path.of("config/explain_prompt.yaml");
    }

    public String explain(ResultsSummary summary, String gpuProfilesPath) throws IOException {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("LLM skipped — using template fallback.");
            return TemplateExplainer.explain(summary, gpuProfilesPath);
        }
        Map<String, Object> promptCfg = ConfigLoader.load(explainPromptPath.toString());
        String systemPrompt = promptCfg.getOrDefault("system_prompt", "").toString();
        String outline = promptCfg.getOrDefault("section_outline", "").toString();

        String userContent = outline + "\n\n---\n\nSimulation JSON:\n" + summary.toJson()
                + "\n\nGPU profiles path: " + gpuProfilesPath
                + "\n\nChart paths:\n" + String.join("\n", summary.chartPaths());

        String response = callChatApi(systemPrompt, userContent);
        ExplainerValidator.ValidationResult v = ExplainerValidator.validate(response, summary);
        if (!v.ok()) {
            System.err.println("LLM output validation warnings:");
            v.warnings().forEach(w -> System.err.println("  - " + w));
        }
        return response;
    }

    private String callChatApi(String systemPrompt, String userContent) throws IOException {
        try {
            String body = buildRequestBody(systemPrompt, userContent);
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl.replaceAll("/$", "") + "/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMinutes(3))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("LLM API error " + response.statusCode() + ": " + response.body());
            }
            return extractContent(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("LLM call interrupted", e);
        }
    }

    private String buildRequestBody(String systemPrompt, String userContent) {
        return "{"
                + "\"model\":" + jsonString(model) + ","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
                + "{\"role\":\"user\",\"content\":" + jsonString(userContent) + "}"
                + "],"
                + "\"temperature\":0.4"
                + "}";
    }

    private static String extractContent(String responseBody) throws IOException {
        int idx = responseBody.indexOf("\"content\"");
        if (idx < 0) {
            throw new IOException("No content in LLM response");
        }
        int start = responseBody.indexOf(':', idx) + 1;
        while (start < responseBody.length() && Character.isWhitespace(responseBody.charAt(start))) {
            start++;
        }
        if (start >= responseBody.length() || responseBody.charAt(start) != '"') {
            throw new IOException("Unexpected LLM response format");
        }
        StringBuilder content = new StringBuilder();
        boolean escape = false;
        for (int i = start + 1; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> content.append('\n');
                    case 't' -> content.append('\t');
                    case '"' -> content.append('"');
                    case '\\' -> content.append('\\');
                    default -> content.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                content.append(c);
            }
        }
        return content.toString().trim();
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : def;
    }
}
