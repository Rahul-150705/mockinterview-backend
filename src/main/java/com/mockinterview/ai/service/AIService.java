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

    public String[] generateQuestions(String jobTitle, String jobDescription, String roundType) {
        try {
            String apiKey = openAIConfig.getApiKey();
            
            // Check if API key is configured
            if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-your")) {
                System.out.println("OpenAI API key not configured, using placeholder questions");
                return getPlaceholderQuestionsByRound(jobTitle, roundType);
            }

            // Build prompt based on round type
            String prompt = buildPromptForRound(jobTitle, jobDescription, roundType);

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
            return getPlaceholderQuestionsByRound(jobTitle, roundType);

        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
            return getPlaceholderQuestionsByRound(jobTitle, roundType);
        }
    }
    
    // Overload for backward compatibility
    public String[] generateQuestions(String jobTitle, String jobDescription) {
        return generateQuestions(jobTitle, jobDescription, "GENERAL");
    }

    private String buildPromptForRound(String jobTitle, String jobDescription, String roundType) {
        StringBuilder promptBuilder = new StringBuilder();
        
        switch (roundType != null ? roundType.toUpperCase() : "GENERAL") {
            case "BEHAVIORAL":
                promptBuilder.append(String.format(
                    "Generate 5 behavioral interview questions for a %s position. " +
                    "Focus on: leadership, teamwork, conflict resolution, problem-solving, and past experiences. " +
                    "Job Description: %s\n\n" +
                    "Return only the questions, one per line, numbered 1-5.",
                    jobTitle, jobDescription
                ));
                break;
                
            case "CODING":
                promptBuilder.append(String.format(
                    "Generate 5 coding interview questions for a %s position. " +
                    "Include practical coding problems, algorithm implementation, and code review scenarios. " +
                    "Job Description: %s\n\n" +
                    "Return only the questions, one per line, numbered 1-5.",
                    jobTitle, jobDescription
                ));
                break;
                
            case "DSA":
                promptBuilder.append(String.format(
                    "Generate 5 Data Structures and Algorithms questions for a %s position. " +
                    "Focus on: arrays, linked lists, trees, graphs, sorting, searching, and complexity analysis. " +
                    "Job Description: %s\n\n" +
                    "Return only the questions, one per line, numbered 1-5.",
                    jobTitle, jobDescription
                ));
                break;
                
            case "SYSTEM_DESIGN":
                promptBuilder.append(String.format(
                    "Generate 5 system design interview questions for a %s position. " +
                    "Focus on: scalability, architecture, databases, microservices, and distributed systems. " +
                    "Job Description: %s\n\n" +
                    "Return only the questions, one per line, numbered 1-5.",
                    jobTitle, jobDescription
                ));
                break;
                
            default:
                promptBuilder.append(String.format(
                    "Generate 5 technical interview questions for a %s position. " +
                    "Job Description: %s\n\n" +
                    "Return only the questions, one per line, numbered 1-5.",
                    jobTitle, jobDescription
                ));
        }
        
        return promptBuilder.toString();
    }

    private String[] getPlaceholderQuestionsByRound(String jobTitle, String roundType) {
        if (roundType == null) roundType = "GENERAL";
        
        switch (roundType.toUpperCase()) {
            case "BEHAVIORAL":
                return new String[]{
                    "Tell me about a time when you had to work with a difficult team member. How did you handle it?",
                    "Describe a situation where you had to meet a tight deadline. What was your approach?",
                    "Give me an example of a project where you showed leadership skills.",
                    "Tell me about a time when you failed. What did you learn from it?",
                    "Describe a situation where you had to resolve a conflict within your team."
                };
                
            case "CODING":
                return new String[]{
                    "Write a function to reverse a string without using built-in reverse methods.",
                    "Implement a function to check if a string is a palindrome.",
                    "Write code to find the first non-repeating character in a string.",
                    "Implement a function to merge two sorted arrays.",
                    "Write a function to detect if a linked list has a cycle."
                };
                
            case "DSA":
                return new String[]{
                    "Explain the difference between a stack and a queue. When would you use each?",
                    "Describe how a hash table works and discuss its time complexity for insertion and lookup.",
                    "What is the time complexity of QuickSort? Explain how the algorithm works.",
                    "Implement a binary search algorithm and explain its time complexity.",
                    "Explain how depth-first search (DFS) and breadth-first search (BFS) differ."
                };
                
            case "SYSTEM_DESIGN":
                return new String[]{
                    "Design a URL shortening service like bit.ly. Discuss scalability and data storage.",
                    "How would you design a distributed cache system?",
                    "Design the architecture for a real-time chat application that supports millions of users.",
                    "Explain how you would design a rate limiter for an API.",
                    "Design a notification system that can handle millions of push notifications per day."
                };
                
            default:
                return new String[]{
                    "Tell me about your experience with " + jobTitle + " technologies.",
                    "What is your approach to solving complex problems?",
                    "Describe a challenging project you've worked on.",
                    "How do you stay updated with the latest technologies?",
                    "What are your strengths and weaknesses as a developer?"
                };
        }
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

    private String getPlaceholderFeedback() {
        return "Your answer demonstrates understanding of the topic. " +
               "Consider providing more specific examples to strengthen your response. " +
               "Overall, this is a solid answer that covers the main points.";
    }
}