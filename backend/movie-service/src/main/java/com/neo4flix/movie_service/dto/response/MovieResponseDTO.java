package com.neo4flix.movie_service.dto.response;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)

public record MovieResponseDTO(
    String id,
    String title,
    String director,
    String description,
    int releaseYear,
    Set<String> genres,
    String imageUrl,
    Double averageRating,
    Long ratingCount,
    Double tmdbVoteAverage,
    Long tmdbVoteCount
) {}