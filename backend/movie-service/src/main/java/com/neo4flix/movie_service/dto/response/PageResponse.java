package com.neo4flix.movie_service.dto.response;


import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size,
                              long totalElements, int totalPages) {}