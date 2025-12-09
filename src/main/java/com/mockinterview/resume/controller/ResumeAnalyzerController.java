package com.mockinterview.resume.controller;

import com.mockinterview.resume.service.ResumeAnalyzerService;
import com.mockinterview.user.entity.User;
import com.mockinterview.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume-analyzer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeAnalyzerController {

    private final ResumeAnalyzerService analyzerService;
    private final UserRepository userRepository;

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobDescription", required = false) String jobDescription,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            System.out.println("Analyze request received");
            System.out.println("File: " + (file != null ? file.getOriginalFilename() : "null"));
            System.out.println("Job Description length: " + (jobDescription != null ? jobDescription.length() : 0));
            
            if (file == null || file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "No file provided or file is empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // Get user - for development, use first user if not specified
            User user;
            if (userId != null) {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            } else {
                List<User> users = userRepository.findAll();
                if (users.isEmpty()) {
                    throw new RuntimeException("No users in database. Please register first.");
                }
                user = users.get(0);
                System.out.println("Using first user: " + user.getEmail());
            }

            Map<String, Object> analysis = analyzerService.analyzeResume(user, file, jobDescription);
            
            System.out.println("Analysis completed successfully");
            
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getAnalysisHistory(
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            // Get user - for development, use first user if not specified
            User user;
            if (userId != null) {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            } else {
                List<User> users = userRepository.findAll();
                if (users.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("analyses", List.of());
                    response.put("count", 0);
                    return ResponseEntity.ok(response);
                }
                user = users.get(0);
            }
            
            List<Map<String, Object>> history = analyzerService.getAnalysisHistory(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("analyses", history);
            response.put("count", history.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("analyses", List.of());
            errorResponse.put("count", 0);
            return ResponseEntity.ok(errorResponse);
        }
    }
}