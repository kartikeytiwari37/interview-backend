package com.interview.app.controller;

import com.interview.app.dto.*;
import com.interview.app.model.Candidate;
import com.interview.app.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/candidates")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:3001"})
public class CandidateController {

    private final CandidateService candidateService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCandidate(@Valid @RequestBody CreateCandidateRequest request) {
        try {
            CandidateDto candidate = candidateService.createCandidate(request);
            log.info("Created candidate: {}", candidate.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(candidate);
        } catch (RuntimeException e) {
            log.error("Error creating candidate: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CandidateDto>> getAllCandidates() {
        List<CandidateDto> candidates = candidateService.getAllCandidates();
        return ResponseEntity.ok(candidates);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @candidateService.isUserOwnCandidate(authentication.name, #id)")
    public ResponseEntity<?> getCandidateById(@PathVariable String id) {
        return candidateService.getCandidateById(id)
            .map(candidate -> ResponseEntity.ok(candidate))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email}")
    @PreAuthorize("hasRole('ADMIN') or authentication.name == #email")
    public ResponseEntity<?> getCandidateByEmail(@PathVariable String email) {
        return candidateService.getCandidateByEmail(email)
            .map(candidate -> ResponseEntity.ok(candidate))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCandidate(@PathVariable String id, 
                                           @Valid @RequestBody UpdateCandidateRequest request) {
        try {
            CandidateDto updatedCandidate = candidateService.updateCandidate(id, request);
            log.info("Updated candidate: {}", updatedCandidate.getEmail());
            return ResponseEntity.ok(updatedCandidate);
        } catch (RuntimeException e) {
            log.error("Error updating candidate: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCandidate(@PathVariable String id) {
        try {
            candidateService.deleteCandidate(id);
            log.info("Deleted candidate with id: {}", id);
            return ResponseEntity.ok(Map.of("message", "Candidate deleted successfully"));
        } catch (RuntimeException e) {
            log.error("Error deleting candidate: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Unified search endpoint with POST and request body
    @PostMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CandidateSearchResponse> searchCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestBody(required = false) CandidateSearchRequest searchRequest) {
        
        // If no search request body provided, create empty one
        if (searchRequest == null) {
            searchRequest = new CandidateSearchRequest();
        }
        
        CandidateSearchResponse response = candidateService.searchCandidates(
            searchRequest, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(response);
    }

    // Interview round management endpoints
    @PostMapping("/{candidateId}/interview-rounds")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addInterviewRound(@PathVariable String candidateId, 
                                             @Valid @RequestBody InterviewRoundDto interviewRoundDto) {
        try {
            Candidate.InterviewRound interviewRound = interviewRoundDto.toInterviewRound();
            CandidateDto updatedCandidate = candidateService.addInterviewRound(candidateId, interviewRound);
            log.info("Added interview round for candidate: {}", candidateId);
            return ResponseEntity.ok(updatedCandidate);
        } catch (RuntimeException e) {
            log.error("Error adding interview round: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{candidateId}/interview-rounds/{interviewId}/status")
    @PreAuthorize("hasRole('ADMIN') or @candidateService.isUserOwnCandidate(authentication.name, #candidateId)")
    public ResponseEntity<?> updateInterviewRoundStatus(@PathVariable String candidateId,
                                                       @PathVariable String interviewId,
                                                       @RequestBody Map<String, String> statusUpdate) {
        try {
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Status is required"));
            }

            Candidate.InterviewRound.InterviewStatus status = 
                Candidate.InterviewRound.InterviewStatus.valueOf(statusStr);
            
            CandidateDto updatedCandidate = candidateService.updateInterviewRoundStatus(candidateId, interviewId, status);
            log.info("Updated interview round status for candidate: {} interview: {}", candidateId, interviewId);
            return ResponseEntity.ok(updatedCandidate);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid status value"));
        } catch (RuntimeException e) {
            log.error("Error updating interview round status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Alternative endpoint for frontend compatibility - using round index instead of interview ID
    @PutMapping("/{candidateId}/rounds/{roundIndex}/status")
    @PreAuthorize("hasRole('ADMIN') or @candidateService.isUserOwnCandidate(authentication.name, #candidateId)")
    public ResponseEntity<?> updateInterviewRoundStatusByIndex(@PathVariable String candidateId,
                                                              @PathVariable int roundIndex,
                                                              @RequestBody Map<String, String> statusUpdate) {
        try {
            String statusStr = statusUpdate.get("status");
            if (statusStr == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Status is required"));
            }

            Candidate.InterviewRound.InterviewStatus status = 
                Candidate.InterviewRound.InterviewStatus.valueOf(statusStr);
            
            CandidateDto updatedCandidate = candidateService.updateInterviewRoundStatusByIndex(candidateId, roundIndex, status);
            log.info("Updated interview round status for candidate: {} round index: {}", candidateId, roundIndex);
            return ResponseEntity.ok(updatedCandidate);
        } catch (IllegalArgumentException e) {
            log.error("Invalid status value: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid status value"));
        } catch (RuntimeException e) {
            log.error("Error updating interview round status: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    // Statistics endpoint
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getCandidateStats() {
        List<CandidateDto> allCandidates = candidateService.getAllCandidates();
        
        long totalCandidates = allCandidates.size();
        long toBeStarted = allCandidates.stream()
            .filter(c -> c.getStatus() == Candidate.CandidateStatus.TO_BE_STARTED)
            .count();
        long inProgress = allCandidates.stream()
            .filter(c -> c.getStatus() == Candidate.CandidateStatus.IN_PROGRESS)
            .count();
        long selected = allCandidates.stream()
            .filter(c -> c.getStatus() == Candidate.CandidateStatus.SELECTED)
            .count();
        long rejected = allCandidates.stream()
            .filter(c -> c.getStatus() == Candidate.CandidateStatus.REJECTED)
            .count();

        Map<String, Object> stats = Map.of(
            "totalCandidates", totalCandidates,
            "toBeStarted", toBeStarted,
            "inProgress", inProgress,
            "selected", selected,
            "rejected", rejected
        );

        return ResponseEntity.ok(stats);
    }
}
