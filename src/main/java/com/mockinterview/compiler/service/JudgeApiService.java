package com.mockinterview.compiler.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class JudgeApiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String JUDGE0_API_URL = "https://judge0-ce.p.rapidapi.com";
    private static final String RAPIDAPI_KEY = "YOUR_RAPIDAPI_KEY"; // Replace with your key
    private static final String RAPIDAPI_HOST = "judge0-ce.p.rapidapi.com";

    // Language ID mapping for Judge0
    private static final Map<String, Integer> LANGUAGE_IDS = Map.of(
        "java", 62,
        "python", 71,
        "javascript", 63,
        "cpp", 54,
        "c", 50,
        "csharp", 51,
        "go", 60,
        "rust", 73
    );

    public Map<String, Object> executeCode(String sourceCode, String language, String stdin) {
        try {
            // Step 1: Submit code
            String token = submitCode(sourceCode, language, stdin);
            
            if (token == null) {
                return createErrorResponse("Failed to submit code");
            }

            // Step 2: Poll for result with timeout
            Map<String, Object> result = pollForResult(token, 10); // 10 second timeout
            
            return result != null ? result : createErrorResponse("Execution timeout");

        } catch (Exception e) {
            System.err.println("Error executing code: " + e.getMessage());
            return createErrorResponse("Execution failed: " + e.getMessage());
        }
    }

    private String submitCode(String sourceCode, String language, String stdin) {
        try {
            Integer languageId = LANGUAGE_IDS.get(language.toLowerCase());
            if (languageId == null) {
                System.err.println("Unsupported language: " + language);
                return null;
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("source_code", Base64.getEncoder().encodeToString(sourceCode.getBytes()));
            requestBody.put("language_id", languageId);
            requestBody.put("stdin", Base64.getEncoder().encodeToString(stdin.getBytes()));

            HttpHeaders headers = createHeaders();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                JUDGE0_API_URL + "/submissions?base64_encoded=true&wait=false",
                request,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("token")) {
                return (String) responseBody.get("token");
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error submitting code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> pollForResult(String token, int maxAttempts) {
        try {
            for (int i = 0; i < maxAttempts; i++) {
                Thread.sleep(1000); // Wait 1 second between polls

                HttpHeaders headers = createHeaders();
                HttpEntity<Void> request = new HttpEntity<>(headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                    JUDGE0_API_URL + "/submissions/" + token + "?base64_encoded=true",
                    HttpMethod.GET,
                    request,
                    Map.class
                );

                Map<String, Object> result = response.getBody();
                if (result != null) {
                    Map<String, Object> status = (Map<String, Object>) result.get("status");
                    Integer statusId = (Integer) status.get("id");

                    // Status IDs: 1-2 = In Queue/Processing, 3 = Accepted, 4-15 = Various errors
                    if (statusId > 2) {
                        return formatResult(result);
                    }
                }
            }

            return createErrorResponse("Execution timeout");

        } catch (Exception e) {
            System.err.println("Error polling for result: " + e.getMessage());
            return createErrorResponse("Error getting result: " + e.getMessage());
        }
    }

    private Map<String, Object> formatResult(Map<String, Object> result) {
        Map<String, Object> formatted = new HashMap<>();

        try {
            Map<String, Object> status = (Map<String, Object>) result.get("status");
            formatted.put("status", status.get("description"));
            formatted.put("statusId", status.get("id"));

            // Decode base64 outputs
            if (result.containsKey("stdout") && result.get("stdout") != null) {
                String stdout = new String(Base64.getDecoder().decode((String) result.get("stdout")));
                formatted.put("stdout", stdout);
            }

            if (result.containsKey("stderr") && result.get("stderr") != null) {
                String stderr = new String(Base64.getDecoder().decode((String) result.get("stderr")));
                formatted.put("stderr", stderr);
            }

            if (result.containsKey("compile_output") && result.get("compile_output") != null) {
                String compileOutput = new String(Base64.getDecoder().decode((String) result.get("compile_output")));
                formatted.put("compile_output", compileOutput);
            }

            formatted.put("time", result.get("time"));
            formatted.put("memory", result.get("memory"));
            formatted.put("success", status.get("id").equals(3)); // 3 = Accepted

        } catch (Exception e) {
            System.err.println("Error formatting result: " + e.getMessage());
            formatted.put("error", "Error formatting result");
        }

        return formatted;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-RapidAPI-Key", RAPIDAPI_KEY);
        headers.set("X-RapidAPI-Host", RAPIDAPI_HOST);
        return headers;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("status", "Error");
        return error;
    }

    public List<String> getSupportedLanguages() {
        return new ArrayList<>(LANGUAGE_IDS.keySet());
    }
}