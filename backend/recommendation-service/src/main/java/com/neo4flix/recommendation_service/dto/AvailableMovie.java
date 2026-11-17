package com.neo4flix.recommendation_service.dto;

import java.util.Set;

public record AvailableMovie(
        String movieId,
        String title,
        String director,
        String description,
        int releaseYear,
        Set<String> genres,
        String imageUrl,
        double averageRating,
        Long ratingCount
) {}