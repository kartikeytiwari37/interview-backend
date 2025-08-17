package com.interview.app.repository;

import com.interview.app.model.Candidate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends MongoRepository<Candidate, String> {
    
    Optional<Candidate> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    // Removed individual search methods - now using unified search in service layer
}
