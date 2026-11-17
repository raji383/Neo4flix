package com.neo4flix.recommendation_service.dto;

public record MovieRecommendation(
        String movieId,
        String title,
        double hybridScore
) {}