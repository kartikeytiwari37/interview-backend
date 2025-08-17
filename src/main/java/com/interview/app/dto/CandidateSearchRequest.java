package com.interview.app.dto;

import com.interview.app.model.Candidate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateSearchRequest {
    
    // Basic candidate fields
    private String email;
    private String firstName;
    private String lastName;
    private Candidate.JobProfile jobProfile;
    private String jobLocation;
    private Candidate.CandidateStatus status;
    
    // Date range filters
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime updatedAfter;
    private LocalDateTime updatedBefore;
    
    // Interview round filters
    private Candidate.InterviewRound.InterviewType interviewType;
    private Candidate.InterviewRound.InterviewStatus interviewStatus;
    private Candidate.InterviewRound.InterviewLevel interviewLevel;
    private LocalDateTime scheduledAfter;
    private LocalDateTime scheduledBefore;
    
    // Multiple values support
    private List<Candidate.JobProfile> jobProfiles;
    private List<Candidate.CandidateStatus> statuses;
    private List<String> jobLocations;
    
    // Text search
    private String searchText; // For searching in name or email
}
