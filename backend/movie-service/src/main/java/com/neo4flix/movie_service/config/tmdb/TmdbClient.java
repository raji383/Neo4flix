
package com.neo4flix.movie_service.config.tmdb;

import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(
        prefix = "app.seed.movies",
        name = "enabled",
        havingValue = "true"
)
public class TmdbClient {

    private final RestClient apiClient;

    @Value("${tmdb.image-base-url}")
    private String imageBaseUrl;

    public TmdbClient(
            @Value("${tmdb.base-url}") String baseUrl,
            @Value("${tmdb.access-token}") String token
    ) {
        if (token.isBlank()) {
            throw new IllegalStateException(
                    "Configure TMDB_ACCESS_TOKEN when movie seeding is enabled"
            );
        }

        this.apiClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + token
                )
                .defaultHeader(
                        HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_JSON_VALUE
                )
                .build();
    }

    public JsonNode discoverMovies(int page) {
        return apiClient.get()
                .uri(
                    "/discover/movie?language=en-US"
                    + "&sort_by=popularity.desc"
                    + "&include_adult=false"
                    + "&page={page}",
                    page
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public JsonNode getMovieDetails(long movieId) {
        return apiClient.get()
                .uri(
                    "/movie/{id}?language=en-US&append_to_response=credits",
                    movieId
                )
                .retrieve()
                .body(JsonNode.class);
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            throw new IllegalArgumentException("Movie has no poster");
        }

        return imageBaseUrl.replaceAll("/+$", "")
                + "/"
                + posterPath.replaceAll("^/+", "");
    }
}
