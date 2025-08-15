package com.interview.app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "Interview Backend");
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("websocketEndpoint", "/ws");
        config.put("supportedMediaTypes", new String[]{"audio/pcm;rate=16000", "image/jpeg"});
        config.put("features", new String[]{"audio", "video", "screen-share", "text"});
        return ResponseEntity.ok(config);
    }
}
