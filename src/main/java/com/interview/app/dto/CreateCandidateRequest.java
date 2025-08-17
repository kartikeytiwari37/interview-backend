package com.interview.app.dto;

import com.interview.app.model.Candidate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCandidateRequest {
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    @NotNull(message = "Job profile is required")
    private Candidate.JobProfile jobProfile;
    
    @NotBlank(message = "Job location is required")
    private String jobLocation;
    
    private List<InterviewRoundDto> interviewRounds = new ArrayList<>();
    
    private Candidate.CandidateStatus status = Candidate.CandidateStatus.TO_BE_STARTED;
}
