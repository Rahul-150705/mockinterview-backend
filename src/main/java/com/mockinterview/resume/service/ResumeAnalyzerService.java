package com.mockinterview.resume.service;

import com.mockinterview.config.OpenAIConfig;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ResumeAnalyzerService {

    private final OpenAIConfig openAIConfig;
    private final ResumeParserService resumeParserService;
    private final RestTemplate restTemplate = new RestTemplate();
    
    // In-memory storage for analysis history (consider using database in production)
    private final Map<String, List<Map<String, Object>>> analysisHistory = new HashMap<>();

    public Map<String, Object> analyzeResume(User user, MultipartFile file, String jobDescription) throws IOException {
        // Extract resume text
        String resumeText = resumeParserService.extractTextFromResume(file);
        
        if (resumeText == null || resumeText.trim().isEmpty()) {
            throw new RuntimeException("Could not extract text from resume");
        }
        
        // Analyze resume with AI
        Map<String, Object> analysis = performAIAnalysis(resumeText, jobDescription);
        
        // Add metadata
        analysis.put("fileName", file.getOriginalFilename());
        analysis.put("analyzedAt", LocalDateTime.now().toString());
        analysis.put("fileSize", file.getSize());
        
        // Store in history
        String userKey = user.getId().toString();
        analysisHistory.computeIfAbsent(userKey, k -> new ArrayList<>()).add(analysis);
        
        return analysis;
    }

    private Map<String, Object> performAIAnalysis(String resumeText, String jobDescription) {
        try {
            String apiKey = openAIConfig.getApiKey();
            
            if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("sk-your")) {
                System.out.println("OpenAI API key not configured, using placeholder analysis");
                return getPlaceholderAnalysis(resumeText);
            }

            // Build AI prompt
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("Analyze this resume and provide detailed feedback:\n\n");
            promptBuilder.append("RESUME:\n").append(resumeText).append("\n\n");
            
            if (jobDescription != null && !jobDescription.trim().isEmpty()) {
                promptBuilder.append("JOB DESCRIPTION:\n").append(jobDescription).append("\n\n");
                promptBuilder.append("Provide analysis considering the job requirements.\n\n");
            }
            
            promptBuilder.append("Please provide:\n");
            promptBuilder.append("1. Overall Score (0-100)\n");
            promptBuilder.append("2. Strengths (list 3-5 key strengths)\n");
            promptBuilder.append("3. Areas for Improvement (list 3-5 areas)\n");
            promptBuilder.append("4. Skills Match (if job description provided, rate 0-100)\n");
            promptBuilder.append("5. Recommendations (3-5 specific suggestions)\n");
            promptBuilder.append("6. ATS Compatibility Score (0-100)\n");
            promptBuilder.append("7. Missing Keywords (list important keywords not found)\n\n");
            promptBuilder.append("Format your response as JSON with keys: overallScore, strengths, improvements, skillsMatch, recommendations, atsScore, missingKeywords, summary");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "user", "content", promptBuilder.toString())
            ));
            requestBody.put("max_tokens", 1500);
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
                    String content = (String) message.get("content");
                    
                    // Try to parse JSON response
                    try {
                        // Remove markdown code blocks if present
                        content = content.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").trim();
                        return parseAnalysisResponse(content);
                    } catch (Exception e) {
                        System.err.println("Failed to parse AI response: " + e.getMessage());
                        return parseTextualResponse(content);
                    }
                }
            }

            return getPlaceholderAnalysis(resumeText);

        } catch (Exception e) {
            System.err.println("Error calling OpenAI API: " + e.getMessage());
            e.printStackTrace();
            return getPlaceholderAnalysis(resumeText);
        }
    }

    private Map<String, Object> parseAnalysisResponse(String jsonContent) {
        try {
            // Simple JSON parsing (in production, use Jackson or Gson)
            Map<String, Object> result = new HashMap<>();
            
            // Extract values using basic string manipulation
            result.put("overallScore", extractScore(jsonContent, "overallScore"));
            result.put("strengths", extractList(jsonContent, "strengths"));
            result.put("improvements", extractList(jsonContent, "improvements"));
            result.put("skillsMatch", extractScore(jsonContent, "skillsMatch"));
            result.put("recommendations", extractList(jsonContent, "recommendations"));
            result.put("atsScore", extractScore(jsonContent, "atsScore"));
            result.put("missingKeywords", extractList(jsonContent, "missingKeywords"));
            result.put("summary", extractString(jsonContent, "summary"));
            
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse analysis response", e);
        }
    }

    private Map<String, Object> parseTextualResponse(String content) {
        // Fallback parser for non-JSON responses
        Map<String, Object> result = new HashMap<>();
        result.put("overallScore", 75);
        result.put("strengths", List.of("Clear formatting", "Relevant experience", "Strong technical skills"));
        result.put("improvements", List.of("Add quantifiable achievements", "Improve keyword optimization"));
        result.put("skillsMatch", 70);
        result.put("recommendations", List.of("Add metrics to achievements", "Include more keywords"));
        result.put("atsScore", 65);
        result.put("missingKeywords", List.of("Leadership", "Agile", "Cloud"));
        result.put("summary", content.length() > 500 ? content.substring(0, 500) + "..." : content);
        return result;
    }

    private int extractScore(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception e) {
            // Ignore
        }
        return 70; // Default
    }

    private List<String> extractList(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\\[(.*?)\\]";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                String listContent = m.group(1);
                String[] items = listContent.split(",");
                List<String> result = new ArrayList<>();
                for (String item : items) {
                    String cleaned = item.replaceAll("\"", "").trim();
                    if (!cleaned.isEmpty()) {
                        result.add(cleaned);
                    }
                }
                return result;
            }
        } catch (Exception e) {
            // Ignore
        }
        return List.of("Analysis in progress");
    }

    private String extractString(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"(.*?)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            // Ignore
        }
        return "Analysis complete";
    }

    private Map<String, Object> getPlaceholderAnalysis(String resumeText) {
        Map<String, Object> analysis = new HashMap<>();
        
        // Calculate basic metrics
        int wordCount = resumeText.split("\\s+").length;
        boolean hasEmail = resumeText.toLowerCase().contains("@");
        boolean hasPhone = resumeText.matches(".*\\d{3}[-.]?\\d{3}[-.]?\\d{4}.*");
        
        int baseScore = 60;
        if (wordCount > 300) baseScore += 10;
        if (hasEmail) baseScore += 5;
        if (hasPhone) baseScore += 5;
        
        analysis.put("overallScore", Math.min(baseScore, 85));
        analysis.put("strengths", List.of(
            "Clear structure and organization",
            "Professional presentation",
            "Relevant work experience included",
            "Technical skills highlighted"
        ));
        analysis.put("improvements", List.of(
            "Add quantifiable achievements with metrics",
            "Include more industry-specific keywords",
            "Expand on project details and impact",
            "Consider adding a professional summary"
        ));
        analysis.put("skillsMatch", 70);
        analysis.put("recommendations", List.of(
            "Use action verbs to start each bullet point",
            "Include measurable results (e.g., 'increased by 25%')",
            "Tailor content to match job description keywords",
            "Keep formatting consistent throughout",
            "Limit resume to 1-2 pages"
        ));
        analysis.put("atsScore", 65);
        analysis.put("missingKeywords", List.of(
            "Leadership",
            "Agile methodology",
            "Cloud platforms",
            "Team collaboration"
        ));
        analysis.put("summary", "Your resume shows solid professional experience and technical skills. "
            + "To improve, focus on adding quantifiable achievements and optimizing for ATS systems. "
            + "Consider including more specific metrics and industry keywords to increase your chances of passing automated screening.");
        
        return analysis;
    }

    public List<Map<String, Object>> getAnalysisHistory(User user) {
        String userKey = user.getId().toString();
        return analysisHistory.getOrDefault(userKey, new ArrayList<>());
    }
}