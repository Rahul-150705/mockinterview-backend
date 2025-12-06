package com.mockinterview.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.mockinterview.exception.CustomExceptions.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<?> handleUserExists(UserAlreadyExistsException ex) {
        return createResponse(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<?> handleInvalidCredentials(InvalidCredentialsException ex) {
        return createResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(ResumeNotFoundException.class)
    public ResponseEntity<?> handleResumeNotFound(ResumeNotFoundException ex) {
        return createResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InterviewNotFoundException.class)
    public ResponseEntity<?> handleInterviewNotFound(InterviewNotFoundException ex) {
        return createResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<?> handleQuestionNotFound(QuestionNotFoundException ex) {
        return createResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    // Fallback for unexpected exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex) {
        ex.printStackTrace();
        return createResponse("Internal server error: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<?> createResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ErrorResponse(message, status.value()));
    }

    record ErrorResponse(String message, int statusCode) {}
}
