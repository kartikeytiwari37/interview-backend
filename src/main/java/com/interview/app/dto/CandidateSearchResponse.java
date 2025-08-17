package com.interview.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSearchResponse {
    
    private List<CandidateDto> candidates;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;
    
    public static CandidateSearchResponse of(List<CandidateDto> candidates, long totalElements, 
                                           int currentPage, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        boolean hasNext = currentPage < totalPages - 1;
        boolean hasPrevious = currentPage > 0;
        
        return new CandidateSearchResponse(
            candidates, totalElements, totalPages, currentPage, pageSize, hasNext, hasPrevious
        );
    }
}
