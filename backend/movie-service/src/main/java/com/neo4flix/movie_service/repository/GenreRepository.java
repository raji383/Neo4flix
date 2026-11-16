package com.neo4flix.movie_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.neo4flix.movie_service.model.Genre;

public interface GenreRepository extends Neo4jRepository<Genre, String> {
}