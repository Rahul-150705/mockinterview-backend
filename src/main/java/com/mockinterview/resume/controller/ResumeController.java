package com.mockinterview.resume.controller;

import com.mockinterview.resume.entity.Resume;
import com.mockinterview.resume.service.ResumeService;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {

        Resume resume = resumeService.uploadResume(user, file);
        return ResponseEntity.ok(resume);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listUserResumes(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(resumeService.getUserResumes(user));
    }
}
