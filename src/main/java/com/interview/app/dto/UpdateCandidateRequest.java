package com.interview.app.dto;

import com.interview.app.model.Candidate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCandidateRequest {
    
    private String firstName;
    
    private String lastName;
    
    @Email(message = "Email should be valid")
    private String email;
    
    private Candidate.JobProfile jobProfile;
    
    private String jobLocation;
    
    private List<InterviewRoundDto> interviewRounds;
    
    private Candidate.CandidateStatus status;
}
