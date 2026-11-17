package com.neo4flix.movie_service.dto.response;
public record RatingSummary(
    String movieId,
    Double average,
    Long count
) {}