package com.mockinterview.interview.service;

import com.mockinterview.ai.service.AIService;
import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Interview;
import com.mockinterview.interview.entity.Question;
import com.mockinterview.interview.repository.InterviewRepository;
import com.mockinterview.interview.repository.QuestionRepository;
import com.mockinterview.interview.repository.AnswerRepository;
import com.mockinterview.resume.entity.Resume;
import com.mockinterview.resume.service.ResumeService;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AIService aiService;
    private final ResumeService resumeService;

    @Transactional
    public Interview startInterview(User user, String jobTitle, String jobDescription, List<String> questionsText) {
        System.out.println("Creating interview for user: " + user.getEmail());
        
        Interview interview = Interview.builder()
                .user(user)
                .jobTitle(jobTitle)
                .jobDescription(jobDescription)
                .startedAt(LocalDateTime.now())
                .build();

        // Save interview first to get ID
        Interview savedInterview = interviewRepository.save(interview);
        System.out.println("Interview saved with ID: " + savedInterview.getId());

        // Create questions - use ArrayList instead of immutable list
        final Interview finalInterview = savedInterview;
        List<Question> questions = new ArrayList<>();
        for (String questionText : questionsText) {
            Question question = Question.builder()
                    .questionText(questionText)
                    .interview(finalInterview)
                    .build();
            questions.add(question);
        }

        finalInterview.setQuestions(questions);
        
        // Save again with questions
        Interview result = interviewRepository.save(finalInterview);
        System.out.println("Interview saved with " + questions.size() + " questions");

        return result;
    }
    
    @Transactional
    public Interview startInterviewWithResume(User user, String jobTitle, String jobDescription) {
        System.out.println("Creating interview for user: " + user.getEmail());
        
        // Get user's latest resume
        Resume resume = resumeService.getLatestResume(user);
        String resumeText = null;
        
        if (resume != null && resume.getResumeText() != null) {
            resumeText = resume.getResumeText();
            System.out.println("Using resume: " + resume.getFileName());
            System.out.println("Resume text length: " + resumeText.length() + " characters");
        } else {
            System.err.println("No resume found or resume has no text, generating generic questions");
        }
        
        // Generate questions with resume context (with fallback)
        List<String> questions;
        try {
            String[] generatedQuestions = aiService.generateQuestions(jobTitle, jobDescription, resumeText);
            questions = List.of(generatedQuestions);
        } catch (Exception e) {
            System.err.println("OpenAI API failed: " + e.getMessage());
            System.out.println("Using fallback questions for job: " + jobTitle);
            questions = getFallbackQuestions(jobTitle);
        }
        
        return startInterview(user, jobTitle, jobDescription, questions);
    }

    @Transactional
    public Answer submitAnswer(Long questionId, String userAnswer) {
        System.out.println("Submitting answer for question ID: " + questionId);
        
        // Find the question directly
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found with ID: " + questionId));
        
        System.out.println("Found question: " + question.getQuestionText());

        // Call AI service for feedback
        String feedback = aiService.getFeedback(question.getQuestionText(), userAnswer);
        double score = aiService.getScore(feedback);
        
        System.out.println("Generated feedback with score: " + score);

        // Create answer
        Answer answer = Answer.builder()
                .question(question)
                .userAnswer(userAnswer)
                .aiFeedback(feedback)
                .score((int) score)
                .build();

        // Save answer
        answer = answerRepository.save(answer);
        System.out.println("Answer saved with ID: " + answer.getId());

        return answer;
    }

    public List<Interview> getUserInterviews(User user) {
        System.out.println("Getting interviews for user: " + user.getEmail());
        List<Interview> interviews = interviewRepository.findByUser(user);
        System.out.println("Found " + interviews.size() + " interviews");
        return interviews;
    }
    
    // Fallback questions when OpenAI API fails
    private List<String> getFallbackQuestions(String jobTitle) {
        List<String> fallbackQuestions = new ArrayList<>();
        
        String jobLower = jobTitle.toLowerCase();
        
        if (jobLower.contains("sde") || jobLower.contains("software") || 
            jobLower.contains("developer") || jobLower.contains("engineer")) {
            
            fallbackQuestions.add("Tell me about yourself and your experience in software development.");
            fallbackQuestions.add("What programming languages are you most comfortable with and why?");
            fallbackQuestions.add("Can you explain the difference between an array and a linked list?");
            fallbackQuestions.add("Describe a challenging bug you encountered and how you solved it.");
            fallbackQuestions.add("What is your experience with version control systems like Git?");
            fallbackQuestions.add("How do you approach debugging a complex issue in production?");
            fallbackQuestions.add("Explain the concept of Object-Oriented Programming and its principles.");
            fallbackQuestions.add("What testing strategies do you use to ensure code quality?");
            
        } else if (jobLower.contains("data") || jobLower.contains("analyst") || 
                   jobLower.contains("scientist")) {
            
            fallbackQuestions.add("Tell me about your experience with data analysis.");
            fallbackQuestions.add("What tools and technologies have you used for data processing?");
            fallbackQuestions.add("Explain the difference between SQL and NoSQL databases.");
            fallbackQuestions.add("How do you handle missing or inconsistent data in a dataset?");
            fallbackQuestions.add("Describe a data project you're proud of and the impact it had.");
            fallbackQuestions.add("What is your experience with data visualization tools?");
            fallbackQuestions.add("How do you ensure data quality and integrity?");
            fallbackQuestions.add("Explain what a statistical hypothesis test is.");
            
        } else if (jobLower.contains("frontend") || jobLower.contains("front-end") || 
                   jobLower.contains("ui") || jobLower.contains("ux")) {
            
            fallbackQuestions.add("Tell me about your experience with frontend development.");
            fallbackQuestions.add("What JavaScript frameworks are you most comfortable with?");
            fallbackQuestions.add("How do you ensure your websites are responsive and accessible?");
            fallbackQuestions.add("Explain the difference between CSS Flexbox and Grid.");
            fallbackQuestions.add("How do you optimize frontend performance?");
            fallbackQuestions.add("What is your approach to cross-browser compatibility?");
            fallbackQuestions.add("Describe your experience with state management in React/Vue/Angular.");
            fallbackQuestions.add("How do you handle API integration in frontend applications?");
            
        } else if (jobLower.contains("backend") || jobLower.contains("back-end") || 
                   jobLower.contains("api")) {
            
            fallbackQuestions.add("Tell me about your experience with backend development.");
            fallbackQuestions.add("What backend frameworks and languages are you proficient in?");
            fallbackQuestions.add("How do you design RESTful APIs?");
            fallbackQuestions.add("Explain database normalization and why it matters.");
            fallbackQuestions.add("How do you handle authentication and authorization?");
            fallbackQuestions.add("Describe your experience with microservices architecture.");
            fallbackQuestions.add("How do you ensure API security?");
            fallbackQuestions.add("What strategies do you use for database optimization?");
            
        } else {
            // Generic questions for any role
            fallbackQuestions.add("Tell me about yourself and your professional background.");
            fallbackQuestions.add("What interests you about this position and our company?");
            fallbackQuestions.add("What are your greatest strengths and how do they apply to this role?");
            fallbackQuestions.add("Describe a challenging situation you faced at work and how you handled it.");
            fallbackQuestions.add("How do you prioritize tasks when managing multiple deadlines?");
            fallbackQuestions.add("Tell me about a time you worked in a team to achieve a goal.");
            fallbackQuestions.add("What motivates you in your professional life?");
            fallbackQuestions.add("Where do you see yourself in 5 years?");
        }
        
        return fallbackQuestions;
    }
}