package com.neo4flix.recommendation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SimilarityServiceTest {

    private final SimilarityService similarityService = new SimilarityService();

    @Test
    void calculatePearsonReturnsPositiveCorrelationForMatchingRatings() {
        var similarity = similarityService.calculatePearson(List.of(
                new SimilarityService.RatingPair(1, 2),
                new SimilarityService.RatingPair(2, 4),
                new SimilarityService.RatingPair(3, 6)));

        assertTrue(similarity.isPresent());
        assertEquals(1.0, similarity.getAsDouble(), 0.0000001);
    }

    @Test
    void calculatePearsonIsEmptyWhenRatingsCannotDefineCorrelation() {
        var similarity = similarityService.calculatePearson(List.of(
                new SimilarityService.RatingPair(3, 1),
                new SimilarityService.RatingPair(3, 4)));

        assertFalse(similarity.isPresent());
    }
}
