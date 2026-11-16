package com.neo4flix.movie_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.repository.GenreRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/genres")
@RequiredArgsConstructor
public class GenreController {

    private final GenreRepository genreRepository;

    @GetMapping
    public List<String> all() {
        return genreRepository.findAll().stream().map(Genre::getName).sorted().toList();
    }
}