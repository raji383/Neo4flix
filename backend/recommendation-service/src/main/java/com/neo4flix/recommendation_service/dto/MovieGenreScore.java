package com.neo4flix.recommendation_service.dto;

public record MovieGenreScore(
        String movieId,
        String title,
        double genreScore
) {}