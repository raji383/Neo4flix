package com.neo4flix.user_service.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.neo4flix.user_service.model.EmailVerificationChallenge;

public interface EmailVerificationRepository
        extends Neo4jRepository<EmailVerificationChallenge, String> {

    List<EmailVerificationChallenge> findAllByUserIdAndConsumedFalse(String userId);
}