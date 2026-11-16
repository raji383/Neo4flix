package com.neo4flix.movie_service.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.neo4flix.movie_service.dto.request.CreateMovieDTO;
import com.neo4flix.movie_service.dto.response.MovieResponseDTO;
import com.neo4flix.movie_service.exceptions.ApiException;
import com.neo4flix.movie_service.model.Genre;
import com.neo4flix.movie_service.model.Movie;
import com.neo4flix.movie_service.repository.GenreRepository;
import com.neo4flix.movie_service.repository.MovieRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MovieService {
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    @Transactional
    public MovieResponseDTO addMovie(CreateMovieDTO request, String userId) {
        Set<Genre> genres = resolveGenres(request.genres());

        Movie movie = Movie.builder()
                .title(request.title())
                .description(request.description())
                .director(request.director())
                .releaseYear(request.releaseYear())
                .createdBy(userId)
                .genres(genres)
                .build();

        return toResponse(movieRepository.save(movie));
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

    private MovieResponseDTO toResponse(Movie movie) {
        return new MovieResponseDTO(
                movie.getId(),
                movie.getTitle(),
                movie.getDirector(),
                movie.getDescription(),
                movie.getReleaseYear(),
                movie.getGenres().stream().map(Genre::getName).collect(Collectors.toSet())
        );
    }

}