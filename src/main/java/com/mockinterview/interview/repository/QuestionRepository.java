package com.mockinterview.interview.repository;

import com.mockinterview.interview.entity.Question;
import com.mockinterview.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    // Spring auto-provides: findById, save, findAll, delete, etc.
    
    // Custom method - Spring generates query automatically from method name
    List<Question> findByInterview(Interview interview);
    
    // Another example - find by question text containing keyword
    List<Question> findByQuestionTextContaining(String keyword);
}