package com.neo4flix.recommendation_service.dto;

public record SimilarUser(
        String userId,
        String username,
        int commonMovies,
        double similarity
) {}