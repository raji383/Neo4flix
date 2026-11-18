package com.neo4flix.movie_service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.neo4flix.movie_service.config.tmdb.TmdbClient;
import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.service.MovieService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class MovieDataSeederTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void seedsSupportedTmdbGenresAsConfiguredOwner() throws Exception {
        TmdbClient tmdbClient = mock(TmdbClient.class);
        MovieService movieService = mock(MovieService.class);
        when(tmdbClient.discoverMovies(1)).thenReturn(objectMapper.readTree("""
                {"results":[{"id":42,"title":"Example Film"}]}
                """));
        when(tmdbClient.getMovieDetails(42)).thenReturn(objectMapper.readTree("""
                {
                  "title":"Example Film",
                  "overview":"A sufficiently long overview.",
                  "release_date":"2020-01-01",
                  "poster_path":"/poster.jpg",
                  "genres":[
                    {"id":878,"name":"Science Fiction"},
                    {"id":18,"name":"Drama"},
                    {"id":10751,"name":"Family"}
                  ],
                  "credits":{"crew":[{"job":"Director","name":"Example Director"}]}
                }
                """));
        when(tmdbClient.downloadPoster("/poster.jpg", 42)).thenReturn(
                new TmdbClient.DownloadedPoster(
                        "tmdb-42.jpg",
                        "image/jpeg",
                        new byte[] {1}
                )
        );

        MovieDataSeeder seeder = new MovieDataSeeder(tmdbClient, movieService);
        ReflectionTestUtils.setField(seeder, "targetCount", 1);
        ReflectionTestUtils.setField(seeder, "owner", "admin-user-id");

        seeder.run(null);

        ArgumentCaptor<CreateMovieDTO> movieCaptor =
                ArgumentCaptor.forClass(CreateMovieDTO.class);
        verify(movieService).addMovie(movieCaptor.capture(), eq("admin-user-id"));
        assertEquals(Set.of("Sci-Fi", "Drama"), movieCaptor.getValue().genres());
    }
}
