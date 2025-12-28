package com.mockinterview.compiler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CodapiService {

    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))
            .setReadTimeout(Duration.ofSeconds(15))
            .build();

    private static final String CODAPI_URL = "https://api.codapi.org/v1/exec";
    private static final int MAX_RETRIES = 2;

    private static final Map<String, String> LANGUAGE_MAP = Map.of(
            "python", "python",
            "javascript", "javascript",
            "java", "java",
            "cpp", "cpp",
            "c", "c",
            "csharp", "csharp",
            "go", "go",
            "rust", "rust"
    );

    public Map<String, Object> executeCode(String sourceCode, String language, String stdin) {

        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return createErrorResponse("Source code cannot be empty");
        }

        String codapiLang = LANGUAGE_MAP.get(language.toLowerCase());
        if (codapiLang == null) {
            return createErrorResponse("Unsupported language: " + language);
        }

        Exception lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("sandbox", codapiLang);

                Map<String, String> files = new HashMap<>();
                String fileName;
                String processedCode;

                if (language.equalsIgnoreCase("java")) {
                    Map<String, String> javaResult = processJavaCode(sourceCode);
                    fileName = javaResult.get("fileName");
                    processedCode = javaResult.get("code");

                    // ✅ THIS IS THE FIX
                    requestBody.put("command", "run " + javaResult.get("className"));
                } else {
                    requestBody.put("command", "run");
                    fileName = getFileNameForLanguage(language);
                    processedCode = sourceCode;
                }

                files.put(fileName, processedCode);
                requestBody.put("files", files);

                if (stdin != null && !stdin.trim().isEmpty()) {
                    requestBody.put("stdin", stdin);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("User-Agent", "MockInterview-App");

                HttpEntity<Map<String, Object>> request =
                        new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.postForEntity(
                        CODAPI_URL,
                        request,
                        Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return formatCodapiResponse(response.getBody());
                }

                lastException = new Exception("Execution failed");

            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        return createErrorResponse(
                lastException != null ? lastException.getMessage() : "Execution failed"
        );
    }

    private Map<String, String> processJavaCode(String sourceCode) {
        Map<String, String> result = new HashMap<>();
        String code = sourceCode.trim();

        String className = extractClassName(code);

        if (className != null) {
            result.put("fileName", className + ".java");
            result.put("code", code);
            result.put("className", className);
            return result;
        }

        String wrapped =
                "public class Main {\n" +
                "    public static void main(String[] args) throws Exception {\n" +
                indentCode(code, 2) +
                "\n    }\n" +
                "}";

        result.put("fileName", "Main.java");
        result.put("code", wrapped);
        result.put("className", "Main");
        return result;
    }

    private String extractClassName(String code) {
        try {
            String clean = code.replaceAll("//.*|/\\*.*?\\*/", "");
            var matcher = java.util.regex.Pattern
                    .compile("public\\s+class\\s+(\\w+)")
                    .matcher(clean);
            if (matcher.find()) return matcher.group(1);

            matcher = java.util.regex.Pattern
                    .compile("class\\s+(\\w+)")
                    .matcher(clean);
            if (matcher.find()) return matcher.group(1);
        } catch (Exception ignored) {}
        return null;
    }

    private String indentCode(String code, int tabs) {
        String indent = "    ".repeat(tabs);
        return indent + code.replace("\n", "\n" + indent);
    }

    private String getFileNameForLanguage(String language) {
        switch (language.toLowerCase()) {
            case "python": return "main.py";
            case "javascript": return "main.js";
            case "cpp": return "main.cpp";
            case "c": return "main.c";
            case "csharp": return "Main.cs";
            case "go": return "main.go";
            case "rust": return "main.rs";
            default: return "main.txt";
        }
    }

    private Map<String, Object> formatCodapiResponse(Map<String, Object> response) {
        Map<String, Object> result = new HashMap<>();
        boolean ok = Boolean.TRUE.equals(response.get("ok"));
        result.put("success", ok);
        result.put("stdout", response.getOrDefault("stdout", ""));
        result.put("stderr", response.getOrDefault("stderr", ""));
        result.put("status", ok ? "Accepted" : "Error");
        result.put("statusId", ok ? 3 : 4);
        return result;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("status", "Error");
        error.put("statusId", 4);
        return error;
    }

    public List<String> getSupportedLanguages() {
        return new ArrayList<>(LANGUAGE_MAP.keySet());
    }
}
