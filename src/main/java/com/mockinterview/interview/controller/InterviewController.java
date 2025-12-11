package com.mockinterview.interview.controller;

import com.mockinterview.ai.service.AIService;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Interview;
import com.mockinterview.interview.repository.InterviewRepository;
import com.mockinterview.interview.service.InterviewService;
import com.mockinterview.user.entity.User;
import com.mockinterview.user.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.mockinterview.pdf.service.PdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InterviewController {
    private final PdfService pdfService;
    private final InterviewService interviewService;
    private final AIService aiService;
    private final UserRepository userRepository;
    private final InterviewRepository interviewRepository;
    
    @PostMapping("/start")
    public ResponseEntity<?> startInterview(
            @RequestBody StartInterviewRequest request,
            @RequestParam(value = "userId", required = false) Long userId) {

        try {
            System.out.println("Starting interview for job: " + request.getJobTitle());
            System.out.println("Interview round: " + request.getRoundType());
            
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

            String[] questions = aiService.generateQuestions(
                request.getJobTitle(), 
                request.getJobDescription(),
                request.getRoundType()
            );

            Interview interview = interviewService.startInterview(
                    user,
                    request.getJobTitle(),
                    request.getJobDescription(),
                    List.of(questions)
            );

            // Return simplified response without circular references
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Interview started successfully");
            response.put("id", interview.getId());
            response.put("jobTitle", interview.getJobTitle());
            response.put("startedAt", interview.getStartedAt().toString());
            
            // Simplify questions
            List<Map<String, Object>> questionList = interview.getQuestions().stream()
                .map(q -> {
                    Map<String, Object> qMap = new HashMap<>();
                    qMap.put("id", q.getId());
                    qMap.put("questionText", q.getQuestionText());
                    return qMap;
                })
                .toList();
            
            response.put("questions", questionList);
            
            System.out.println("Interview created with " + questions.length + " questions");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/{questionId}/answer")
    public ResponseEntity<?> submitAnswer(
            @PathVariable Long questionId,
            @RequestBody AnswerRequest request) {

        try {
            System.out.println("Submitting answer for question: " + questionId);
            
            Answer answer = interviewService.submitAnswer(questionId, request.getAnswer());
            
            // Return simplified response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Answer submitted successfully");
            response.put("id", answer.getId());
            response.put("userAnswer", answer.getUserAnswer());
            response.put("aiFeedback", answer.getAiFeedback());
            response.put("score", answer.getScore());
            
            System.out.println("Answer submitted with score: " + answer.getScore());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{interviewId}/download-pdf")
    public ResponseEntity<?> downloadInterviewPdf(
            @PathVariable Long interviewId,
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            System.out.println("Generating PDF for interview: " + interviewId);
            
            // Get user - for development, use first user if not specified
            User user;
            if (userId != null) {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            } else {
                List<User> users = userRepository.findAll();
                if (users.isEmpty()) {
                    throw new RuntimeException("No users in database.");
                }
                user = users.get(0);
            }
            
            // Get interview and verify ownership
            Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("Interview not found with ID: " + interviewId));
            
            if (!interview.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Unauthorized: This interview belongs to another user");
            }
            
            // Generate PDF
            byte[] pdfBytes = pdfService.generateInterviewPdf(interview);
            
            // Generate filename
            String filename = "Interview_" + interview.getJobTitle().replaceAll("[^a-zA-Z0-9]", "_") 
                            + "_" + interview.getId() + ".pdf";
            
            System.out.println("PDF generated successfully: " + filename);
            
            // Return PDF as downloadable file
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(pdfBytes);
                    
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getUserInterviews(
            @RequestParam(value = "userId", required = false) Long userId) {
        
        try {
            // Get user - for development, use first user if not specified
            User user;
            if (userId != null) {
                user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            } else {
                List<User> users = userRepository.findAll();
                if (users.isEmpty()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("interviews", List.of());
                    response.put("count", 0);
                    return ResponseEntity.ok(response);
                }
                user = users.get(0);
            }
            
            List<Interview> interviews = interviewService.getUserInterviews(user);
            
            // Simplify interviews
            List<Map<String, Object>> interviewList = interviews.stream()
                .map(i -> {
                    Map<String, Object> iMap = new HashMap<>();
                    iMap.put("id", i.getId());
                    iMap.put("jobTitle", i.getJobTitle());
                    iMap.put("jobDescription", i.getJobDescription());
                    iMap.put("startedAt", i.getStartedAt().toString());
                    if (i.getFinishedAt() != null) {
                        iMap.put("finishedAt", i.getFinishedAt().toString());
                    }
                    iMap.put("questionCount", i.getQuestions() != null ? i.getQuestions().size() : 0);
                    return iMap;
                })
                .toList();
            
            Map<String, Object> response = new HashMap<>();
            response.put("interviews", interviewList);
            response.put("count", interviewList.size());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("interviews", List.of());
            errorResponse.put("count", 0);
            return ResponseEntity.ok(errorResponse);
        }
    }
}

@Data
class StartInterviewRequest {
    private String jobTitle;
    private String jobDescription;
    private String roundType; // BEHAVIORAL, CODING, DSA, SYSTEM_DESIGN
}

@Data
class AnswerRequest {
    private String answer;
}