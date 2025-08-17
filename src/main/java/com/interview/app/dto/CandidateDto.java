package com.interview.app.dto;

import com.interview.app.model.Candidate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Candidate.JobProfile jobProfile;
    private String jobLocation;
    private List<InterviewRoundDto> interviewRounds;
    private Candidate.CandidateStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CandidateDto fromCandidate(Candidate candidate) {
        CandidateDto dto = new CandidateDto();
        dto.setId(candidate.getId());
        dto.setFirstName(candidate.getFirstName());
        dto.setLastName(candidate.getLastName());
        dto.setEmail(candidate.getEmail());
        dto.setJobProfile(candidate.getJobProfile());
        dto.setJobLocation(candidate.getJobLocation());
        dto.setStatus(candidate.getStatus());
        dto.setCreatedAt(candidate.getCreatedAt());
        dto.setUpdatedAt(candidate.getUpdatedAt());
        
        if (candidate.getInterviewRounds() != null) {
            dto.setInterviewRounds(candidate.getInterviewRounds().stream()
                .map(InterviewRoundDto::fromInterviewRound)
                .collect(Collectors.toList()));
        }
        
        return dto;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
