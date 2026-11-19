
package com.neo4flix.recommendation_service.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Comparator;

import org.springframework.stereotype.Service;

import com.neo4flix.recommendation_service.dto.*;
import com.neo4flix.recommendation_service.exceptions.ApiException;
import com.neo4flix.recommendation_service.repository.RecommendationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationService {

        private final RecommendationRepository recommendationRepository;
        private final SimilarityService similarityService;

        public List<MovieRecommendation> recommendMovies(String currentUserId) {

                // Step 1: Verify that the user exists
                if (!recommendationRepository.userExists(currentUserId)) {
                        throw ApiException.notFound("User not found: " + currentUserId);
                }

                // Step 2: Calculate collaborative filtering predictions
                var commonRatings = recommendationRepository.findCommonRatings(currentUserId);

                var similarUsers = similarityService.calculateSimilarityScores(commonRatings);

                var selectedUsers = similarUsers.stream()
                                .filter(user -> user.similarity() > 0)
                                .filter(user -> user.commonMovies() >= 3)
                                .toList();

                Map<String, Double> collaborativeScores = Map.of();

                if (!selectedUsers.isEmpty()) {

                        var similarUserIds = selectedUsers.stream()
                                        .map(user -> user.userId())
                                        .toList();

                        var candidateRatings = recommendationRepository.findCandidateMovies(
                                        currentUserId,
                                        similarUserIds);

                        Map<String, SimilarUser> similarUsersMap = selectedUsers.stream()
                                        .collect(Collectors.toMap(
                                                        user -> user.userId(),
                                                        user -> user));

                        Map<String, List<CandidateMovieRating>> groupedMovies = candidateRatings.stream()
                                        .collect(Collectors.groupingBy(
                                                        rating -> rating.movieId()));

                        collaborativeScores = groupedMovies.entrySet().stream()
                                        .collect(Collectors.toMap(
                                                        entry -> entry.getKey(),
                                                        entry -> calculatePredictedRating(
                                                                        entry.getValue(),
                                                                        similarUsersMap)));
                }

                // Step 3: Calculate genre preference scores
                var genreScores = recommendationRepository.findMovieGenreScores(currentUserId)
                                .stream()
                                .collect(Collectors.toMap(
                                               movieGenreScore -> movieGenreScore.movieId(),
                                                movieGenreScore -> movieGenreScore.genreScore()));

                // Step 4: Get all movies not rated by the current user
                var availableMovies = recommendationRepository.findAvailableMovies(currentUserId);

                // Step 5: Calculate the hybrid score for every available movie
                List<MovieRecommendation> recommendations = new ArrayList<>();

                for (AvailableMovie movie : availableMovies) {

                        Double collaborativeScore = collaborativeScores.get(movie.movieId());

                        Double genreScore = genreScores.get(movie.movieId());

                        double hybridScore = calculateHybridScore(
                                        collaborativeScore,
                                        genreScore,
                                        movie.averageRating());

                        recommendations.add(new MovieRecommendation(
                                        movie.movieId(),
                                        movie.title(),
                                        hybridScore,
                                        movie.director(),
                                        movie.description(),
                                        movie.releaseYear(),
                                        movie.genres(),
                                        movie.imageUrl(),
                                        movie.averageRating(),
                                        movie.ratingCount()));
                }

                // Step 6: Return the best 10 movies
                return recommendations.stream()
                                .sorted(Comparator.comparingDouble(
                                                (MovieRecommendation movie) -> movie.hybridScore()).reversed())
                                .limit(10)
                                .toList();
        }

        private double calculatePredictedRating(
                        List<CandidateMovieRating> ratings,
                        Map<String, SimilarUser> similarUsers) {

                double weightedSum = 0;
                double similaritySum = 0;

                for (CandidateMovieRating rating : ratings) {

                        SimilarUser user = similarUsers.get(rating.userId());

                        weightedSum += user.similarity() * rating.rating();
                        similaritySum += user.similarity();
                }

                if (similaritySum == 0) {
                        return 0;
                }

                return weightedSum / similaritySum;
        }

        private double calculateHybridScore(
                        Double collaborativeRating,
                        Double genreScore,
                        double averageRating) {

                double weightedSum = 0;
                double availableWeight = 0;

                // S: Collaborative Filtering (0.45)
                if (collaborativeRating != null) {
                        weightedSum += 0.45 * (collaborativeRating / 5.0);
                        availableWeight += 0.45;
                }

                // G: Genre Preference (0.35)
                if (genreScore != null) {
                        weightedSum += 0.35 * genreScore;
                        availableWeight += 0.35;
                }

                // R: Global Average Rating (0.20)
                weightedSum += 0.20 * (averageRating / 5.0);
                availableWeight += 0.20;

                if (availableWeight == 0) {
                        return 0;
                }

                // Normalize the available weights
                return weightedSum / availableWeight;
        }
}
