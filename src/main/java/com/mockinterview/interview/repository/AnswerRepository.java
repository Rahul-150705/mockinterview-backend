package com.mockinterview.interview.repository;

import com.mockinterview.interview.entity.Answer;
import com.mockinterview.interview.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    // Spring auto-provides: findById, save, findAll, delete, etc.
    
    // Custom method - find all answers for a specific question
    List<Answer> findByQuestion(Question question);
    
    // Another example - find answers with score above threshold
    List<Answer> findByScoreGreaterThan(Double score);
}