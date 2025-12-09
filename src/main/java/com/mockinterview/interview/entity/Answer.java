package com.mockinterview.interview.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userAnswer;

    private String aiFeedback;

    private double score;

    @ManyToOne
    @JoinColumn(name = "question_id")
    private Question question;
}
