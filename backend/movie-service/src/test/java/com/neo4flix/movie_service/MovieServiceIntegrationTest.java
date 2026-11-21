package com.neo4flix.movie_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import com.neo4flix.movie_service.config.TmdbRatingCleanup;
import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.dto.response.MovieResponseDTO;
import com.neo4flix.movie_service.dto.response.PageResponse;
import com.neo4flix.movie_service.repository.GenreRepository;
import com.neo4flix.movie_service.service.MovieService;
import com.neo4flix.movie_service.model.Genre;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@SpringBootTest(properties = {
        "MOVIE_SERVICE_PORT=0",
        "eureka.client.enabled=false",
        "cloudinary.cloud-name=test-cloud",
        "cloudinary.api-key=test-api-key",
        "cloudinary.api-secret=test-api-secret",
        "spring.neo4j.authentication.username=test",
        "spring.neo4j.authentication.password=test"
})
@AutoConfigureMockMvc
class MovieServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreRepository genreRepository;

    @MockitoBean
    private TmdbRatingCleanup tmdbRatingCleanup;

    @Test
    void listMoviesEndpointReturnsPagedResultsAndPassesFilters() throws Exception {
        MovieResponseDTO movie = movieResponse("movie-123", "Example Film");
        when(movieService.getMovies(1, 2, "Film", List.of("Drama"), 2020))
                .thenReturn(new PageResponse<>(List.of(movie), 1, 2, 5, 3));

        mockMvc.perform(get("/api/movies")
                        .param("page", "1")
                        .param("size", "2")
                        .param("q", "Film")
                        .param("genre", "Drama")
                        .param("releaseYear", "2020")
                        .header("X-Authenticated-User-Id", "user-123")
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content[0].title").value("Example Film"));
        verify(movieService).getMovies(1, 2, "Film", List.of("Drama"), 2020);
    }

    @Test
    void movieDetailsEndpointReturnsMovieForAuthenticatedGatewayRequest() throws Exception {
        when(movieService.getById("movie-123")).thenReturn(movieResponse("movie-123", "Example Film"));

        mockMvc.perform(get("/api/movies/movie-123")
                        .header("X-Authenticated-User-Id", "user-123")
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("movie-123"))
                .andExpect(jsonPath("$.title").value("Example Film"))
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.ratingCount").value(2));
    }

    @Test
    void addMovieEndpointBindsRequestAndReturnsCreatedMovie() throws Exception {
        MockMultipartFile poster = poster();
        MovieResponseDTO response = movieResponse("movie-123", "Example Film");
        when(movieService.addMovie(any(CreateMovieDTO.class), eq("admin-123"))).thenReturn(response);

        mockMvc.perform(movieRequest("/api/movies/add", "POST", poster)
                        .header("X-Authenticated-User-Id", "admin-123")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("movie-123"))
                .andExpect(jsonPath("$.title").value("Example Film"));

        verify(movieService).addMovie(any(CreateMovieDTO.class), eq("admin-123"));
    }

    @Test
    void updateMovieEndpointBindsRequestAndReturnsUpdatedMovie() throws Exception {
        MovieResponseDTO response = movieResponse("movie-123", "Updated Film");
        when(movieService.updateMovie(eq("movie-123"), any(CreateMovieDTO.class), eq("admin-123")))
                .thenReturn(response);

        mockMvc.perform(movieRequest("/api/movies/movie-123", "PUT", poster())
                        .header("X-Authenticated-User-Id", "admin-123")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Film"));

        verify(movieService).updateMovie(eq("movie-123"), any(CreateMovieDTO.class), eq("admin-123"));
    }

    @Test
    void deleteMovieEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/movies/movie-123")
                        .header("X-Authenticated-User-Id", "admin-123")
                        .header("X-Authenticated-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        verify(movieService).deleteMovie("movie-123", "admin-123");
    }

    @Test
    void popularMoviesEndpointReturnsPopularMovies() throws Exception {
        when(movieService.getPopular(5)).thenReturn(List.of(movieResponse("movie-123", "Example Film")));

        mockMvc.perform(get("/api/movies/popular")
                        .param("limit", "5")
                        .header("X-Authenticated-User-Id", "user-123")
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("movie-123"));
    }

    @Test
    void genresEndpointReturnsSortedGenreNames() throws Exception {
        when(genreRepository.findAll()).thenReturn(List.of(new Genre("Drama"), new Genre("Action")));

        mockMvc.perform(get("/api/genres")
                        .header("X-Authenticated-User-Id", "user-123")
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Action"))
                .andExpect(jsonPath("$[1]").value("Drama"));
    }

    @Test
    void movieDetailsEndpointRejectsRequestWithoutGatewayIdentity() throws Exception {
        mockMvc.perform(get("/api/movies/movie-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void writeEndpointRejectsNonAdminRole() throws Exception {
        mockMvc.perform(delete("/api/movies/movie-123")
                        .header("X-Authenticated-User-Id", "user-123")
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MovieResponseDTO movieResponse(String id, String title) {
        return new MovieResponseDTO(
                id,
                title,
                "Example Director",
                "A sample movie.",
                2020,
                Set.of("Drama"),
                "https://example.test/poster.png",
                4.5,
                2L);
    }

    private MockMultipartFile poster() {
        return new MockMultipartFile("file", "poster.png", "image/png", new byte[] {1});
    }

    private MockMultipartHttpServletRequestBuilder movieRequest(
            String uri, String method, MockMultipartFile poster) {
        var request = multipart(uri)
                .file(poster)
                .param("title", "Example Film")
                .param("director", "Example Director")
                .param("description", "A sample movie.")
                .param("releaseYear", "2020")
                .param("genres", "Drama");
        return request.with(servletRequest -> {
            servletRequest.setMethod(method);
            return servletRequest;
        });
    }
}
