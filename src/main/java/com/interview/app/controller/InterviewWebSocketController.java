package com.interview.app.controller;

import com.interview.app.dto.InterviewMessage;
import com.interview.app.security.UserPrincipal;
import com.interview.app.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class InterviewWebSocketController {
    
    private final InterviewService interviewService;
    
    @MessageMapping("/interview/start")
    @SendToUser("/queue/session")
    public String startInterview(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated user trying to start interview");
            throw new RuntimeException("User not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String userId = userPrincipal.getId();
        
        log.info("Starting interview for authenticated user: {} ({})", userPrincipal.getEmail(), userId);
        return interviewService.startInterview(userId);
    }
    
    @MessageMapping("/interview/message")
    public void handleMessage(@Payload InterviewMessage message, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated user trying to send message");
            return;
        }
        
        log.debug("Received message for session: {}", message.getSessionId());
        interviewService.processMessage(message);
    }
    
    @MessageMapping("/interview/end")
    public void endInterview(@Payload String sessionId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Unauthenticated user trying to end interview");
            return;
        }
        
        log.info("Ending interview session: {}", sessionId);
        interviewService.endInterview(sessionId);
    }
}
