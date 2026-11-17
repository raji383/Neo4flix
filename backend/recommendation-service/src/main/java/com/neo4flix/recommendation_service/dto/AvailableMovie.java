package com.neo4flix.recommendation_service.dto;

public record AvailableMovie(
        String movieId,
        String title,
        double averageRating
) {}