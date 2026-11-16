package com.neo4flix.movie_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.neo4flix.movie_service.model.Movie;

public interface MovieRepository extends Neo4jRepository<Movie, String> {
}