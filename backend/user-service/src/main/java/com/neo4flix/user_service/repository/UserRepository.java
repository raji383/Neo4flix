package com.neo4flix.user_service.repository;

import java.util.List;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.neo4flix.user_service.model.User;

public interface UserRepository extends Neo4jRepository<User, String> {
	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	List<User> findAllByUsername(String username);
}
