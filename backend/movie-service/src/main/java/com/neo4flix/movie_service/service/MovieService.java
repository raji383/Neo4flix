package com.neo4flix.movie_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.dto.response.MovieResponseDTO;
import com.neo4flix.movie_service.dto.response.PageResponse;
import com.neo4flix.movie_service.dto.response.RatingSummary;
import com.neo4flix.movie_service.exceptions.ApiException;
import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.model.Movie;
import com.neo4flix.movie_service.repository.GenreRepository;
import com.neo4flix.movie_service.repository.MovieRepository;
import com.neo4flix.movie_service.service.MediaUploadService.UploadResult;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MediaUploadService mediaUploadService;

    @Transactional
    public MovieResponseDTO addMovie(CreateMovieDTO request, String userId) {
        if (movieRepository.existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                request.title(),
                request.releaseYear(),
                request.director(),
                userId)) {
            throw ApiException.conflict("You have already added this movie");
        }

        Set<Genre> genres = resolveGenres(request.genres());
        UploadResult uploadResult;
        try {
            uploadResult = mediaUploadService.uploadFile(request.file());
        } catch (Exception e) {
            throw ApiException.badRequest("Invalid file type. Only JPEG, PNG, and WEBP images are allowed.");
        }
        Movie movie = Movie.builder()
                .title(request.title())
                .description(request.description())
                .director(request.director())
                .releaseYear(request.releaseYear())
                .createdBy(userId)
                .genres(genres)
                .imageUrl(uploadResult.url())
                .build();

        return toResponse(movieRepository.save(movie), null);
    }

    @Transactional
    public MovieResponseDTO addOrUpdateTmdbMovie(
            CreateMovieDTO request,
            String userId,
            long tmdbId,
            double tmdbPopularity,
            String posterUrl) {
        Movie movie = movieRepository.findFirstByTmdbId(tmdbId).orElse(null);
        if (movie == null) {
            movie = movieRepository
                    .findFirstByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                            request.title(),
                            request.releaseYear(),
                            request.director(),
                            userId)
                    .orElse(null);
        }

        boolean duplicate = movie == null
                ? movieRepository.existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
                        request.title(),
                        request.releaseYear(),
                        request.director(),
                        userId)
                : movieRepository.existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedByAndIdNot(
                        request.title(),
                        request.releaseYear(),
                        request.director(),
                        userId,
                        movie.getId());
        if (duplicate) {
            throw ApiException.conflict("You have already added this movie");
        }

        Set<Genre> genres = resolveGenres(request.genres());
        if (movie == null) {
            movie = Movie.builder()
                    .createdBy(userId)
                    .build();
        }

        movie.setTmdbId(tmdbId);
        movie.setTmdbPopularity(tmdbPopularity);
        movie.setImageUrl(posterUrl);
        movie.setTitle(request.title());
        movie.setDirector(request.director());
        movie.setDescription(request.description());
        movie.setReleaseYear(request.releaseYear());
        movie.setGenres(genres);

        return toResponse(movieRepository.save(movie), null);
    }

  @Transactional
public MovieResponseDTO updateMovie(String id, CreateMovieDTO request, String userId) {

    Movie movie = findOwnedMovie(id, userId);

    boolean duplicate = movieRepository
            .existsByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedByAndIdNot(
                    request.title(),
                    request.releaseYear(),
                    request.director(),
                    userId,
                    id
            );

    if (duplicate) {
        throw ApiException.conflict("You have already added this movie");
    }

    movie.setTitle(request.title());
    movie.setDescription(request.description());
    movie.setDirector(request.director());
    movie.setReleaseYear(request.releaseYear());
    movie.setGenres(resolveGenres(request.genres()));

    if (request.file() != null && !request.file().isEmpty()) {
        try {
            movie.setImageUrl(mediaUploadService.uploadFile(request.file()).url());
        } catch (Exception e) {
            throw ApiException.badRequest(
                    "Invalid file type. Only JPEG, PNG, and WEBP images are allowed."
            );
        }
    }

    return toResponse(movieRepository.save(movie), null);
}
    @Transactional
    public void deleteMovie(String id, String userId) {
        movieRepository.delete(findOwnedMovie(id, userId));
    }

    private Movie findOwnedMovie(String id, String userId) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Movie not found"));

        // if (!movie.getCreatedBy().equals(userId)) {
        // throw ApiException.forbidden("You can only modify movies you created");
        // }
        return movie;
    }

    private Set<Genre> resolveGenres(Set<String> names) {
        List<Genre> found = genreRepository.findAllById(names);
        if (found.size() != names.size()) {
            Set<String> foundNames = found.stream().map(Genre::getName).collect(Collectors.toSet());
            Set<String> unknown = new HashSet<>(names);
            unknown.removeAll(foundNames);
            throw ApiException.badRequest("Unknown genres: " + unknown);
        }
        return new HashSet<>(found);
    }

    public PageResponse<MovieResponseDTO> getMovies(
            int page, int size, String q, List<String> genres, Integer releaseYear) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Movie> result = movieRepository.search(
                blankToNull(q), cleanGenres(genres), releaseYear, PageRequest.of(safePage, safeSize));

        Map<String, RatingSummary> ratings = movieRepository.summaries(
                result.getContent().stream().map(Movie::getId).toList());

        List<MovieResponseDTO> content = result.getContent().stream()
                .map(m -> toResponse(m, ratings.get(m.getId())))
                .toList();

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private List<String> cleanGenres(List<String> genres) {
        return genres == null ? List.of()
                : genres.stream().filter(genre -> genre != null && !genre.isBlank()).map(String::trim).toList();
    }

    public MovieResponseDTO getById(String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Movie not found"));
        return toResponse(movie, movieRepository.summaries(List.of(id)).get(id));
    }

    public List<MovieResponseDTO> getPopular(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 20);

        List<Movie> tmdbPopular = movieRepository.topTmdbPopular(safeLimit);
        if (tmdbPopular.isEmpty()) {
            return List.of();
        }

        Map<String, RatingSummary> ratings = movieRepository.summaries(
                tmdbPopular.stream().map(Movie::getId).toList());
        return tmdbPopular.stream()
                .map(movie -> toResponse(movie, ratings.get(movie.getId())))
                .toList();
    }

    private MovieResponseDTO toResponse(Movie movie, RatingSummary rating) {
        Set<String> genres = movie.getGenres() == null ? Set.of()
                : movie.getGenres().stream()
                        .map(Genre::getName)
                        .collect(Collectors.toSet());

        Double avg = (rating == null || rating.average() == null) ? null
                : Math.round(rating.average() * 10) / 10.0;
        long count = rating == null ? 0 : rating.count();

        return new MovieResponseDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getDescription(),
                movie.getReleaseYear(),
                genres,
                movie.getImageUrl(),
                avg,
                count);
    }
}
