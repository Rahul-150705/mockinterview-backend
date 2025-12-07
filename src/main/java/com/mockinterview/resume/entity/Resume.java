package com.mockinterview.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mockinterview.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resumes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;

    private String filePath;

    @Column(columnDefinition = "TEXT")
    private String resumeText; // Extracted text content from the resume file

    private LocalDateTime uploadedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore  // Prevents circular reference with User
    private User user;
}