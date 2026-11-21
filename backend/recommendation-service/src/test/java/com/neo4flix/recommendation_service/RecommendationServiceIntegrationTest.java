package com.neo4flix.recommendation_service;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import com.neo4flix.recommendation_service.dto.MovieRecommendation;
import com.neo4flix.recommendation_service.exceptions.ApiException;
import com.neo4flix.recommendation_service.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "RECOMMENDATION_SERVICE_PORT=0",
        "eureka.client.enabled=false",
        "spring.neo4j.authentication.username=test",
        "spring.neo4j.authentication.password=test"
})
@AutoConfigureMockMvc
class RecommendationServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @Test
    void recommendationEndpointReturnsRecommendationsForAuthenticatedUser() throws Exception {
        when(recommendationService.recommendMovies("user-123")).thenReturn(List.of(
                new MovieRecommendation(
                        "movie-123",
                        "Example Film",
                        0.92,
                        "Example Director",
                        "A sample movie.",
                        2020,
                        Set.of("Drama"),
                        "https://example.test/poster.png",
                        4.5,
                        12L)));

        mockMvc.perform(get("/api/recommendations")
                        .header("X-Authenticated-User-Id", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movieId").value("movie-123"))
                .andExpect(jsonPath("$[0].title").value("Example Film"))
                .andExpect(jsonPath("$[0].hybridScore").value(0.92));
    }

    @Test
    void recommendationEndpointReturnsNotFoundForUnknownUser() throws Exception {
        when(recommendationService.recommendMovies("missing-user"))
                .thenThrow(ApiException.notFound("User not found: missing-user"));

        mockMvc.perform(get("/api/recommendations")
                        .header("X-Authenticated-User-Id", "missing-user"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
