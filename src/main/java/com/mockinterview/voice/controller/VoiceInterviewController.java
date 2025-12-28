package com.mockinterview.voice.controller;

import com.mockinterview.voice.service.VoiceInterviewService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/voice-interview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VoiceInterviewController {

    private final VoiceInterviewService voiceInterviewService;

    /**
     * Process voice input and return AI feedback as TEXT
     * Frontend will handle Text-to-Speech using browser's Web Speech API (FREE)
     */
    @PostMapping("/process")
    public ResponseEntity<?> processVoiceInput(@RequestBody VoiceInputRequest request) {
        try {
            System.out.println("=== Voice Interview: Processing Answer ===");
            System.out.println("Question ID: " + request.getQuestionId());
            System.out.println("User answer length: " + (request.getUserAnswer() != null ? request.getUserAnswer().length() : 0));
            
            if (request.getUserAnswer() == null || request.getUserAnswer().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "User answer is required")
                );
            }

            if (request.getQuestionId() == null) {
                return ResponseEntity.badRequest().body(
                    Map.of("error", "Question ID is required")
                );
            }

            // Process the answer and get AI feedback as TEXT
            Map<String, Object> result = voiceInterviewService.processVoiceAnswer(
                request.getQuestionId(),
                request.getUserAnswer(),
                request.getQuestionText()
            );

            System.out.println("✅ Voice answer processed successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to process voice input: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get question text for voice playback
     * Frontend will use browser's Web Speech API to speak it (FREE)
     */
    @GetMapping("/{questionId}/text")
    public ResponseEntity<?> getQuestionText(@PathVariable Long questionId) {
        try {
            System.out.println("=== Getting question text for voice: " + questionId);
            
            Map<String, Object> result = voiceInterviewService.getQuestionText(questionId);
            
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Failed to get question text: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Voice chat - send message and get AI reply as TEXT
     * Frontend will speak it using browser TTS (FREE)
     * ALWAYS returns a response, even on error
     */
    @PostMapping("/chat")
    public ResponseEntity<?> voiceChat(@RequestBody VoiceChatRequest request) {
        System.out.println("=== Voice Chat Endpoint Called ===");
        System.out.println("Message: " + request.getMessage());
        System.out.println("Interview ID: " + request.getInterviewId());
        
        try {
            if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
                System.out.println("❌ Empty message received");
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reply", "I didn't hear anything. Please try again."
                ));
            }

            // Get AI reply as TEXT
            Map<String, Object> result = voiceInterviewService.getChatReply(
                request.getMessage(),
                request.getInterviewId()
            );

            System.out.println("✅ Returning response to frontend");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Exception in voice chat endpoint:");
            e.printStackTrace();
            
            // ALWAYS return a valid response, never throw error
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("success", true);
            fallbackResponse.put("reply", "Sorry, the AI service is currently not available. Please check your OpenAI API key configuration or try again later.");
            
            return ResponseEntity.ok(fallbackResponse);
        }
    }
}

@Data
class VoiceInputRequest {
    private Long questionId;
    private String questionText; // Optional, for context
    private String userAnswer; // Transcribed text from user's speech
}

@Data
class VoiceChatRequest {
    private String message;
    private String interviewId; // Optional
}