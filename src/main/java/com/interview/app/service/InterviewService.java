package com.interview.app.service;

import com.interview.app.dto.InterviewMessage;
import com.interview.app.dto.MediaChunk;
import com.interview.app.websocket.GeminiWebSocketClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {
    
    private final GeminiWebSocketClient geminiClient;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, InterviewSession> activeSessions = new ConcurrentHashMap<>();
    
    public String startInterview(String userId) {
        String sessionId = UUID.randomUUID().toString();
        InterviewSession session = new InterviewSession(sessionId, userId);
        activeSessions.put(sessionId, session);
        
        // Setup Gemini session handler
        geminiClient.setupSession(sessionId, new GeminiWebSocketClient.SessionHandler() {
            @Override
            public void onSetupComplete() {
                log.info("Gemini setup complete for session: {}", sessionId);
                sendMessageToClient(sessionId, "SETUP_COMPLETE", null, null);
            }
            
            @Override
            public void onAudioData(String mimeType, String base64Data) {
                MediaChunk audioChunk = new MediaChunk(mimeType, base64Data);
                sendMessageToClient(sessionId, "AUDIO_RESPONSE", null, List.of(audioChunk));
            }
            
            @Override
            public void onTextResponse(String text) {
                sendMessageToClient(sessionId, "TEXT_RESPONSE", text, null);
            }
            
            @Override
            public void onTurnComplete() {
                sendMessageToClient(sessionId, "TURN_COMPLETE", null, null);
            }
            
            @Override
            public void onInterrupted() {
                log.info("Gemini interrupted for session: {}", sessionId);
                sendMessageToClient(sessionId, "INTERRUPTED", null, null);
            }
            
            @Override
            public void onDisconnect() {
                log.warn("Gemini disconnected for session: {}", sessionId);
                sendMessageToClient(sessionId, "DISCONNECTED", null, null);
            }
        });
        
        log.info("Started interview session: {} for user: {}", sessionId, userId);
        return sessionId;
    }
    
    public void endInterview(String sessionId) {
        InterviewSession session = activeSessions.remove(sessionId);
        if (session != null) {
            // Remove session from Gemini client - this will close connection if no other sessions
            geminiClient.removeSession(sessionId);
            log.info("Ended interview session: {}", sessionId);
        }
    }
    
    public void processMessage(InterviewMessage message) {
        String sessionId = message.getSessionId();
        InterviewSession session = activeSessions.get(sessionId);
        
        if (session == null) {
            log.error("No active session found for: {}", sessionId);
            return;
        }
        
        switch (message.getType()) {
            case TEXT:
                geminiClient.sendTextMessage(sessionId, message.getContent());
                break;
            case AUDIO:
            case VIDEO:
            case SCREEN_SHARE:
            case MIXED:
                if (message.getMediaChunks() != null && !message.getMediaChunks().isEmpty()) {
                    geminiClient.sendRealtimeInput(sessionId, message.getMediaChunks());
                }
                break;
            case CONTROL:
                handleControlMessage(sessionId, message);
                break;
        }
    }
    
    private void handleControlMessage(String sessionId, InterviewMessage message) {
        // Handle control messages like pause, resume, etc.
        log.debug("Handling control message for session: {}", sessionId);
    }
    
    private void sendMessageToClient(String sessionId, String type, String content, List<MediaChunk> mediaChunks) {
        InterviewMessage response = new InterviewMessage();
        response.setSessionId(sessionId);
        response.setType(InterviewMessage.MessageType.valueOf(type.contains("AUDIO") ? "AUDIO" : "TEXT"));
        response.setContent(content);
        response.setMediaChunks(mediaChunks);
        response.setTimestamp(System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/interview/" + sessionId, response);
    }
    
    public void sendMediaToGemini(String sessionId, List<MediaChunk> mediaChunks) {
        // Direct method for sending media to Gemini, bypassing STOMP
        geminiClient.sendRealtimeInput(sessionId, mediaChunks);
    }
    
    private static class InterviewSession {
        private final String sessionId;
        private final String userId;
        private final long startTime;
        
        public InterviewSession(String sessionId, String userId) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.startTime = System.currentTimeMillis();
        }
    }
}
