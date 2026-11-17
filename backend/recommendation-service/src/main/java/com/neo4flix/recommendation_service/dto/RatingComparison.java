
package com.neo4flix.recommendation_service.dto;


public record RatingComparison(
        String userId,
        String username,
        String movieId,
        double currentRating,
        double otherRating) {
}
