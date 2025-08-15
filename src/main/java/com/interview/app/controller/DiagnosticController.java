package com.interview.app.controller;

import com.interview.app.websocket.GeminiWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/interview/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {
    
    private final GeminiWebSocketClient geminiClient;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.websocket-url}")
    private String websocketUrl;
    
    @GetMapping("/test-connection")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("websocketUrl", websocketUrl);
        result.put("apiKeyPresent", apiKey != null && !apiKey.isEmpty());
        result.put("apiKeyLength", apiKey != null ? apiKey.length() : 0);
        result.put("websocketState", geminiClient.getReadyState().toString());
        result.put("isOpen", geminiClient.isOpen());
        result.put("isClosing", geminiClient.isClosing());
        result.put("isClosed", geminiClient.isClosed());
        
        // Try to connect if not connected
        if (!geminiClient.isOpen()) {
            try {
                log.info("Attempting to connect to Gemini...");
                boolean connected = geminiClient.connectBlocking();
                result.put("connectionAttempt", connected);
                result.put("newState", geminiClient.getReadyState().toString());
            } catch (Exception e) {
                result.put("connectionError", e.getMessage());
                log.error("Connection error", e);
            }
        }
        
        return result;
    }
    
    @PostMapping("/test-setup")
    public Map<String, Object> testSetup() {
        Map<String, Object> result = new HashMap<>();
        String testSessionId = "test-" + System.currentTimeMillis();
        
        result.put("sessionId", testSessionId);
        result.put("websocketState", geminiClient.getReadyState().toString());
        
        final Map<String, Object> responses = new HashMap<>();
        
        geminiClient.setupSession(testSessionId, new GeminiWebSocketClient.SessionHandler() {
            @Override
            public void onSetupComplete() {
                responses.put("setupComplete", true);
                log.info("Test setup complete!");
            }
            
            @Override
            public void onAudioData(String mimeType, String base64Data) {
                responses.put("audioReceived", true);
                responses.put("audioMimeType", mimeType);
            }
            
            @Override
            public void onTextResponse(String text) {
                responses.put("textReceived", true);
                responses.put("textContent", text);
            }
            
            @Override
            public void onTurnComplete() {
                responses.put("turnComplete", true);
            }
            
            @Override
            public void onInterrupted() {
                responses.put("interrupted", true);
            }
            
            @Override
            public void onDisconnect() {
                responses.put("disconnected", true);
                log.warn("Test session disconnected");
            }
        });
        
        // Wait a bit for response
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        result.put("responses", responses);
        
        // Clean up
        geminiClient.removeSession(testSessionId);
        
        return result;
    }
}
