package com.mockinterview.resume.service;

import com.mockinterview.resume.entity.Resume;
import com.mockinterview.resume.repository.ResumeRepository;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;

    private final String uploadDir = "uploads/resumes/";

    public Resume uploadResume(User user, MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String filePath = uploadDir + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        file.transferTo(new File(filePath));

        Resume resume = Resume.builder()
                .user(user)
                .fileName(file.getOriginalFilename())
                .filePath(filePath)
                .uploadedAt(LocalDateTime.now())
                .build();

        return resumeRepository.save(resume);
    }

    public List<Resume> getUserResumes(User user) {
        return resumeRepository.findByUser(user);
    }
}
