package com.interview.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.app.websocket.GeminiWebSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.net.URI;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Configuration
public class GeminiConfig {
    
    @Value("${gemini.api.websocket-url}")
    private String geminiWebSocketUrl;
    
    @Value("${gemini.api.key}")
    private String geminiApiKey;
    
    @Bean
    public GeminiWebSocketClient geminiWebSocketClient(ObjectMapper objectMapper) throws Exception {
        String urlWithKey = geminiWebSocketUrl + "?key=" + geminiApiKey;
        log.info("Connecting to Gemini WebSocket URL: {}", geminiWebSocketUrl + "?key=***");
        
        URI serverUri = new URI(urlWithKey);
        
        GeminiWebSocketClient client = new GeminiWebSocketClient(serverUri, objectMapper);
        
        // Configure SSL to trust all certificates (matching the working implementation)
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
            log.info("SSL context configured to trust all certificates");
        } catch (Exception e) {
            log.error("Failed to configure SSL context", e);
        }
        
        // Don't connect immediately - let Spring manage the lifecycle
        log.info("GeminiWebSocketClient bean created, connection will be established on first use");
        
        return client;
    }
}
