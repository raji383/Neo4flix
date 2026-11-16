package com.neo4flix.movie_service.dto.request;

import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record CreateMovieDTO(
                @NotBlank @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters") String title,
                @NotBlank @Size(min = 2, max = 100, message = "Director must be between 2 and 100 characters") String director,
                @NotBlank @Size(max = 250, message = "Description must be at most 250 characters") String description,
                @Min(1888) @Max(2100) int releaseYear,
                @NotEmpty Set<String> genres,
                MultipartFile file

) {
}