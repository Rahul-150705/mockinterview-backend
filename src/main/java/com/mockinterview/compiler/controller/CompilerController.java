package com.mockinterview.compiler.controller;

import com.mockinterview.compiler.service.CodapiService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/compiler")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CompilerController {

    private final CodapiService codapiService;

    @PostMapping("/execute")
public ResponseEntity<?> executeCode(@RequestBody CodeExecutionRequest request) {
    try {
        System.out.println("Executing code in language: " + request.getLanguage());
        System.out.println("Stdin received: [" + request.getStdin() + "]"); // Debug log
        
        if (request.getSourceCode() == null || request.getSourceCode().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Source code is required")
            );
        }

        String stdin = request.getStdin() != null ? request.getStdin() : "";
        
        Map<String, Object> result = codapiService.executeCode(
            request.getSourceCode(),
            request.getLanguage(),
            stdin
        );

        return ResponseEntity.ok(result);

    } catch (Exception e) {
        e.printStackTrace();
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("error", "Execution failed: " + e.getMessage());
        return ResponseEntity.badRequest().body(errorResponse);
    }
}

    @PostMapping("/test")
    public ResponseEntity<?> testCode(@RequestBody CodeTestRequest request) {
        try {
            System.out.println("Testing code with test cases");
            
            if (request.getSourceCode() == null || request.getSourceCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Source code is required")
                );
            }

            if (request.getTestCases() == null || request.getTestCases().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Test cases are required")
                );
            }

            List<Map<String, Object>> results = new java.util.ArrayList<>();
            int passedCount = 0;

            for (TestCase testCase : request.getTestCases()) {
                Map<String, Object> result = codapiService.executeCode(
                    request.getSourceCode(),
                    request.getLanguage(),
                    testCase.getInput()
                );

                boolean passed = false;
                if (result.containsKey("stdout")) {
                    String output = ((String) result.get("stdout")).trim();
                    String expected = testCase.getExpectedOutput().trim();
                    passed = output.equals(expected);
                }

                if (passed) passedCount++;

                Map<String, Object> testResult = new HashMap<>();
                testResult.put("input", testCase.getInput());
                testResult.put("expectedOutput", testCase.getExpectedOutput());
                testResult.put("actualOutput", result.get("stdout"));
                testResult.put("passed", passed);
                testResult.put("status", result.get("status"));
                testResult.put("time", result.get("time"));
                
                if (result.containsKey("stderr")) {
                    testResult.put("stderr", result.get("stderr"));
                }
                
                if (result.containsKey("error")) {
                    testResult.put("error", result.get("error"));
                }
                
                results.add(testResult);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("testResults", results);
            response.put("totalTests", request.getTestCases().size());
            response.put("passedTests", passedCount);
            response.put("success", passedCount == request.getTestCases().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Test execution failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

@Data
class CodeExecutionRequest {
    private String sourceCode;
    private String language;
    private String stdin;
}

@Data
class CodeTestRequest {
    private String sourceCode;
    private String language;
    private List<TestCase> testCases;
}

@Data
class TestCase {
    private String input;
    private String expectedOutput;
}