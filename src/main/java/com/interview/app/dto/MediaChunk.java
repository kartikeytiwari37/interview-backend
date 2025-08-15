package com.interview.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaChunk {
    private String mimeType;
    private String data; // Base64 encoded data
}
