package com.neo4flix.recommendation_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.neo4flix.recommendation_service.dto.AvailableMovie;
import com.neo4flix.recommendation_service.dto.CandidateMovieRating;
import com.neo4flix.recommendation_service.dto.MovieGenreScore;
import com.neo4flix.recommendation_service.dto.RatingComparison;
import com.neo4flix.recommendation_service.exceptions.ApiException;
import com.neo4flix.recommendation_service.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                recommendationRepository,
                new SimilarityService());
    }

    @Test
    void recommendMoviesCombinesScoresAndRanksAvailableMovies() {
        List<RatingComparison> commonRatings = List.of(
                comparison("similar-user", "rated-1", 1, 1),
                comparison("similar-user", "rated-2", 2, 3),
                comparison("similar-user", "rated-3", 3, 5),
                comparison("weak-user", "rated-1", 1, 2),
                comparison("weak-user", "rated-2", 5, 4));
        when(recommendationRepository.userExists("current-user")).thenReturn(true);
        when(recommendationRepository.findCommonRatings("current-user")).thenReturn(commonRatings);
        when(recommendationRepository.findCandidateMovies("current-user", List.of("similar-user")))
                .thenReturn(List.of(
                        new CandidateMovieRating("movie-a", "Movie A", "similar-user", 5)));
        when(recommendationRepository.findMovieGenreScores("current-user")).thenReturn(List.of(
                new MovieGenreScore("movie-a", "Movie A", 0.8),
                new MovieGenreScore("movie-b", "Movie B", 0.9)));
        when(recommendationRepository.findAvailableMovies("current-user")).thenReturn(List.of(
                availableMovie("movie-a", "Movie A", 4.0),
                availableMovie("movie-b", "Movie B", 5.0)));

        var recommendations = recommendationService.recommendMovies("current-user");

        assertEquals("movie-b", recommendations.get(0).movieId());
        assertEquals(0.9363636363, recommendations.get(0).hybridScore(), 0.0000001);
        assertEquals("movie-a", recommendations.get(1).movieId());
        assertEquals(0.89, recommendations.get(1).hybridScore(), 0.0000001);
        verify(recommendationRepository).findCandidateMovies("current-user", List.of("similar-user"));
    }

    @Test
    void recommendMoviesRejectsUnknownUserBeforeLoadingRatings() {
        when(recommendationRepository.userExists("missing-user")).thenReturn(false);

        ApiException exception = assertThrows(
                ApiException.class,
                () -> recommendationService.recommendMovies("missing-user"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(recommendationRepository, never()).findCommonRatings("missing-user");
    }

    private RatingComparison comparison(
            String userId, String movieId, double currentRating, double otherRating) {
        return new RatingComparison(userId, userId, movieId, currentRating, otherRating);
    }

    private AvailableMovie availableMovie(String id, String title, double averageRating) {
        return new AvailableMovie(
                id,
                title,
                "Example Director",
                "A sample movie.",
                2020,
                Set.of("Drama"),
                "https://example.test/poster.png",
                averageRating,
                10L);
    }
}
