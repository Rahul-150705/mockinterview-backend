package com.mockinterview.resume.service;

import com.mockinterview.resume.entity.Resume;
import com.mockinterview.resume.repository.ResumeRepository;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeParserService resumeParserService;

    public Resume uploadResume(User user, MultipartFile file) throws IOException {
        // Use user's home directory or project directory
        String userHome = System.getProperty("user.home");
        String uploadDir = userHome + File.separator + "mockinterview-uploads" + File.separator + "resumes";
        
        // Create directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("Created upload directory: " + uploadPath.toAbsolutePath());
        }

        // Generate unique filename
        String timestamp = String.valueOf(System.currentTimeMillis());
        String originalFilename = file.getOriginalFilename();
        String filename = timestamp + "_" + originalFilename;
        
        // Full file path
        Path filePath = uploadPath.resolve(filename);
        
        // Save file
        file.transferTo(filePath.toFile());
        
        System.out.println("File saved to: " + filePath.toAbsolutePath());

        // Extract text from resume
        String resumeText = null;
        try {
            resumeText = resumeParserService.extractTextFromResume(file);
            System.out.println("Successfully extracted resume text: " + resumeText.substring(0, Math.min(100, resumeText.length())) + "...");
        } catch (Exception e) {
            System.err.println("Failed to extract text from resume: " + e.getMessage());
            // Continue anyway - save without text
        }

        // Save to database
        Resume resume = Resume.builder()
                .user(user)
                .fileName(originalFilename)
                .filePath(filePath.toString())
                .resumeText(resumeText) // Store extracted text
                .uploadedAt(LocalDateTime.now())
                .build();

        return resumeRepository.save(resume);
    }

    public List<Resume> getUserResumes(User user) {
        return resumeRepository.findByUser(user);
    }
    
    public Resume getLatestResume(User user) {
        List<Resume> resumes = resumeRepository.findByUserOrderByUploadedAtDesc(user);
        return resumes.isEmpty() ? null : resumes.get(0);
    }
}