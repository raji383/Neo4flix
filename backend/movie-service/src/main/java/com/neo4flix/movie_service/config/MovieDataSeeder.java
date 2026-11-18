package com.neo4flix.movie_service.config;


import com.neo4flix.movie_service.config.tmdb.ByteArrayMultipartFile;
import com.neo4flix.movie_service.config.tmdb.TmdbClient;
import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.service.MovieService;

import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.Year;
import java.util.HashSet;
import java.util.Set;

@Component
@ConditionalOnProperty(
        prefix = "app.seed.movies",
        name = "enabled",
        havingValue = "true"
)
@Order(Ordered.LOWEST_PRECEDENCE)
public class MovieDataSeeder implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(MovieDataSeeder.class);

    private final TmdbClient tmdbClient;
    private final MovieService movieService;

    @Value("${app.seed.movies.count:100}")
    private int targetCount;

    @Value("${app.seed.movies.owner:}")
    private String owner;

    public MovieDataSeeder(
            TmdbClient tmdbClient,
            MovieService movieService
    ) {
        this.tmdbClient = tmdbClient;
        this.movieService = movieService;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (owner == null || owner.isBlank()) {
            throw new IllegalStateException(
                    "Configure app.seed.movies.owner before seeding"
            );
        }

        log.info("Starting TMDB movie seeding. Target: {}", targetCount);

        int created = 0;
        int failed = 0;

        for (int page = 1; page <= 500 && created < targetCount; page++) {

            JsonNode response = tmdbClient.discoverMovies(page);
            JsonNode movies = response.path("results");

            if (!movies.isArray() || movies.isEmpty()) {
                break;
            }

            for (JsonNode movie : movies) {

                if (created >= targetCount) {
                    break;
                }

                long tmdbId = movie.path("id").asLong();

                try {
                    seedMovie(movie, tmdbId);

                    created++;

                    log.info(
                            "Created movie {}/{}: {}",
                            created,
                            targetCount,
                            movie.path("title").asText()
                    );

                } catch (Exception e) {

                    failed++;

                    log.error(
                            "Failed to seed TMDB movie {}: {}",
                            tmdbId,
                            e.getMessage()
                    );
                }
            }
        }

        log.info(
                "Movie seeding finished. Created: {}, Failed: {}",
                created,
                failed
        );
    }

    private void seedMovie(
            JsonNode movie,
            long tmdbId
    ) {

        JsonNode details = tmdbClient.getMovieDetails(tmdbId);

        String title = truncate(
                details.path("title").asText(),
                100
        );

        String description = truncate(
                details.path("overview").asText(),
                250
        );

        String releaseDate = details.path("release_date").asText();

        if (title.isBlank()
                || description.isBlank()
                || releaseDate.length() < 4
                || details.path("poster_path").isMissingNode()
                || details.path("poster_path").isNull()) {

            throw new IllegalArgumentException(
                    "Missing required movie information"
            );
        }

        int releaseYear = Integer.parseInt(releaseDate.substring(0, 4));

        if (releaseYear < 1888 || releaseYear > Year.now().getValue()) {
            throw new IllegalArgumentException("Invalid release year");
        }

        String director = findDirector(details);

        Set<String> genres = new HashSet<>();

        for (JsonNode genre : details.path("genres")) {
            String name = genre.path("name").asText();
            if ("Science Fiction".equals(name)) {
                name = "Sci-Fi";
            }
            if (GenreSeeder.GENRES.contains(name)) {
                genres.add(name);
            }
        }

        if (genres.isEmpty()) {
            throw new IllegalArgumentException("Movie has no genres");
        }

        String posterPath = details.path("poster_path").asText();

        TmdbClient.DownloadedPoster poster =
                tmdbClient.downloadPoster(posterPath, tmdbId);

        MultipartFile file = new ByteArrayMultipartFile(
                "file",
                poster.filename(),
                poster.contentType(),
                poster.bytes()
        );

        CreateMovieDTO dto = new CreateMovieDTO(
                title,
                director,
                description,
                releaseYear,
                genres,
                file
        );

        // Reuse your existing business logic.
        movieService.addMovie(dto, owner);
    }

    private String findDirector(JsonNode details) {

        for (JsonNode crew : details.path("credits").path("crew")) {

            if ("Director".equalsIgnoreCase(
                    crew.path("job").asText()
            )) {
                String director = crew.path("name").asText();

                if (!director.isBlank()) {
                    return truncate(director, 100);
                }
            }
        }

        throw new IllegalArgumentException("Director not found");
    }

    private String truncate(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        String cleaned = value.trim();

        return cleaned.length() > maxLength
                ? cleaned.substring(0, maxLength)
                : cleaned;
    }
}
