package com.interview.app.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiConnectionPool {
    
    private final ObjectMapper objectMapper;
    private final Map<String, GeminiWebSocketClient> connections = new ConcurrentHashMap<>();
    
    @Value("${gemini.api.websocket-url}")
    private String geminiWebSocketUrl;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Value("${interview.config.default-voice}")
    private String defaultVoice;
    
    @Value("${interview.config.system-instruction}")
    private String systemInstruction;
    
    /**
     * Get or create a dedicated Gemini WebSocket connection for a session
     */
    public GeminiWebSocketClient getConnection(String sessionId) {
        return connections.computeIfAbsent(sessionId, this::createConnection);
    }
    
    /**
     * Remove and close connection for a session
     */
    public void removeConnection(String sessionId) {
        GeminiWebSocketClient client = connections.remove(sessionId);
        if (client != null) {
            try {
                log.info("Closing Gemini connection for session: {}", sessionId);
                client.closeBlocking();
            } catch (InterruptedException e) {
                log.error("Error closing connection for session: {}", sessionId, e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Get count of active connections
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }
    
    /**
     * Check if session has an active connection
     */
    public boolean hasConnection(String sessionId) {
        GeminiWebSocketClient client = connections.get(sessionId);
        return client != null && client.isOpen();
    }
    
    /**
     * Close all connections (for shutdown)
     */
    @PreDestroy
    public void closeAllConnections() {
        log.info("Closing all Gemini connections. Count: {}", connections.size());
        connections.forEach((sessionId, client) -> {
            try {
                client.closeBlocking();
            } catch (InterruptedException e) {
                log.error("Error closing connection for session: {}", sessionId, e);
                Thread.currentThread().interrupt();
            }
        });
        connections.clear();
    }
    
    private GeminiWebSocketClient createConnection(String sessionId) {
        try {
            String urlWithKey = geminiWebSocketUrl + "?key=" + geminiApiKey;
            log.info("Creating dedicated Gemini connection for session: {}", sessionId);
            
            URI serverUri = new URI(urlWithKey);
            
            GeminiWebSocketClient client = new GeminiWebSocketClient(
                serverUri, 
                objectMapper, 
                sessionId,  // Pass session ID to client
                defaultVoice,
                systemInstruction
            );
            
            // Configure SSL to trust all certificates
            try {
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { }
                    }
                };
                
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                client.setSocketFactory(sslContext.getSocketFactory());
                log.debug("SSL context configured for session: {}", sessionId);
            } catch (Exception e) {
                log.error("Failed to configure SSL context for session: {}", sessionId, e);
            }
            
            log.info("Created dedicated Gemini connection for session: {}", sessionId);
            return client;
            
        } catch (Exception e) {
            log.error("Failed to create Gemini connection for session: {}", sessionId, e);
            throw new RuntimeException("Failed to create Gemini connection", e);
        }
    }
}
