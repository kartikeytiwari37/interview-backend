package com.interview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewMessage {
    private MessageType type;
    private String sessionId;
    private String content;
    private List<MediaChunk> mediaChunks;
    private Long timestamp;
    
    public enum MessageType {
        TEXT,
        AUDIO,
        VIDEO,
        SCREEN_SHARE,
        MIXED,
        CONTROL
    }
}
