package com.neo4flix.movie_service.config;

import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.repository.GenreRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GenreSeeder implements CommandLineRunner {

    private final GenreRepository genreRepository;

    static final Set<String> GENRES = Set.of(
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