package com.mockinterview.compiler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CodapiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String CODAPI_URL = "https://api.codapi.org/v1/exec";

    // Language mapping for Codapi
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
        try {
            System.out.println("Executing code with Codapi");
            System.out.println("Language: " + language);
            
            String codapiLang = LANGUAGE_MAP.get(language.toLowerCase());
            if (codapiLang == null) {
                return createErrorResponse("Unsupported language: " + language);
            }

            // Build request body for Codapi
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("sandbox", codapiLang);
            requestBody.put("command", "run");
            requestBody.put("files", Map.of(
                getFileName(language), sourceCode
            ));
            
            if (stdin != null && !stdin.trim().isEmpty()) {
                requestBody.put("stdin", stdin);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            // Call Codapi API
            ResponseEntity<Map> response = restTemplate.postForEntity(
                CODAPI_URL,
                request,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return formatCodapiResponse(response.getBody());
            } else {
                return createErrorResponse("Execution failed with status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("Error executing code with Codapi: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Execution failed: " + e.getMessage());
        }
    }

    private Map<String, Object> formatCodapiResponse(Map<String, Object> codapiResponse) {
        Map<String, Object> formatted = new HashMap<>();

        try {
            boolean success = codapiResponse.containsKey("ok") && 
                             Boolean.TRUE.equals(codapiResponse.get("ok"));
            
            formatted.put("success", success);
            
            // Get stdout
            if (codapiResponse.containsKey("stdout")) {
                formatted.put("stdout", codapiResponse.get("stdout"));
            }
            
            // Get stderr
            if (codapiResponse.containsKey("stderr")) {
                String stderr = (String) codapiResponse.get("stderr");
                if (stderr != null && !stderr.trim().isEmpty()) {
                    formatted.put("stderr", stderr);
                }
            }
            
            // Get error
            if (codapiResponse.containsKey("error")) {
                formatted.put("error", codapiResponse.get("error"));
            }
            
            // Status
            if (success) {
                formatted.put("status", "Accepted");
                formatted.put("statusId", 3);
            } else {
                formatted.put("status", "Error");
                formatted.put("statusId", 4);
            }
            
            // Execution time (if available)
            if (codapiResponse.containsKey("duration")) {
                Object duration = codapiResponse.get("duration");
                if (duration instanceof Number) {
                    formatted.put("time", String.format("%.3f", ((Number) duration).doubleValue() / 1000.0));
                }
            }

        } catch (Exception e) {
            System.err.println("Error formatting Codapi response: " + e.getMessage());
            formatted.put("error", "Error formatting result");
            formatted.put("success", false);
        }

        return formatted;
    }

    private String getFileName(String language) {
        switch (language.toLowerCase()) {
            case "python": return "main.py";
            case "javascript": return "main.js";
            case "java": return "Main.java";
            case "cpp": return "main.cpp";
            case "c": return "main.c";
            case "csharp": return "Main.cs";
            case "go": return "main.go";
            case "rust": return "main.rs";
            default: return "main.txt";
        }
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