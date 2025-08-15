package com.interview.app.controller;

import com.interview.app.service.InterviewService;
import com.interview.app.dto.MediaChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class MediaController {
    
    private final InterviewService interviewService;
    
    @PostMapping("/stream/{sessionId}")
    public ResponseEntity<Map<String, String>> streamMedia(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> payload) {
        try {
            // Extract media chunks from payload
            List<Map<String, String>> chunks = (List<Map<String, String>>) payload.get("mediaChunks");
            
            for (Map<String, String> chunk : chunks) {
                MediaChunk mediaChunk = new MediaChunk();
                mediaChunk.setMimeType(chunk.get("mimeType"));
                mediaChunk.setData(chunk.get("data"));
                
                // Send directly to Gemini, bypassing STOMP
                interviewService.sendMediaToGemini(sessionId, List.of(mediaChunk));
            }
            
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error streaming media", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}
