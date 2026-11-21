package com.neo4flix.rating_service.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import com.neo4flix.rating_service.dto.response.RatedMovieResponse;
import com.neo4flix.rating_service.exceptions.ApiException;
import com.neo4flix.rating_service.repository.RatingQueryRepository;
import com.neo4flix.rating_service.repository.RatingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RatingServiceImplTest {

    @Mock
    private RatingRepository ratingRepository;

    @Mock
    private RatingQueryRepository ratingQueryRepository;

    private RatingServiceImpl ratingService;

    @BeforeEach
    void setUp() {
        ratingService = new RatingServiceImpl(ratingRepository, ratingQueryRepository);
    }

    @Test
    void createRatingStoresValidRating() {
        when(ratingRepository.existsRating("user-123", "movie-123")).thenReturn(false);
        when(ratingRepository.createRating("user-123", "movie-123", 4)).thenReturn(1L);

        ratingService.createRating("user-123", "movie-123", 4);

        verify(ratingRepository).createRating("user-123", "movie-123", 4);
    }

    @Test
    void createRatingRejectsValuesOutsideOneToFive() {
        ApiException exception = assertThrows(
                ApiException.class,
                () -> ratingService.createRating("user-123", "movie-123", 6));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    @Test
    void updateRatingChangesExistingRating() {
        when(ratingRepository.updateRating("user-123", "movie-123", 5)).thenReturn(1L);

        ratingService.updateRating("user-123", "movie-123", 5);

        verify(ratingRepository).updateRating("user-123", "movie-123", 5);
    }

    @Test
    void deleteRatingRemovesExistingRating() {
        when(ratingRepository.deleteRating("user-123", "movie-123")).thenReturn(1L);

        ratingService.deleteRating("user-123", "movie-123");

        verify(ratingRepository).deleteRating("user-123", "movie-123");
    }

    @Test
    void getMyRatingReturnsValueAndUsesZeroWhenNoRatingExists() {
        when(ratingRepository.getMyRating("user-123", "movie-123"))
                .thenReturn(4)
                .thenReturn(null);

        assertEquals(4, ratingService.getMyRating("user-123", "movie-123"));
        assertEquals(0, ratingService.getMyRating("user-123", "movie-123"));
    }

    @Test
    void getMoviesRatedByUserReturnsQueryResults() {
        List<RatedMovieResponse> movies = List.of(
                new RatedMovieResponse(
                        "movie-123",
                        "Example Film",
                        "Example Director",
                        "A sample movie.",
                        2020,
                        Set.of("Drama"),
                        null,
                        4.5,
                        12L,
                        5));
        when(ratingQueryRepository.findMoviesRatedByUser("user-123")).thenReturn(movies);

        assertEquals(movies, ratingService.getMoviesRatedByUser("user-123"));
    }
}
