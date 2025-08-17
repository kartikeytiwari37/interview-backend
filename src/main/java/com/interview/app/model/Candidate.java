package com.interview.app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "candidates")
@CompoundIndexes({
    @CompoundIndex(name = "job_profile_location_idx", def = "{'jobProfile': 1, 'jobLocation': 1}"),
    @CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "job_profile_status_idx", def = "{'jobProfile': 1, 'status': 1}"),
    @CompoundIndex(name = "location_status_idx", def = "{'jobLocation': 1, 'status': 1}"),
    @CompoundIndex(name = "interview_rounds_status_idx", def = "{'interviewRounds.status': 1, 'status': 1}"),
    @CompoundIndex(name = "interview_rounds_type_level_idx", def = "{'interviewRounds.interviewType': 1, 'interviewRounds.level': 1}"),
    @CompoundIndex(name = "scheduled_interviews_idx", def = "{'interviewRounds.scheduledAt': 1, 'interviewRounds.status': 1}")
})
public class Candidate {
    
    @Id
    private String id;
    
    private String firstName;
    
    private String lastName;
    
    @Indexed(unique = true)
    private String email;
    
    private JobProfile jobProfile;
    
    private String jobLocation;
    
    private List<InterviewRound> interviewRounds = new ArrayList<>();
    
    private CandidateStatus status = CandidateStatus.TO_BE_STARTED;
    
    private LocalDateTime createdAt = LocalDateTime.now();
    
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    public enum JobProfile {
        SDE1, SDE2
    }
    
    public enum CandidateStatus {
        TO_BE_STARTED, IN_PROGRESS, SELECTED, REJECTED
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InterviewRound {
        private InterviewType interviewType;
        private LocalDateTime scheduledAt;
        private InterviewStatus status = InterviewStatus.TO_BE_STARTED;
        private String interviewId;
        private InterviewLevel level;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        
        public enum InterviewType {
            PROBLEM_SOLVING, LLD, HLD, BEHAVIORAL, HR
        }
        
        public enum InterviewStatus {
            TO_BE_STARTED, IN_PROGRESS, SELECTED, REJECTED
        }
        
        public enum InterviewLevel {
            EASY, MEDIUM, HARD
        }
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
