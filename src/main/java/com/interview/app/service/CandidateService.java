package com.interview.app.service;

import com.interview.app.dto.CandidateDto;
import com.interview.app.dto.CandidateSearchRequest;
import com.interview.app.dto.CandidateSearchResponse;
import com.interview.app.dto.CreateCandidateRequest;
import com.interview.app.dto.UpdateCandidateRequest;
import com.interview.app.model.Candidate;
import com.interview.app.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateService {

    private final CandidateRepository candidateRepository;

    public CandidateDto createCandidate(CreateCandidateRequest request) {
        if (candidateRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Candidate with email " + request.getEmail() + " already exists");
        }

        Candidate candidate = new Candidate();
        candidate.setFirstName(request.getFirstName());
        candidate.setLastName(request.getLastName());
        candidate.setEmail(request.getEmail());
        candidate.setJobProfile(request.getJobProfile());
        candidate.setJobLocation(request.getJobLocation());
        candidate.setStatus(request.getStatus());
        candidate.setCreatedAt(LocalDateTime.now());
        candidate.setUpdatedAt(LocalDateTime.now());

        // Convert InterviewRoundDto to InterviewRound
        if (request.getInterviewRounds() != null && !request.getInterviewRounds().isEmpty()) {
            candidate.setInterviewRounds(request.getInterviewRounds().stream()
                .map(dto -> dto.toInterviewRound())
                .collect(Collectors.toList()));
        }

        Candidate savedCandidate = candidateRepository.save(candidate);
        log.info("Created new candidate: {} {}", savedCandidate.getFirstName(), savedCandidate.getLastName());

        return CandidateDto.fromCandidate(savedCandidate);
    }

    public List<CandidateDto> getAllCandidates() {
        return candidateRepository.findAll().stream()
            .map(CandidateDto::fromCandidate)
            .collect(Collectors.toList());
    }

    public Optional<CandidateDto> getCandidateById(String id) {
        return candidateRepository.findById(id)
            .map(CandidateDto::fromCandidate);
    }

    public Optional<CandidateDto> getCandidateByEmail(String email) {
        return candidateRepository.findByEmail(email)
            .map(CandidateDto::fromCandidate);
    }

    public CandidateDto updateCandidate(String id, UpdateCandidateRequest request) {
        Candidate candidate = candidateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + id));

        // Check if email is being updated and if it already exists
        if (request.getEmail() != null && !request.getEmail().equals(candidate.getEmail())) {
            if (candidateRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Candidate with email " + request.getEmail() + " already exists");
            }
            candidate.setEmail(request.getEmail());
        }

        // Update fields if provided
        if (request.getFirstName() != null) {
            candidate.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            candidate.setLastName(request.getLastName());
        }
        if (request.getJobProfile() != null) {
            candidate.setJobProfile(request.getJobProfile());
        }
        if (request.getJobLocation() != null) {
            candidate.setJobLocation(request.getJobLocation());
        }
        if (request.getStatus() != null) {
            candidate.setStatus(request.getStatus());
        }
        if (request.getInterviewRounds() != null) {
            candidate.setInterviewRounds(request.getInterviewRounds().stream()
                .map(dto -> dto.toInterviewRound())
                .collect(Collectors.toList()));
        }

        candidate.setUpdatedAt(LocalDateTime.now());

        Candidate updatedCandidate = candidateRepository.save(candidate);
        log.info("Updated candidate: {} {}", updatedCandidate.getFirstName(), updatedCandidate.getLastName());

        return CandidateDto.fromCandidate(updatedCandidate);
    }

    public void deleteCandidate(String id) {
        if (!candidateRepository.existsById(id)) {
            throw new RuntimeException("Candidate not found with id: " + id);
        }
        candidateRepository.deleteById(id);
        log.info("Deleted candidate with id: {}", id);
    }

    // Unified search method
    public CandidateSearchResponse searchCandidates(CandidateSearchRequest searchRequest, 
                                                   int page, int size, String sortBy, String sortDirection) {
        
        // Build dynamic query using MongoDB criteria
        List<Candidate> allCandidates = candidateRepository.findAll();
        
        // Apply filters
        List<Candidate> filteredCandidates = allCandidates.stream()
            .filter(candidate -> matchesSearchCriteria(candidate, searchRequest))
            .collect(Collectors.toList());
        
        // Apply sorting
        filteredCandidates = applySorting(filteredCandidates, sortBy, sortDirection);
        
        // Calculate pagination
        long totalElements = filteredCandidates.size();
        int startIndex = page * size;
        int endIndex = Math.min(startIndex + size, filteredCandidates.size());
        
        // Get page data
        List<Candidate> pageData = filteredCandidates.subList(startIndex, endIndex);
        List<CandidateDto> candidateDtos = pageData.stream()
            .map(CandidateDto::fromCandidate)
            .collect(Collectors.toList());
        
        return CandidateSearchResponse.of(candidateDtos, totalElements, page, size);
    }
    
    private boolean matchesSearchCriteria(Candidate candidate, CandidateSearchRequest searchRequest) {
        // Email filter
        if (searchRequest.getEmail() != null && !searchRequest.getEmail().isEmpty()) {
            if (!candidate.getEmail().toLowerCase().contains(searchRequest.getEmail().toLowerCase())) {
                return false;
            }
        }
        
        // First name filter
        if (searchRequest.getFirstName() != null && !searchRequest.getFirstName().isEmpty()) {
            if (!candidate.getFirstName().toLowerCase().contains(searchRequest.getFirstName().toLowerCase())) {
                return false;
            }
        }
        
        // Last name filter
        if (searchRequest.getLastName() != null && !searchRequest.getLastName().isEmpty()) {
            if (!candidate.getLastName().toLowerCase().contains(searchRequest.getLastName().toLowerCase())) {
                return false;
            }
        }
        
        // Job profile filter (single)
        if (searchRequest.getJobProfile() != null) {
            if (!candidate.getJobProfile().equals(searchRequest.getJobProfile())) {
                return false;
            }
        }
        
        // Job profiles filter (multiple)
        if (searchRequest.getJobProfiles() != null && !searchRequest.getJobProfiles().isEmpty()) {
            if (!searchRequest.getJobProfiles().contains(candidate.getJobProfile())) {
                return false;
            }
        }
        
        // Job location filter (single)
        if (searchRequest.getJobLocation() != null && !searchRequest.getJobLocation().isEmpty()) {
            if (!candidate.getJobLocation().toLowerCase().contains(searchRequest.getJobLocation().toLowerCase())) {
                return false;
            }
        }
        
        // Job locations filter (multiple)
        if (searchRequest.getJobLocations() != null && !searchRequest.getJobLocations().isEmpty()) {
            boolean locationMatch = searchRequest.getJobLocations().stream()
                .anyMatch(location -> candidate.getJobLocation().toLowerCase().contains(location.toLowerCase()));
            if (!locationMatch) {
                return false;
            }
        }
        
        // Status filter (single)
        if (searchRequest.getStatus() != null) {
            if (!candidate.getStatus().equals(searchRequest.getStatus())) {
                return false;
            }
        }
        
        // Statuses filter (multiple)
        if (searchRequest.getStatuses() != null && !searchRequest.getStatuses().isEmpty()) {
            if (!searchRequest.getStatuses().contains(candidate.getStatus())) {
                return false;
            }
        }
        
        // Date range filters
        if (searchRequest.getCreatedAfter() != null) {
            if (candidate.getCreatedAt().isBefore(searchRequest.getCreatedAfter())) {
                return false;
            }
        }
        
        if (searchRequest.getCreatedBefore() != null) {
            if (candidate.getCreatedAt().isAfter(searchRequest.getCreatedBefore())) {
                return false;
            }
        }
        
        if (searchRequest.getUpdatedAfter() != null) {
            if (candidate.getUpdatedAt().isBefore(searchRequest.getUpdatedAfter())) {
                return false;
            }
        }
        
        if (searchRequest.getUpdatedBefore() != null) {
            if (candidate.getUpdatedAt().isAfter(searchRequest.getUpdatedBefore())) {
                return false;
            }
        }
        
        // Interview round filters
        if (searchRequest.getInterviewType() != null || searchRequest.getInterviewStatus() != null || 
            searchRequest.getInterviewLevel() != null || searchRequest.getScheduledAfter() != null || 
            searchRequest.getScheduledBefore() != null) {
            
            boolean hasMatchingRound = candidate.getInterviewRounds().stream()
                .anyMatch(round -> matchesInterviewRoundCriteria(round, searchRequest));
            
            if (!hasMatchingRound) {
                return false;
            }
        }
        
        // Text search (searches in name and email)
        if (searchRequest.getSearchText() != null && !searchRequest.getSearchText().isEmpty()) {
            String searchText = searchRequest.getSearchText().toLowerCase();
            boolean textMatch = candidate.getFirstName().toLowerCase().contains(searchText) ||
                               candidate.getLastName().toLowerCase().contains(searchText) ||
                               candidate.getEmail().toLowerCase().contains(searchText);
            if (!textMatch) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean matchesInterviewRoundCriteria(Candidate.InterviewRound round, CandidateSearchRequest searchRequest) {
        if (searchRequest.getInterviewType() != null && !round.getInterviewType().equals(searchRequest.getInterviewType())) {
            return false;
        }
        
        if (searchRequest.getInterviewStatus() != null && !round.getStatus().equals(searchRequest.getInterviewStatus())) {
            return false;
        }
        
        if (searchRequest.getInterviewLevel() != null && !round.getLevel().equals(searchRequest.getInterviewLevel())) {
            return false;
        }
        
        if (searchRequest.getScheduledAfter() != null && round.getScheduledAt() != null) {
            if (round.getScheduledAt().isBefore(searchRequest.getScheduledAfter())) {
                return false;
            }
        }
        
        if (searchRequest.getScheduledBefore() != null && round.getScheduledAt() != null) {
            if (round.getScheduledAt().isAfter(searchRequest.getScheduledBefore())) {
                return false;
            }
        }
        
        return true;
    }
    
    private List<Candidate> applySorting(List<Candidate> candidates, String sortBy, String sortDirection) {
        boolean ascending = "ASC".equalsIgnoreCase(sortDirection);
        
        return candidates.stream()
            .sorted((c1, c2) -> {
                int comparison = 0;
                
                switch (sortBy.toLowerCase()) {
                    case "firstname":
                        comparison = c1.getFirstName().compareToIgnoreCase(c2.getFirstName());
                        break;
                    case "lastname":
                        comparison = c1.getLastName().compareToIgnoreCase(c2.getLastName());
                        break;
                    case "email":
                        comparison = c1.getEmail().compareToIgnoreCase(c2.getEmail());
                        break;
                    case "jobprofile":
                        comparison = c1.getJobProfile().compareTo(c2.getJobProfile());
                        break;
                    case "joblocation":
                        comparison = c1.getJobLocation().compareToIgnoreCase(c2.getJobLocation());
                        break;
                    case "status":
                        comparison = c1.getStatus().compareTo(c2.getStatus());
                        break;
                    case "updatedat":
                        comparison = c1.getUpdatedAt().compareTo(c2.getUpdatedAt());
                        break;
                    case "createdat":
                    default:
                        comparison = c1.getCreatedAt().compareTo(c2.getCreatedAt());
                        break;
                }
                
                return ascending ? comparison : -comparison;
            })
            .collect(Collectors.toList());
    }

    // Interview round management methods
    public CandidateDto addInterviewRound(String candidateId, Candidate.InterviewRound interviewRound) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + candidateId));

        candidate.getInterviewRounds().add(interviewRound);
        candidate.setUpdatedAt(LocalDateTime.now());

        Candidate updatedCandidate = candidateRepository.save(candidate);
        log.info("Added interview round for candidate: {} {}", candidate.getFirstName(), candidate.getLastName());

        return CandidateDto.fromCandidate(updatedCandidate);
    }

    public CandidateDto updateInterviewRoundStatus(String candidateId, String interviewId, 
                                                  Candidate.InterviewRound.InterviewStatus status) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + candidateId));

        candidate.getInterviewRounds().stream()
            .filter(round -> interviewId.equals(round.getInterviewId()))
            .findFirst()
            .ifPresentOrElse(
                round -> {
                    round.setStatus(status);
                    if (status == Candidate.InterviewRound.InterviewStatus.IN_PROGRESS) {
                        round.setStartedAt(LocalDateTime.now());
                        // Update candidate status when first interview starts
                        if (candidate.getStatus() == Candidate.CandidateStatus.TO_BE_STARTED) {
                            candidate.setStatus(Candidate.CandidateStatus.IN_PROGRESS);
                        }
                    } else if (status == Candidate.InterviewRound.InterviewStatus.SELECTED || 
                              status == Candidate.InterviewRound.InterviewStatus.REJECTED) {
                        round.setCompletedAt(LocalDateTime.now());
                        // Update candidate status based on final interview outcome
                        updateCandidateStatusBasedOnRounds(candidate);
                    }
                },
                () -> {
                    throw new RuntimeException("Interview round not found with id: " + interviewId);
                }
            );

        candidate.setUpdatedAt(LocalDateTime.now());
        Candidate updatedCandidate = candidateRepository.save(candidate);

        return CandidateDto.fromCandidate(updatedCandidate);
    }

    public CandidateDto updateInterviewRoundStatusByIndex(String candidateId, int roundIndex, 
                                                         Candidate.InterviewRound.InterviewStatus status) {
        Candidate candidate = candidateRepository.findById(candidateId)
            .orElseThrow(() -> new RuntimeException("Candidate not found with id: " + candidateId));

        if (roundIndex < 0 || roundIndex >= candidate.getInterviewRounds().size()) {
            throw new RuntimeException("Invalid round index: " + roundIndex);
        }

        Candidate.InterviewRound round = candidate.getInterviewRounds().get(roundIndex);
        round.setStatus(status);
        
        if (status == Candidate.InterviewRound.InterviewStatus.IN_PROGRESS) {
            round.setStartedAt(LocalDateTime.now());
            // Update candidate status when first interview starts
            if (candidate.getStatus() == Candidate.CandidateStatus.TO_BE_STARTED) {
                candidate.setStatus(Candidate.CandidateStatus.IN_PROGRESS);
            }
        } else if (status == Candidate.InterviewRound.InterviewStatus.SELECTED || 
                  status == Candidate.InterviewRound.InterviewStatus.REJECTED) {
            round.setCompletedAt(LocalDateTime.now());
            // Update candidate status based on final interview outcome
            updateCandidateStatusBasedOnRounds(candidate);
        }

        candidate.setUpdatedAt(LocalDateTime.now());
        Candidate updatedCandidate = candidateRepository.save(candidate);

        return CandidateDto.fromCandidate(updatedCandidate);
    }

    // Helper method to update candidate status based on interview rounds
    private void updateCandidateStatusBasedOnRounds(Candidate candidate) {
        List<Candidate.InterviewRound> rounds = candidate.getInterviewRounds();
        
        // Check if any round is selected
        boolean hasSelected = rounds.stream()
            .anyMatch(r -> r.getStatus() == Candidate.InterviewRound.InterviewStatus.SELECTED);
        
        // Check if all rounds are completed (either selected or rejected)
        boolean allCompleted = rounds.stream()
            .allMatch(r -> r.getStatus() == Candidate.InterviewRound.InterviewStatus.SELECTED ||
                          r.getStatus() == Candidate.InterviewRound.InterviewStatus.REJECTED);
        
        // Check if all rounds are rejected
        boolean allRejected = rounds.stream()
            .allMatch(r -> r.getStatus() == Candidate.InterviewRound.InterviewStatus.REJECTED);
        
        if (hasSelected && allCompleted) {
            // If at least one round is selected and all rounds are completed
            candidate.setStatus(Candidate.CandidateStatus.SELECTED);
        } else if (allRejected) {
            // If all rounds are rejected
            candidate.setStatus(Candidate.CandidateStatus.REJECTED);
        } else if (rounds.stream().anyMatch(r -> r.getStatus() == Candidate.InterviewRound.InterviewStatus.IN_PROGRESS)) {
            // If any round is still in progress
            candidate.setStatus(Candidate.CandidateStatus.IN_PROGRESS);
        }
        // Otherwise keep the current status
    }

    // Security method to check if user owns the candidate record
    public boolean isUserOwnCandidate(String userEmail, String candidateId) {
        Optional<Candidate> candidate = candidateRepository.findById(candidateId);
        return candidate.isPresent() && candidate.get().getEmail().equals(userEmail);
    }
}
