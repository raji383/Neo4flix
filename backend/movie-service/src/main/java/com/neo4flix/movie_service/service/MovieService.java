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
    public MovieResponseDTO updateMovie(String id, CreateMovieDTO request, String userId) {
        Movie movie = findOwnedMovie(id, userId);

        movie.setTitle(request.title());
        movie.setDescription(request.description());
        movie.setDirector(request.director());
        movie.setReleaseYear(request.releaseYear());
        movie.setGenres(resolveGenres(request.genres()));

        if (request.file() != null && !request.file().isEmpty()) {
            try {
                movie.setImageUrl(mediaUploadService.uploadFile(request.file()).url());
            } catch (Exception e) {
                throw ApiException.badRequest("Invalid file type. Only JPEG, PNG, and WEBP images are allowed.");
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

        if (!movie.getCreatedBy().equals(userId)) {
            throw ApiException.forbidden("You can only modify movies you created");
        }
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

    public PageResponse<MovieResponseDTO> getMovies(int page, int size, String q, String genre, Integer releaseYear) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<Movie> result = movieRepository.search(
                blankToNull(q), blankToNull(genre), releaseYear, PageRequest.of(safePage, safeSize));

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

    public MovieResponseDTO getById(String id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Movie not found"));
        return toResponse(movie, movieRepository.summaries(List.of(id)).get(id));
    }

    private MovieResponseDTO toResponse(Movie movie, RatingSummary rating) {
        Set<String> genres = movie.getGenres() == null ? Set.of() : movie.getGenres().stream()
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
