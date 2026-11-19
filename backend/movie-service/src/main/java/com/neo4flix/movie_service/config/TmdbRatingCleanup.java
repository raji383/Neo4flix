package com.neo4flix.movie_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.neo4flix.movie_service.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TmdbRatingCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TmdbRatingCleanup.class);

    private final MovieRepository movieRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long cleanedMovies = movieRepository.removeTmdbRatingMetadata();
        if (cleanedMovies > 0) {
            log.info("Removed TMDB rating metadata from {} movies", cleanedMovies);
        }
    }
}
