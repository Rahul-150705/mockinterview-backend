package com.mockinterview.resume.controller;

import com.mockinterview.resume.entity.Resume;
import com.mockinterview.resume.service.ResumeService;
import com.mockinterview.user.entity.User;
import com.mockinterview.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserRepository userRepository;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            System.out.println("Upload request received");
            System.out.println("File: " + (file != null ? file.getOriginalFilename() : "null"));
            System.out.println("UserId: " + userId);
            System.out.println("File size: " + (file != null ? file.getSize() : 0));
            
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
                // Get first user for testing
                List<User> users = userRepository.findAll();
                if (users.isEmpty()) {
                    throw new RuntimeException("No users in database. Please register first.");
                }
                user = users.get(0);
                System.out.println("Using first user: " + user.getEmail());
            }

            Resume resume = resumeService.uploadResume(user, file);
            
            // Return simplified response without circular references
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Resume uploaded successfully");
            response.put("id", resume.getId());
            response.put("fileName", resume.getFileName());
            response.put("uploadedAt", resume.getUploadedAt().toString());
            
            System.out.println("Upload successful: " + resume.getFileName());
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to upload file: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> listUserResumes(
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
                    throw new RuntimeException("No users in database. Please register first.");
                }
                user = users.get(0);
            }
            
            List<Resume> resumes = resumeService.getUserResumes(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put("resumes", resumes);
            response.put("count", resumes.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("resumes", List.of());
            errorResponse.put("count", 0);
            return ResponseEntity.ok(errorResponse);
        }
    }
}