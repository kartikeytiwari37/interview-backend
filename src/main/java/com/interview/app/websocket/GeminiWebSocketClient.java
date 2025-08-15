package com.interview.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.app.dto.MediaChunk;
import com.interview.app.dto.SetupMessage;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class GeminiWebSocketClient extends WebSocketClient {
    
    private final ObjectMapper objectMapper;
    private final Map<String, SessionHandler> sessionHandlers = new ConcurrentHashMap<>();
    private boolean isConnected = false;
    private final String sessionId;
    private final String defaultVoice;
    private final String systemInstruction;
    
    @Value("${gemini.api.model}")
    private String model;
    
    @Value("${interview.config.response-modality}")
    private String responseModality;
    
    // Constructor for Spring Bean (backward compatibility)
    public GeminiWebSocketClient(URI serverUri, ObjectMapper objectMapper) {
        super(serverUri);
        this.objectMapper = objectMapper;
        this.sessionId = null;
        this.defaultVoice = null;
        this.systemInstruction = null;
        
        // Add necessary headers
        this.addHeader("Origin", "http://localhost:8080");
        this.addHeader("User-Agent", "Interview-App/1.0");
    }
    
    // Constructor for dedicated session connections
    public GeminiWebSocketClient(URI serverUri, ObjectMapper objectMapper, String sessionId, 
                                String defaultVoice, String systemInstruction) {
        super(serverUri);
        this.objectMapper = objectMapper;
        this.sessionId = sessionId;
        this.defaultVoice = defaultVoice;
        this.systemInstruction = systemInstruction;
        
        // Add necessary headers
        this.addHeader("Origin", "http://localhost:8080");
        this.addHeader("User-Agent", "Interview-App/1.0");
    }
    
    @Override
    public void onOpen(ServerHandshake handshake) {
        log.info("Connected to Gemini Live API - Status: {}, HTTP Status: {}", 
                 handshake.getHttpStatusMessage(), handshake.getHttpStatus());
        isConnected = true;
        
        // Don't send setup here - it will be sent in setupSession
        // This prevents sending duplicate setup messages
    }
    
    @Override
    public void onMessage(String message) {
        try {
            log.info("Received TEXT message from Gemini: {}", message);
            Map<String, Object> response = objectMapper.readValue(message, Map.class);
            
            // Handle different message types from Gemini
            if (response.containsKey("setupComplete")) {
                handleSetupComplete(response);
            } else if (response.containsKey("serverContent")) {
                handleServerContent(response);
            } else if (response.containsKey("toolCall")) {
                handleToolCall(response);
            } else {
                log.warn("Received unknown message type: {}", response.keySet());
            }
        } catch (Exception e) {
            log.error("Error processing Gemini message: {}", message, e);
        }
    }
    
    @Override
    public void onMessage(ByteBuffer bytes) {
        try {
            // Convert ByteBuffer to String for processing
            byte[] array = new byte[bytes.remaining()];
            bytes.get(array);
            String message = new String(array, java.nio.charset.StandardCharsets.UTF_8);
            log.info("Received BINARY message from Gemini: {}", message);
            
            // Process the same way as text messages
            Map<String, Object> response = objectMapper.readValue(message, Map.class);
            
            if (response.containsKey("setupComplete")) {
                handleSetupComplete(response);
            } else if (response.containsKey("serverContent")) {
                handleServerContent(response);
            } else if (response.containsKey("toolCall")) {
                handleToolCall(response);
            } else {
                log.warn("Received unknown binary message type: {}", response.keySet());
            }
        } catch (Exception e) {
            log.error("Error processing binary message", e);
        }
    }
    
    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.error("Disconnected from Gemini Live API. Code: {}, Reason: {}, Remote: {}", code, reason, remote);
        isConnected = false;
        
        // Common close codes
        switch (code) {
            case 1000:
                log.info("Normal closure");
                break;
            case 1001:
                log.info("Going away");
                break;
            case 1002:
                log.error("Protocol error");
                break;
            case 1003:
                log.error("Unsupported data");
                break;
            case 1006:
                log.error("Abnormal closure - no close frame received");
                break;
            case 1008:
                log.error("Policy violation - possibly invalid API key or setup message");
                break;
            case 1011:
                log.error("Server error");
                break;
            default:
                log.error("Unknown close code: {}", code);
        }
        
        // Notify all sessions about disconnection
        sessionHandlers.values().forEach(handler -> handler.onDisconnect());
    }
    
    @Override
    public void onError(Exception ex) {
        log.error("WebSocket error: {}", ex.getMessage(), ex);
        isConnected = false;
    }
    
    public void setupSession(String sessionId, SessionHandler handler) {
        sessionHandlers.put(sessionId, handler);
        
        // If we have an existing connection that's not in a good state, close it
        if (isOpen() && !isConnected) {
            log.info("WebSocket in bad state, closing existing connection");
            try {
                closeBlocking();
            } catch (InterruptedException e) {
                log.error("Error closing connection", e);
                Thread.currentThread().interrupt();
            }
        }
        
        // Ensure we're connected
        if (!isConnected || !isOpen()) {
            log.info("WebSocket not connected, attempting to connect...");
            try {
                // Reconnect to ensure fresh connection
                if (getReadyState() == ReadyState.CLOSED) {
                    reconnectBlocking();
                } else {
                    boolean connected = this.connectBlocking();
                    log.info("Connection attempt result: {}", connected);
                    if (!connected) {
                        log.error("Failed to connect to Gemini WebSocket");
                        handler.onDisconnect();
                        return;
                    }
                }
            } catch (InterruptedException e) {
                log.error("Connection interrupted", e);
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        if (isConnected) {
            // Add a small delay to ensure connection is fully established
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendSetupMessage(sessionId);
        } else {
            log.warn("WebSocket not connected yet, setup will be sent when connection is established");
        }
    }
    
    public void removeSession(String sessionId) {
        sessionHandlers.remove(sessionId);
        
        // If no more sessions, close the connection to ensure clean state for next session
        if (sessionHandlers.isEmpty() && isOpen()) {
            log.info("No active sessions, closing WebSocket connection");
            try {
                closeBlocking();
                isConnected = false;
            } catch (InterruptedException e) {
                log.error("Error closing WebSocket", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void sendRealtimeInput(String sessionId, List<MediaChunk> mediaChunks) {
        try {
            if (!isConnected) {
                log.warn("Cannot send realtime input - not connected to Gemini");
                return;
            }
            
            Map<String, Object> message = new HashMap<>();
            Map<String, Object> realtimeInput = new HashMap<>();
            
            // Convert MediaChunk to proper format matching GenerativeContentBlob
            List<Map<String, Object>> chunks = new ArrayList<>();
            for (MediaChunk chunk : mediaChunks) {
                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put("mimeType", chunk.getMimeType());
                chunkMap.put("data", chunk.getData());
                chunks.add(chunkMap);
            }
            
            realtimeInput.put("mediaChunks", chunks);
            message.put("realtimeInput", realtimeInput);
            
            String json = objectMapper.writeValueAsString(message);
            log.debug("Sending realtime input JSON: {}", json);
            send(json);
            log.debug("Sent realtime input for session: {} with {} chunks", sessionId, chunks.size());
        } catch (Exception e) {
            log.error("Error sending realtime input", e);
        }
    }
    
    public void sendTextMessage(String sessionId, String text) {
        try {
            Map<String, Object> message = new HashMap<>();
            Map<String, Object> clientContent = new HashMap<>();
            
            List<Map<String, Object>> turns = new ArrayList<>();
            Map<String, Object> turn = new HashMap<>();
            turn.put("role", "user");
            
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", text);
            parts.add(part);
            
            turn.put("parts", parts);
            turns.add(turn);
            
            clientContent.put("turns", turns);
            clientContent.put("turnComplete", true);
            message.put("clientContent", clientContent);
            
            String json = objectMapper.writeValueAsString(message);
            send(json);
            log.debug("Sent text message for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending text message", e);
        }
    }
    
    private void sendSetupMessage(String sessionId) {
        try {
            // Create setup matching live-api-web-console exactly
            Map<String, Object> setup = new HashMap<>();
            setup.put("model", "models/gemini-2.0-flash-exp");
            
            // Generation config with audio response
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("responseModalities", "audio");
            
            // Speech config with voice
            Map<String, Object> speechConfig = new HashMap<>();
            Map<String, Object> voiceConfig = new HashMap<>();
            Map<String, Object> prebuiltVoiceConfig = new HashMap<>();
            prebuiltVoiceConfig.put("voiceName", defaultVoice != null ? defaultVoice : "Aoede");
            voiceConfig.put("prebuiltVoiceConfig", prebuiltVoiceConfig);
            speechConfig.put("voiceConfig", voiceConfig);
            generationConfig.put("speechConfig", speechConfig);
            
            setup.put("generationConfig", generationConfig);
            
            // System instruction
            Map<String, Object> systemInstructionObj = new HashMap<>();
            List<Map<String, Object>> parts = new ArrayList<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", systemInstruction != null ? systemInstruction : 
                "You are an AI interviewer conducting a technical interview. Be professional, ask relevant questions based on the candidate's responses, and evaluate their technical skills.");
            parts.add(part);
            systemInstructionObj.put("parts", parts);
            setup.put("systemInstruction", systemInstructionObj);
            
            // Tools - matching live-api-web-console format
            List<Map<String, Object>> tools = new ArrayList<>();
            
            // Google Search tool
            Map<String, Object> googleSearchTool = new HashMap<>();
            googleSearchTool.put("googleSearch", new HashMap<>());
            tools.add(googleSearchTool);
            
            setup.put("tools", tools);
            
            // Use the SetupMessage class for proper serialization
            SetupMessage message = new SetupMessage(setup);
            
            String json = objectMapper.writeValueAsString(message);
            log.info("Sending full setup message: {}", json);
            
            if (!isOpen()) {
                log.error("WebSocket is not open! State: {}", getReadyState());
                return;
            }
            
            send(json);
            log.info("Setup message sent successfully for session: {}", sessionId);
        } catch (Exception e) {
            log.error("Error sending setup message", e);
        }
    }
    
    private void handleSetupComplete(Map<String, Object> response) {
        log.info("Setup complete");
        // Notify all sessions that setup is complete
        sessionHandlers.values().forEach(handler -> handler.onSetupComplete());
    }
    
    private void handleServerContent(Map<String, Object> response) {
        Map<String, Object> serverContent = (Map<String, Object>) response.get("serverContent");
        
        // Check for interrupted
        if (serverContent.containsKey("interrupted") && Boolean.TRUE.equals(serverContent.get("interrupted"))) {
            log.info("Received interrupted signal");
            sessionHandlers.values().forEach(handler -> handler.onInterrupted());
            return;
        }
        
        // Check for turn complete
        if (serverContent.containsKey("turnComplete") && Boolean.TRUE.equals(serverContent.get("turnComplete"))) {
            log.info("Turn complete");
            sessionHandlers.values().forEach(handler -> handler.onTurnComplete());
            // Continue processing in case there's more content
        }
        
        // Check for model turn
        if (serverContent.containsKey("modelTurn")) {
            Map<String, Object> modelTurn = (Map<String, Object>) serverContent.get("modelTurn");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) modelTurn.get("parts");
            
            if (parts != null) {
                for (Map<String, Object> part : parts) {
                    if (part.containsKey("inlineData")) {
                        Map<String, Object> inlineData = (Map<String, Object>) part.get("inlineData");
                        String mimeType = (String) inlineData.get("mimeType");
                        String data = (String) inlineData.get("data");
                        
                        // Check if it's audio data
                        if (mimeType != null && mimeType.startsWith("audio/pcm")) {
                            // Send audio data to all active sessions
                            sessionHandlers.values().forEach(handler -> 
                                handler.onAudioData(mimeType, data));
                        }
                    } else if (part.containsKey("text")) {
                        String text = (String) part.get("text");
                        // Send text to all active sessions
                        sessionHandlers.values().forEach(handler -> 
                            handler.onTextResponse(text));
                    }
                }
            }
        }
    }
    
    private void handleToolCall(Map<String, Object> response) {
        // Handle tool calls if needed
        log.debug("Tool call received: {}", response);
    }
    
    public interface SessionHandler {
        void onSetupComplete();
        void onAudioData(String mimeType, String base64Data);
        void onTextResponse(String text);
        void onTurnComplete();
        void onInterrupted();
        void onDisconnect();
    }
}
