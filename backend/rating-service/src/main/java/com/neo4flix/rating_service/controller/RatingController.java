package com.neo4flix.rating_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.neo4flix.rating_service.dto.request.RatingRequest;
import com.neo4flix.rating_service.repository.RatedMovieProjection;
import com.neo4flix.rating_service.service.RatingService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/{movieId}")
    public ResponseEntity<Void> createRating(
            @RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId,
            @Valid @RequestBody RatingRequest request) {

        ratingService.createRating(userId, movieId, request.value());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{movieId}")
    public ResponseEntity<Void> updateRating(
            @RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId,
            @Valid @RequestBody RatingRequest request) {

        ratingService.updateRating(userId, movieId, request.value());

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> deleteRating(
            @RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId) {

        ratingService.deleteRating(userId, movieId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<RatedMovieProjection>> getMyRatedMovies(
            @RequestHeader("X-Authenticated-User-Id") String userId) {

        return ResponseEntity.ok(
            ratingService.getMoviesRatedByUser(userId)
        );
    }
}