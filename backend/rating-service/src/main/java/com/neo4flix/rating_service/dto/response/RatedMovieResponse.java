package com.neo4flix.rating_service.dto.response;

import java.util.Set;

public record RatedMovieResponse(
    String id,
    String title,
    String director,
    String description,
    int releaseYear,
    Set<String> genres,
    String imageUrl,
    Double averageRating,
    Long ratingCount
) {}