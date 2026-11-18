package com.neo4flix.movie_service.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.context.annotation.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.dto.response.MovieResponseDTO;
import com.neo4flix.movie_service.dto.response.PageResponse;
import com.neo4flix.movie_service.service.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDTO> addMovie(
            @Valid @ModelAttribute CreateMovieDTO request,
            Principal principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(movieService.addMovie(request, principal.getName()));
    }

    @GetMapping()
    public ResponseEntity<PageResponse<MovieResponseDTO>> getMovies(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "genre", required = false) List<String> genre,
            @RequestParam(required = false) Integer releaseYear) {

        PageResponse<MovieResponseDTO> response = movieService.getMovies(page, size, q, genre, releaseYear);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDTO> getMovie(@PathVariable String id) {
        return ResponseEntity.ok(movieService.getById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MovieResponseDTO> updateMovie(
            @PathVariable String id,
            @Valid @ModelAttribute CreateMovieDTO request,
            Principal principal) {

        return ResponseEntity.ok(movieService.updateMovie(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMovie(@PathVariable String id, Principal principal) {
        movieService.deleteMovie(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<MovieResponseDTO>> getPopular(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(movieService.getPopular(limit));
    }
}
