package com.neo4flix.movie_service.dto.response;

public record ErrorResponse(String code, String message, Object details) {
}