package com.neo4flix.rating_service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import com.neo4flix.rating_service.dto.response.RatedMovieResponse;
import com.neo4flix.rating_service.dto.response.WatchlistItemDTO;
import com.neo4flix.rating_service.service.RatingService;
import com.neo4flix.rating_service.service.WatchlistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "RATING_SERVICE_PORT=0",
        "eureka.client.enabled=false",
        "spring.neo4j.authentication.username=test",
        "spring.neo4j.authentication.password=test"
})
@AutoConfigureMockMvc
class RatingServiceIntegrationTest {

    private static final String USER_ID = "user-123";
    private static final String MOVIE_ID = "movie-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RatingService ratingService;

    @MockitoBean
    private WatchlistService watchlistService;

    @Test
    void createRatingEndpointReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/ratings/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":4}"))
                .andExpect(status().isCreated());

        verify(ratingService).createRating(USER_ID, MOVIE_ID, 4);
    }

    @Test
    void updateRatingEndpointReturnsOk() throws Exception {
        mockMvc.perform(put("/api/ratings/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":5}"))
                .andExpect(status().isOk());

        verify(ratingService).updateRating(USER_ID, MOVIE_ID, 5);
    }

    @Test
    void deleteRatingEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/ratings/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(ratingService).deleteRating(USER_ID, MOVIE_ID);
    }

    @Test
    void getMyRatingEndpointReturnsRatingValue() throws Exception {
        when(ratingService.getMyRating(USER_ID, MOVIE_ID)).thenReturn(4);

        mockMvc.perform(get("/api/ratings/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));
    }

    @Test
    void getMyRatedMoviesEndpointReturnsRatedMovies() throws Exception {
        when(ratingService.getMoviesRatedByUser(USER_ID)).thenReturn(List.of(
                new RatedMovieResponse(
                        MOVIE_ID,
                        "Example Film",
                        "Example Director",
                        "A sample movie.",
                        2020,
                        Set.of("Drama"),
                        "https://example.test/poster.png",
                        4.5,
                        12L,
                        5)));

        mockMvc.perform(get("/api/ratings/me")
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MOVIE_ID))
                .andExpect(jsonPath("$[0].myRating").value(5));
    }

    @Test
    void addToWatchlistEndpointReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/watchlist/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isCreated());

        verify(watchlistService).add(USER_ID, MOVIE_ID);
    }

    @Test
    void removeFromWatchlistEndpointReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/watchlist/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(watchlistService).remove(USER_ID, MOVIE_ID);
    }

    @Test
    void listWatchlistEndpointReturnsItems() throws Exception {
        when(watchlistService.list(USER_ID)).thenReturn(List.of(
                new WatchlistItemDTO(
                        MOVIE_ID,
                        "Example Film",
                        "https://example.test/poster.png",
                        2020,
                        ZonedDateTime.parse("2026-01-01T10:00:00Z"))));

        mockMvc.perform(get("/api/watchlist")
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(MOVIE_ID))
                .andExpect(jsonPath("$[0].title").value("Example Film"));
    }

    @Test
    void containsWatchlistEndpointReturnsMembership() throws Exception {
        when(watchlistService.contains(USER_ID, MOVIE_ID)).thenReturn(true);

        mockMvc.perform(get("/api/watchlist/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inWatchlist").value(true));
    }

    @Test
    void ratingEndpointRejectsOutOfRangeValue() throws Exception {
        mockMvc.perform(post("/api/ratings/{movieId}", MOVIE_ID)
                        .header("X-Authenticated-User-Id", USER_ID)
                        .header("X-Authenticated-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void endpointsRejectRequestsWithoutGatewayIdentity() throws Exception {
        mockMvc.perform(get("/api/watchlist"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
