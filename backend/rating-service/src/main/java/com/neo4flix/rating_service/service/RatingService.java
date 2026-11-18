package com.neo4flix.rating_service.service;

import java.util.List;

import com.neo4flix.rating_service.repository.RatedMovieProjection;

public interface RatingService {

    void createRating(String userId, String movieId, Integer value);

    void updateRating(String userId, String movieId, Integer value);

    void deleteRating(String userId, String movieId);

    List<RatedMovieProjection> getMoviesRatedByUser(String userId);
}