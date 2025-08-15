package com.interview.app.controller;

import com.interview.app.dto.InterviewMessage;
import com.interview.app.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class InterviewWebSocketController {
    
    private final InterviewService interviewService;
    
    @MessageMapping("/interview/start")
    @SendToUser("/queue/session")
    public String startInterview(@Payload String userId) {
        log.info("Starting interview for user: {}", userId);
        return interviewService.startInterview(userId);
    }
    
    @MessageMapping("/interview/message")
    public void handleMessage(@Payload InterviewMessage message) {
        log.debug("Received message for session: {}", message.getSessionId());
        interviewService.processMessage(message);
    }
    
    @MessageMapping("/interview/end")
    public void endInterview(@Payload String sessionId) {
        log.info("Ending interview session: {}", sessionId);
        interviewService.endInterview(sessionId);
    }
}
