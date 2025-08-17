package com.interview.app.dto;

import com.interview.app.model.Candidate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewRoundDto {
    
    private Candidate.InterviewRound.InterviewType interviewType;
    private LocalDateTime scheduledAt;
    private Candidate.InterviewRound.InterviewStatus status;
    private String interviewId;
    private Candidate.InterviewRound.InterviewLevel level;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    public static InterviewRoundDto fromInterviewRound(Candidate.InterviewRound round) {
        InterviewRoundDto dto = new InterviewRoundDto();
        dto.setInterviewType(round.getInterviewType());
        dto.setScheduledAt(round.getScheduledAt());
        dto.setStatus(round.getStatus());
        dto.setInterviewId(round.getInterviewId());
        dto.setLevel(round.getLevel());
        dto.setStartedAt(round.getStartedAt());
        dto.setCompletedAt(round.getCompletedAt());
        return dto;
    }
    
    public Candidate.InterviewRound toInterviewRound() {
        Candidate.InterviewRound round = new Candidate.InterviewRound();
        round.setInterviewType(this.interviewType);
        round.setScheduledAt(this.scheduledAt);
        round.setStatus(this.status);
        round.setInterviewId(this.interviewId);
        round.setLevel(this.level);
        round.setStartedAt(this.startedAt);
        round.setCompletedAt(this.completedAt);
        return round;
    }
}
