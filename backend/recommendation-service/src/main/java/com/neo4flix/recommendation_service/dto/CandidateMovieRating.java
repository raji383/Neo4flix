package com.neo4flix.recommendation_service.dto;

public record CandidateMovieRating(
        String movieId,
        String title,
        String userId,
        double rating
) {}