package com.mockinterview.exception;

public class CustomExceptions {

    public static class UserAlreadyExistsException extends RuntimeException {
        public UserAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class ResumeNotFoundException extends RuntimeException {
        public ResumeNotFoundException(String message) {
            super(message);
        }
    }

    public static class InterviewNotFoundException extends RuntimeException {
        public InterviewNotFoundException(String message) {
            super(message);
        }
    }

    public static class QuestionNotFoundException extends RuntimeException {
        public QuestionNotFoundException(String message) {
            super(message);
        }
    }
}
