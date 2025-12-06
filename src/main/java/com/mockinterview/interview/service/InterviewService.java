package com.mockinterview.interview.service;

import com.mockinterview.ai.service.AIService;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Interview;
import com.mockinterview.interview.entity.Question;
import com.mockinterview.interview.repository.InterviewRepository;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final AIService aiService;

    public Interview startInterview(User user, String jobTitle, String jobDescription, List<String> questionsText) {
        Interview interview = Interview.builder()
                .user(user)
                .jobTitle(jobTitle)
                .jobDescription(jobDescription)
                .startedAt(LocalDateTime.now())
                .build();

        List<Question> questions = questionsText.stream().map(q -> Question.builder()
                .questionText(q)
                .interview(interview)
                .build()).toList();

        interview.setQuestions(questions);

        return interviewRepository.save(interview);
    }

    public Answer submitAnswer(Long questionId, String userAnswer) {
        // Fetch question from DB
        Question question = interviewRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"))
                .getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Question not found"));

        // Call AI service for feedback
        String feedback = aiService.getFeedback(question.getQuestionText(), userAnswer);
        double score = aiService.getScore(feedback);

        Answer answer = Answer.builder()
                .question(question)
                .userAnswer(userAnswer)
                .aiFeedback(feedback)
                .score(score)
                .build();

        question.getAnswers().add(answer);

        interviewRepository.save(question.getInterview());

        return answer;
    }

    public List<Interview> getUserInterviews(User user) {
        return interviewRepository.findByUser(user);
    }
}
