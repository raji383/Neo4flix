package com.neo4flix.movie_service.dto.response;

import java.util.Set;

public record MovieResponseDTO(
        String id,
        String title,
        String director,
        String description,
        int releaseYear,
        Set<String> genres,
        String imageUrl) {
}