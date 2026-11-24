package com.neo4flix.movie_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
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
import com.neo4flix.movie_service.service.MediaUploadService.UploadResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MediaUploadService mediaUploadService;

    private MovieService movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieService(movieRepository, genreRepository, mediaUploadService);
    }

    @Test
    void addMovieSavesMovieWithResolvedGenresAndUploadedImage() throws Exception {
        MockMultipartFile poster = new MockMultipartFile(
                "file",
                "poster.png",
                "image/png",
                new byte[] {1});
        CreateMovieDTO request = new CreateMovieDTO(
                "Example Film",
                "Example Director",
                "A sample movie.",
                2020,
                Set.of("Drama"),
                poster);
        when(movieRepository.existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                "Example Film", 2020, "Example Director", "user-123"))
                .thenReturn(false);
        when(genreRepository.findAllById(Set.of("Drama")))
                .thenReturn(List.of(new Genre("Drama")));
        when(mediaUploadService.uploadFile(poster))
                .thenReturn(new UploadResult("https://example.test/poster.png", "poster"));
        when(movieRepository.save(any(Movie.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = movieService.addMovie(request, "user-123");

        ArgumentCaptor<Movie> movieCaptor = ArgumentCaptor.forClass(Movie.class);
        verify(movieRepository).save(movieCaptor.capture());
        Movie savedMovie = movieCaptor.getValue();
        assertEquals("Example Film", response.title());
        assertEquals("user-123", savedMovie.getCreatedBy());
        assertEquals(Set.of("Drama"), response.genres());
        assertEquals("https://example.test/poster.png", savedMovie.getImageUrl());
        verify(mediaUploadService).uploadFile(poster);
    }

    @Test
    void updateMovieChangesFieldsAndPreservesImageWhenNoNewFileIsProvided() throws Exception {
        Movie existing = Movie.builder()
                .id("movie-123")
                .title("Old Film")
                .director("Old Director")
                .releaseYear(2010)
                .createdBy("user-123")
                .imageUrl("https://example.test/old-poster.png")
                .build();
        when(movieRepository.findById("movie-123")).thenReturn(Optional.of(existing));
        when(movieRepository
                .existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedByAndIdNot(
                        "Updated Film", 2021, "Updated Director", "user-123", "movie-123"))
                .thenReturn(false);
        when(genreRepository.findAllById(Set.of("Drama")))
                .thenReturn(List.of(new Genre("Drama")));
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateMovieDTO request = new CreateMovieDTO(
                "Updated Film",
                "Updated Director",
                "Updated description.",
                2021,
                Set.of("Drama"),
                null);

        var response = movieService.updateMovie("movie-123", request, "user-123");

        assertEquals("Updated Film", response.title());
        assertEquals("https://example.test/old-poster.png", existing.getImageUrl());
        assertEquals(Set.of("Drama"), response.genres());
        verify(movieRepository).save(existing);
        verify(mediaUploadService, never()).uploadFile(any());
    }

    @Test
    void deleteMovieDeletesTheFoundMovie() {
        Movie existing = Movie.builder()
                .id("movie-123")
                .createdBy("user-123")
                .build();
        when(movieRepository.findById("movie-123")).thenReturn(Optional.of(existing));

        movieService.deleteMovie("movie-123", "user-123");

        verify(movieRepository).delete(existing);
    }

    @Test
    void getMoviesTrimsSearchFiltersAndReturnsPageMetadata() {
        Movie movie = Movie.builder().id("movie-123").title("Example Film").build();
        PageRequest pageRequest = PageRequest.of(0, 10);
        when(movieRepository.search("Film", List.of("Drama"), 2020, pageRequest))
                .thenReturn(new PageImpl<>(List.of(movie), pageRequest, 1));
        when(movieRepository.summaries(List.of("movie-123"))).thenReturn(
                Map.of("movie-123", new RatingSummary("movie-123", 4.25, 3L)));

        var response = movieService.getMovies(
                0, 10, " Film ", Arrays.asList(" Drama ", " "), 2020);

        assertEquals(1, response.totalElements());
        assertEquals("Example Film", response.content().get(0).title());
        assertEquals(4.3, response.content().get(0).averageRating());
    }

    @Test
    void getMovieReturnsDetailsWithRoundedRating() {
        Movie movie = Movie.builder()
                .id("movie-123")
                .title("Example Film")
                .genres(Set.of(new Genre("Drama")))
                .build();
        when(movieRepository.findById("movie-123")).thenReturn(Optional.of(movie));
        when(movieRepository.summaries(List.of("movie-123"))).thenReturn(
                Map.of("movie-123", new RatingSummary("movie-123", 4.25, 3L)));

        var response = movieService.getById("movie-123");

        assertEquals("Example Film", response.title());
        assertEquals(Set.of("Drama"), response.genres());
        assertEquals(4.3, response.averageRating());
        assertEquals(3L, response.ratingCount());
    }

    @Test
    void getMovieReturnsNotFoundWhenIdDoesNotExist() {
        when(movieRepository.findById("missing")).thenReturn(Optional.empty());

        var exception = assertThrows(
                com.neo4flix.movie_service.exceptions.ApiException.class,
                () -> movieService.getById("missing"));

        assertEquals(404, exception.getStatus().value());
    }
}
