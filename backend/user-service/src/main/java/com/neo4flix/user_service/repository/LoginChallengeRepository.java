
package com.neo4flix.user_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.neo4flix.user_service.model.LoginChallenge;

public interface LoginChallengeRepository
        extends Neo4jRepository<LoginChallenge, String> {
}
