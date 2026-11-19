package com.neo4flix.rating_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.rating_service.dto.response.WatchlistItemDTO;
import com.neo4flix.rating_service.service.WatchlistService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {
    private final WatchlistService service;

    @PostMapping("/{movieId}")
    public ResponseEntity<Void> add(@RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId) {
        service.add(userId, movieId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> remove(@RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId) {
        service.remove(userId, movieId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<WatchlistItemDTO> list(@RequestHeader("X-Authenticated-User-Id") String userId) {
        return service.list(userId);
    }

    @GetMapping("/{movieId}")
    public Map<String, Boolean> contains(@RequestHeader("X-Authenticated-User-Id") String userId,
            @PathVariable String movieId) {
        return Map.of("inWatchlist", service.contains(userId, movieId));
    }
}
