package com.mockinterview.interview.repository;

import com.mockinterview.interview.entity.Interview;
import com.mockinterview.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUser(User user);
}
