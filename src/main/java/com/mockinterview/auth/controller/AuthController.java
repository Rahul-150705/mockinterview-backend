package com.mockinterview.auth.controller;

import com.mockinterview.auth.dto.LoginRequest;
import com.mockinterview.auth.dto.RegisterRequest;
import com.mockinterview.auth.service.AuthService;
import com.mockinterview.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String token = authService.register(request);
        
        // Get the user to return their ID
        User user = authService.getUserByEmail(request.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("token", token);
        response.put("email", request.getEmail());
        response.put("userId", user.getId());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String token = authService.login(request);
        
        // Get the user to return their ID
        User user = authService.getUserByEmail(request.getEmail());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User logged in successfully");
        response.put("token", token);
        response.put("email", request.getEmail());
        response.put("userId", user.getId());
        
        return ResponseEntity.ok(response);
    }
}