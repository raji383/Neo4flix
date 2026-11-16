package com.neo4flix.movie_service.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.repository.GenreRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GenreSeeder implements CommandLineRunner {

    private final GenreRepository genreRepository;

    private static final List<String> GENRES = List.of(
            "Action", "Adventure", "Animation", "Comedy", "Crime", "Documentary",
            "Drama", "Fantasy", "Horror", "Mystery", "Romance", "Sci-Fi",
            "Thriller", "War", "Western");

    @Override
    public void run(String... args) {
        GENRES.forEach(name -> {
            if (!genreRepository.existsById(name)) {
                genreRepository.save(new Genre(name));
            }
        });
    }
}