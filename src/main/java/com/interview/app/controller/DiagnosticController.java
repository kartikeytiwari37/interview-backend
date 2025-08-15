package com.interview.app.controller;

import com.interview.app.websocket.GeminiWebSocketClient;
import com.interview.app.websocket.GeminiConnectionPool;
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
    
    private final GeminiConnectionPool connectionPool;
    
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
        result.put("activeConnections", connectionPool.getActiveConnectionCount());
        
        // Create a test connection to verify connectivity
        String testSessionId = "diagnostic-test-" + System.currentTimeMillis();
        try {
            log.info("Creating test connection for diagnostics...");
            GeminiWebSocketClient testClient = connectionPool.getConnection(testSessionId);
            
            result.put("websocketState", testClient.getReadyState().toString());
            result.put("isOpen", testClient.isOpen());
            result.put("isClosing", testClient.isClosing());
            result.put("isClosed", testClient.isClosed());
            
            // Try to connect if not connected
            if (!testClient.isOpen()) {
                try {
                    log.info("Attempting to connect to Gemini...");
                    boolean connected = testClient.connectBlocking();
                    result.put("connectionAttempt", connected);
                    result.put("newState", testClient.getReadyState().toString());
                } catch (Exception e) {
                    result.put("connectionError", e.getMessage());
                    log.error("Connection error", e);
                }
            }
            
            // Clean up test connection
            connectionPool.removeConnection(testSessionId);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("Error creating test connection", e);
        }
        
        return result;
    }
    
    @PostMapping("/test-setup")
    public Map<String, Object> testSetup() {
        Map<String, Object> result = new HashMap<>();
        String testSessionId = "test-" + System.currentTimeMillis();
        
        result.put("sessionId", testSessionId);
        result.put("activeConnections", connectionPool.getActiveConnectionCount());
        
        final Map<String, Object> responses = new HashMap<>();
        
        try {
            // Get dedicated connection for test
            GeminiWebSocketClient testClient = connectionPool.getConnection(testSessionId);
            result.put("websocketState", testClient.getReadyState().toString());
            
            testClient.setupSession(testSessionId, new GeminiWebSocketClient.SessionHandler() {
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
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            log.error("Error during test setup", e);
        } finally {
            // Clean up
            connectionPool.removeConnection(testSessionId);
        }
        
        return result;
    }
}
