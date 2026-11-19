package com.neo4flix.recommendation_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neo4flix.recommendation_service.dto.RatingComparison;
import com.neo4flix.recommendation_service.dto.SimilarUser;

@Service
public class SimilarityService {

    public record RatingPair(
            double currentRating,
            double otherRating) {
    }

    public OptionalDouble calculatePearson(
            List<RatingPair> ratings) {

        if (ratings == null || ratings.size() < 2) {
            return OptionalDouble.empty();
        }

        double currentAverage = ratings.stream()
                .mapToDouble(r->r.currentRating())
                .average()
                .orElse(0);

        double otherAverage = ratings.stream()
                .mapToDouble(r->r.otherRating())
                .average()
                .orElse(0);

        double numerator = 0;
        double currentSquared = 0;
        double otherSquared = 0;

        for (RatingPair pair : ratings) {

            double currentDiff = pair.currentRating() - currentAverage;

            double otherDiff = pair.otherRating() - otherAverage;

            numerator += currentDiff * otherDiff;

            currentSquared += currentDiff * currentDiff;

            otherSquared += otherDiff * otherDiff;
        }

        double denominator = Math.sqrt(currentSquared * otherSquared);

        if (denominator == 0) {
            return OptionalDouble.empty();
        }

        return OptionalDouble.of(numerator / denominator);
    }

    public List<SimilarUser> calculateSimilarityScores(
            List<RatingComparison> comparisons) {

        Map<String, List<RatingComparison>> groupedRatings = comparisons.stream()
                .collect(Collectors.groupingBy(r -> r.userId()));

        List<SimilarUser> similarUsers = new ArrayList<>();

        for (var entry : groupedRatings.entrySet()) {

            List<RatingComparison> userRatings = entry.getValue();

            List<RatingPair> pairs = userRatings.stream()
                    .map(r -> new RatingPair(
                            r.currentRating(),
                            r.otherRating()))
                    .toList();

            OptionalDouble similarity = calculatePearson(pairs);

            if (similarity.isEmpty()) {
                continue;
            }

            RatingComparison first = userRatings.get(0);

            similarUsers.add(new SimilarUser(
                    first.userId(),
                    first.username(),
                    userRatings.size(),
                    similarity.getAsDouble()));
        }

        return similarUsers;
    }
}
