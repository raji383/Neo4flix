package com.neo4flix.movie_service.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/add")
    public ResponseEntity<MovieResponseDTO> addMovie(@RequestParam @Valid CreateMovieDTO request , Principal principal) {
        MovieResponseDTO movieResponseDTO = movieService.addMovie(request, principal.getName());
        return ResponseEntity.status(201).body(movieResponseDTO);
    }
    // @GetMapping()
    // public ResponseEntity<PageResponse<MovieResponseDTO>> getMovies() {

    //     return ResponseEntity.ok("Hello from Movie Service");
    // }
}