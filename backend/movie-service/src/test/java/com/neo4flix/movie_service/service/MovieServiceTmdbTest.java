package com.neo4flix.movie_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.dto.response.RatingSummary;
import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.model.Movie;
import com.neo4flix.movie_service.repository.GenreRepository;
import com.neo4flix.movie_service.repository.MovieRepository;
import org.junit.jupiter.api.Test;

class MovieServiceTmdbTest {

    @Test
    void popularMoviesUsesTmdbPopularityWhenSeededMoviesExist() {
        MovieRepository movieRepository = mock(MovieRepository.class);
        GenreRepository genreRepository = mock(GenreRepository.class);
        MediaUploadService mediaUploadService = mock(MediaUploadService.class);
        MovieService movieService = new MovieService(
                movieRepository,
                genreRepository,
                mediaUploadService
        );
        Movie tmdbMovie = Movie.builder()
                .id("tmdb-movie")
                .title("Example Film")
                .tmdbPopularity(42.5)
                .build();
        when(movieRepository.topTmdbPopular(8)).thenReturn(List.of(tmdbMovie));
        when(movieRepository.summaries(List.of("tmdb-movie"))).thenReturn(
                Map.of("tmdb-movie", new RatingSummary("tmdb-movie", null, 0L))
        );

        var popular = movieService.getPopular(8);

        assertEquals(1, popular.size());
        assertEquals("tmdb-movie", popular.get(0).id());
        verify(movieRepository, never()).topRated(3, 8);
    }

    @Test
    void updatesExistingSeededMovieWithTmdbAggregate() throws Exception {
        MovieRepository movieRepository = mock(MovieRepository.class);
        GenreRepository genreRepository = mock(GenreRepository.class);
        MediaUploadService mediaUploadService = mock(MediaUploadService.class);
        MovieService movieService = new MovieService(
                movieRepository,
                genreRepository,
                mediaUploadService
        );

        Movie existing = Movie.builder()
                .id("movie-id")
                .title("Example Film")
                .director("Example Director")
                .releaseYear(2020)
                .createdBy("admin-id")
                .imageUrl("https://example.test/poster.jpg")
                .tmdbPopularity(42.5)
                .build();
        when(movieRepository.findFirstByTmdbId(42L)).thenReturn(Optional.empty());
        when(movieRepository
                .findFirstByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                        "Example Film",
                        2020,
                        "Example Director",
                        "admin-id"))
                .thenReturn(Optional.of(existing));
        when(movieRepository.existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                "Example Film",
                2020,
                "Example Director",
                "admin-id"))
                .thenReturn(true);
        when(movieRepository
                .existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedByAndIdNot(
                        "Example Film",
                        2020,
                        "Example Director",
                        "admin-id",
                        "movie-id"))
                .thenReturn(false);
        when(genreRepository.findAllById(Set.of("Drama")))
                .thenReturn(List.of(new Genre("Drama")));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateMovieDTO request = new CreateMovieDTO(
                "Example Film",
                "Example Director",
                "An example description.",
                2020,
                Set.of("Drama"),
                null
        );

        var response = movieService.addOrUpdateTmdbMovie(
                request,
                "admin-id",
                42,
                42.5
        );

        assertEquals(42L, existing.getTmdbId());
        assertEquals(42.5, existing.getTmdbPopularity());
        assertNull(response.averageRating());
        verify(mediaUploadService, never()).uploadFile(any());
        verify(movieRepository)
                .existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedByAndIdNot(
                        "Example Film",
                        2020,
                        "Example Director",
                        "admin-id",
                        "movie-id");
        verify(movieRepository).save(existing);
    }
}
