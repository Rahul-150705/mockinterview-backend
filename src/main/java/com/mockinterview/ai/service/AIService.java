package com.mockinterview.ai.service;

import com.mockinterview.config.OpenAIConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AIService {

    private final OpenAIConfig openAIConfig;

    public String getFeedback(String question, String userAnswer) {
        // TODO: Integrate with OpenAI/Claude API
        return "This is a placeholder AI feedback.";
    }

    public double getScore(String feedback) {
        // TODO: Parse feedback to score (0-100)
        return 80.0;
    }

    public String[] generateQuestions(String jobTitle, String jobDescription) {
        // TODO: Call OpenAI API to generate questions
        return new String[]{"Question 1", "Question 2"};
    }
}
