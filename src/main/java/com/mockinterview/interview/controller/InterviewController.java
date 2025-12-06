package com.mockinterview.interview.controller;

import com.mockinterview.ai.service.AIService;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Interview;
import com.mockinterview.interview.service.InterviewService;
import com.mockinterview.user.entity.User;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {

    private final InterviewService interviewService;
    private final AIService aiService;

    @PostMapping("/start")
    public ResponseEntity<?> startInterview(
            @AuthenticationPrincipal User user,
            @RequestBody StartInterviewRequest request) {

        String[] questions = aiService.generateQuestions(request.getJobTitle(), request.getJobDescription());

        Interview interview = interviewService.startInterview(
                user,
                request.getJobTitle(),
                request.getJobDescription(),
                List.of(questions)
        );

        return ResponseEntity.ok(interview);
    }

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<?> submitAnswer(
            @PathVariable Long questionId,
            @RequestBody AnswerRequest request) {

        Answer answer = interviewService.submitAnswer(questionId, request.getAnswer());
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserInterviews(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(interviewService.getUserInterviews(user));
    }
}

@Data
class StartInterviewRequest {
    private String jobTitle;
    private String jobDescription;
}

@Data
class AnswerRequest {
    private String answer;
}
