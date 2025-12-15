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

    // ✅ RestTemplate with timeout configuration
    private final RestTemplate restTemplate = new RestTemplateBuilder()
            .setConnectTimeout(Duration.ofSeconds(5))  // Connection timeout
            .setReadTimeout(Duration.ofSeconds(15))    // Read timeout (execution time)
            .build();
    
    private static final String CODAPI_URL = "https://api.codapi.org/v1/exec";
    private static final int MAX_RETRIES = 2;

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
        // ✅ Validate inputs early
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            return createErrorResponse("Source code cannot be empty");
        }
        
        String codapiLang = LANGUAGE_MAP.get(language.toLowerCase());
        if (codapiLang == null) {
            return createErrorResponse("Unsupported language: " + language);
        }

        // ✅ Special handling for Java - extract class name
        String fileName = getFileName(language);
        if (language.equalsIgnoreCase("java")) {
            fileName = extractJavaClassName(sourceCode);
        }

        // ✅ Try with retries for better reliability
        Exception lastException = null;
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("Executing code with Codapi (attempt " + (attempt + 1) + ")");
                System.out.println("Language: " + language);
                System.out.println("File name: " + fileName);
                
                // Build request body for Codapi
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("sandbox", codapiLang);
                requestBody.put("command", "run");
                
                // ✅ For Java, ensure proper structure
                Map<String, String> files = new HashMap<>();
                if (language.equalsIgnoreCase("java")) {
                    // Validate Java code has a main method
                    if (!sourceCode.contains("public static void main")) {
                        return createErrorResponse("Java code must contain a 'public static void main' method");
                    }
                    
                    // Ensure the code uses 'public class Main' if no specific class is detected
                    if (!sourceCode.contains("public class") && !sourceCode.contains("class ")) {
                        return createErrorResponse("Java code must contain a class declaration");
                    }
                    
                    // If user didn't specify 'public class Main', modify the code
                    String processedCode = sourceCode;
                    if (!processedCode.contains("public class Main") && !processedCode.matches("(?s).*public\\s+class\\s+\\w+.*")) {
                        // No public class found, wrap in Main
                        processedCode = "public class Main {\n" + sourceCode + "\n}";
                        fileName = "Main.java";
                    }
                    
                    files.put(fileName, processedCode);
                } else {
                    files.put(fileName, sourceCode);
                }
                
                requestBody.put("files", files);
                
                if (stdin != null && !stdin.trim().isEmpty()) {
                    requestBody.put("stdin", stdin);
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("User-Agent", "MockInterview-App");

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

                // ✅ Make API call with configured timeout
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    CODAPI_URL,
                    request,
                    Map.class
                );

                if (response.getStatusCode() == HttpStatus.OK) {
                    return formatCodapiResponse(response.getBody());
                } else {
                    System.err.println("Codapi returned status: " + response.getStatusCode());
                    lastException = new Exception("Execution failed with status: " + response.getStatusCode());
                }

            } catch (org.springframework.web.client.ResourceAccessException e) {
                // ✅ Handle timeout specifically
                System.err.println("Timeout on attempt " + (attempt + 1) + ": " + e.getMessage());
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(1000); // Wait 1 second before retry
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error executing code with Codapi (attempt " + (attempt + 1) + "): " + e.getMessage());
                e.printStackTrace();
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

        // ✅ Return meaningful error after all retries
        String errorMessage = "Execution failed after " + (MAX_RETRIES + 1) + " attempts";
        if (lastException != null) {
            if (lastException instanceof org.springframework.web.client.ResourceAccessException) {
                errorMessage = "Execution timed out. The code took too long to run or the service is unavailable.";
            } else {
                errorMessage += ": " + lastException.getMessage();
            }
        }
        return createErrorResponse(errorMessage);
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
            case "java": return "Main.java"; // Default, will be overridden
            case "cpp": return "main.cpp";
            case "c": return "main.c";
            case "csharp": return "Main.cs";
            case "go": return "main.go";
            case "rust": return "main.rs";
            default: return "main.txt";
        }
    }

    /**
     * ✅ Extract the actual class name from Java code
     * Java requires the filename to match the public class name
     */
    private String extractJavaClassName(String sourceCode) {
        try {
            // Remove comments to avoid false matches
            String cleanCode = sourceCode.replaceAll("//.*|(\"(?:\\\\[^\"]|\\\\\"|.)*?\")|(?s)/\\*.*?\\*/", "$1");
            
            // Look for "public class ClassName"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "public\\s+class\\s+(\\w+)"
            );
            java.util.regex.Matcher matcher = pattern.matcher(cleanCode);
            
            if (matcher.find()) {
                String className = matcher.group(1);
                System.out.println("Extracted public Java class name: " + className);
                return className + ".java";
            }
            
            // Fallback: look for any class declaration (non-public)
            pattern = java.util.regex.Pattern.compile(
                "class\\s+(\\w+)"
            );
            matcher = pattern.matcher(cleanCode);
            
            if (matcher.find()) {
                String className = matcher.group(1);
                System.out.println("Extracted Java class name (no public modifier): " + className);
                return className + ".java";
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting Java class name: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Default fallback
        System.out.println("Could not extract Java class name, using Main.java");
        return "Main.java";
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