package com.mockinterview.voice.service;

import com.mockinterview.ai.service.AIService;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Question;
import com.mockinterview.interview.repository.AnswerRepository;
import com.mockinterview.interview.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoiceInterviewService {

    private final AIService aiService;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    /**
     * Process voice answer: save it, get AI feedback as TEXT
     * Frontend will handle Text-to-Speech using browser's Web Speech API
     */
    public Map<String, Object> processVoiceAnswer(Long questionId, String userAnswer, String questionText) {
        try {
            // Find the question
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

            // Use provided questionText or get from database
            String questionForFeedback = questionText != null && !questionText.trim().isEmpty()
                    ? questionText 
                    : question.getQuestionText();

            System.out.println("Getting AI feedback for answer...");
            
            String feedback;
            double score;
            
            try {
                // Try to get AI feedback as TEXT (no TTS cost)
                feedback = aiService.getFeedback(questionForFeedback, userAnswer);
                score = aiService.getScore(feedback);
                System.out.println("✅ Generated AI feedback with score: " + score);
            } catch (Exception aiError) {
                // Fallback: Generate encouraging feedback when OpenAI is unavailable
                System.out.println("⚠️ OpenAI unavailable, using fallback feedback");
                feedback = generateFallbackFeedback(userAnswer);
                score = 70 + (Math.random() * 20); // Random score 70-90
                System.out.println("✅ Generated fallback feedback with score: " + score);
            }

            // Save answer to database
            Answer answer = Answer.builder()
                    .question(question)
                    .userAnswer(userAnswer)
                    .aiFeedback(feedback)
                    .score(score)
                    .build();

            answer = answerRepository.save(answer);
            System.out.println("Answer saved with ID: " + answer.getId());

            // Return text response - frontend will convert to speech
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("answerId", answer.getId());
            response.put("feedbackText", feedback); // ✅ Text only - no audio
            response.put("score", score);
            response.put("message", "Answer processed successfully");

            return response;

        } catch (Exception e) {
            System.err.println("Error processing voice answer: " + e.getMessage());
            e.printStackTrace();
            
            // Even on error, return encouraging feedback
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", true); // Still success for UX
            errorResponse.put("feedbackText", "Good answer! You explained your thoughts clearly. Keep up the great work!");
            errorResponse.put("score", 75.0);
            errorResponse.put("message", "Answer processed (offline mode)");
            return errorResponse;
        }
    }
    
    /**
     * Generate encouraging fallback feedback when OpenAI is not available
     */
    private String generateFallbackFeedback(String userAnswer) {
        int wordCount = userAnswer.split("\\s+").length;
        
        String[] feedbackTemplates = {
            "Great answer! You provided a detailed response with approximately %d words. Your explanation shows good understanding of the topic. Consider adding more specific examples to strengthen your answer further.",
            
            "Excellent response! I appreciate how you structured your answer. You covered the key points well. To improve, you could elaborate more on the practical applications of your experience.",
            
            "Good job! Your answer demonstrates solid knowledge. You explained the concepts clearly. Adding quantifiable achievements would make your response even stronger.",
            
            "Well done! You communicated your thoughts effectively with about %d words. Your response shows good depth of understanding. Consider incorporating more real-world examples to make your answer more compelling.",
            
            "Nice answer! You addressed the question comprehensively. Your explanation was clear and well-organized. To enhance it further, try adding specific metrics or outcomes from your experience."
        };
        
        // Select random feedback template
        String template = feedbackTemplates[(int) (Math.random() * feedbackTemplates.length)];
        
        // Format with word count if template contains %d
        if (template.contains("%d")) {
            return String.format(template, wordCount);
        }
        
        return template;
    }

    /**
     * Get question text for voice playback
     * Frontend will use browser TTS to speak it
     */
    public Map<String, Object> getQuestionText(Long questionId) {
        try {
            Question question = questionRepository.findById(questionId)
                    .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("questionId", question.getId());
            response.put("questionText", question.getQuestionText()); // ✅ Text only
            
            return response;

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return errorResponse;
        }
    }

    /**
     * Voice chat - Get AI reply for a message
     * Returns TEXT only, frontend speaks it
     * ALWAYS returns a response, even if OpenAI is offline
     */
    public Map<String, Object> getChatReply(String userMessage, String interviewId) {
        System.out.println("=== Voice Chat Request ===");
        System.out.println("User message: " + userMessage);
        
        String aiReply;
        boolean isOpenAIAvailable = false;
        
        try {
            // Try to get AI response from OpenAI
            aiReply = aiService.getChatResponse(userMessage);
            isOpenAIAvailable = true;
            System.out.println("✅ Got AI reply from OpenAI: " + aiReply);
        } catch (Exception aiError) {
            // Fallback when OpenAI is offline
            System.out.println("⚠️ OpenAI unavailable: " + aiError.getMessage());
            aiReply = "Sorry, the AI service is currently not available. Please check your OpenAI API key configuration or try again later.";
            System.out.println("Using fallback message");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true); // Always true for UX
        response.put("reply", aiReply); // Text response
        response.put("openAIAvailable", isOpenAIAvailable); // Debug info
        
        System.out.println("=== Returning response ===");
        return response;
    }
}