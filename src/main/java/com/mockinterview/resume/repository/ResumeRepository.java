package com.mockinterview.resume.repository;

import com.mockinterview.resume.entity.Resume;
import com.mockinterview.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUser(User user);
    List<Resume> findByUserOrderByUploadedAtDesc(User user); // Get resumes ordered by newest first
}