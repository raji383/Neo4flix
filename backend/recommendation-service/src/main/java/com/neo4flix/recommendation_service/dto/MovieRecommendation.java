package com.neo4flix.recommendation_service.dto;

import java.util.Set;

public record MovieRecommendation(
                String movieId,
                String title,
                double hybridScore,
                String director,
                String description,
                int releaseYear,
                Set<String> genres,
                String imageUrl,
                Double averageRating,
                Long ratingCount) {
}