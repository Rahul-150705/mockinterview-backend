package com.mockinterview.ai.service;

import com.mockinterview.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIService {

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    public String[] generateQuestions(String jobTitle, String jobDescription, String resumeText) {
        try {
            String apiKey = openAIConfig.getApiKey();
            
            // Check if API key is configured
            if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-your")) {
                System.out.println("OpenAI API key not configured, using placeholder questions");
                return getPlaceholderQuestions(jobTitle);
            }

            // Build prompt with resume if available
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append(String.format(
                "Generate 5 technical interview questions for a %s position. " +
                "Job Description: %s\n\n",
                jobTitle, jobDescription
            ));
            
            if (resumeText != null && !resumeText.trim().isEmpty()) {
                // Add resume context (truncate if too long)
                String resumeSummary = resumeText.length() > 2000 
                    ? resumeText.substring(0, 2000) + "..." 
                    : resumeText;
                promptBuilder.append("Candidate's Resume:\n")
                    .append(resumeSummary)
                    .append("\n\n");
                promptBuilder.append("Generate questions that are relevant to both the job description AND the candidate's background.\n\n");
            }
            
            promptBuilder.append("Return only the questions, one per line, numbered 1-5.");
            String prompt = promptBuilder.toString();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                request,
                Map.class
            );

            // Parse response
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    
                    // Split by lines and clean up
                    String[] questions = content.split("\n");
                    return java.util.Arrays.stream(questions)
                        .map(q -> q.replaceAll("^\\d+\\.\\s*", "").trim())
                        .filter(q -> !q.isEmpty())
                        .toArray(String[]::new);
                }
            }

            System.out.println("Failed to parse OpenAI response, using placeholder questions");
            return getPlaceholderQuestions(jobTitle);

        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
            return getPlaceholderQuestions(jobTitle);
        }
    }
    
    // Overload for backward compatibility
    public String[] generateQuestions(String jobTitle, String jobDescription) {
        return generateQuestions(jobTitle, jobDescription, null);
    }

    public String getFeedback(String question, String userAnswer) {
        try {
            String apiKey = openAIConfig.getApiKey();
            
            if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-your")) {
                return getPlaceholderFeedback();
            }

            String prompt = String.format(
                "You are an expert technical interviewer. " +
                "Question: %s\n" +
                "Candidate's Answer: %s\n\n" +
                "Provide constructive feedback on this answer in 2-3 sentences. " +
                "Include what was good and what could be improved.",
                question, userAnswer
            );

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("max_tokens", 200);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                request,
                Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            return getPlaceholderFeedback();

        } catch (Exception e) {
            System.err.println("Error getting AI feedback: " + e.getMessage());
            return getPlaceholderFeedback();
        }
    }

    public double getScore(String feedback) {
        // Simple scoring logic - in production, use AI to generate score
        if (feedback.toLowerCase().contains("excellent") || feedback.toLowerCase().contains("great")) {
            return 90.0 + (Math.random() * 10); // 90-100
        } else if (feedback.toLowerCase().contains("good")) {
            return 75.0 + (Math.random() * 15); // 75-90
        } else if (feedback.toLowerCase().contains("needs improvement")) {
            return 50.0 + (Math.random() * 25); // 50-75
        }
        return 70.0 + (Math.random() * 20); // 70-90 default
    }

    private String[] getPlaceholderQuestions(String jobTitle) {
        return new String[]{
            "Tell me about your experience with " + jobTitle + " technologies.",
            "What is your approach to solving complex problems?",
            "Describe a challenging project you've worked on.",
            "How do you stay updated with the latest technologies?",
            "What are your strengths and weaknesses as a developer?"
        };
    }

    private String getPlaceholderFeedback() {
        return "Your answer demonstrates understanding of the topic. " +
               "Consider providing more specific examples to strengthen your response. " +
               "Overall, this is a solid answer that covers the main points.";
    }
}