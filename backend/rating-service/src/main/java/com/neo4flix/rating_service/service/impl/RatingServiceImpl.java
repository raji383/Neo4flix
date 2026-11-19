package com.neo4flix.rating_service.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neo4flix.rating_service.dto.response.RatedMovieResponse;
import com.neo4flix.rating_service.repository.RatingQueryRepository;
import com.neo4flix.rating_service.repository.RatingRepository;
import com.neo4flix.rating_service.service.RatingService;
import com.neo4flix.rating_service.exceptions.ApiException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final RatingQueryRepository ratingQueryRepository;

    private void validateRating(Integer value) {
        if (value == null || value < 1 || value > 5) {
            throw ApiException.badRequest("Rating value must be between 1 and 5");
        }
    }

    @Override
    public void createRating(String userId, String movieId, Integer value) {

        validateRating(value);

        if (ratingRepository.existsRating(userId, movieId)) {
            throw ApiException.conflict("You have already rated this movie");
        }

        Long result = ratingRepository.createRating(userId, movieId, value);

        if (result == null) {
            throw ApiException.notFound("User or Movie not found");
        }
    }

    @Override
    public void updateRating(String userId, String movieId, Integer value) {

        validateRating(value);

        Long result = ratingRepository.updateRating(userId, movieId, value);

        if (result == null) {
            throw ApiException.notFound("Rating not found");
        }
    }

    @Override
    public void deleteRating(String userId, String movieId) {

        Long result = ratingRepository.deleteRating(userId, movieId);

        if (result == null) {
            throw ApiException.notFound("Rating not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatedMovieResponse> getMoviesRatedByUser(String userId) {

        return ratingQueryRepository.findMoviesRatedByUser(userId);
    }
    @Override
    @Transactional(readOnly = true)
    public Integer getMyRating(String userId, String movieId) {
        Integer rating = ratingRepository.getMyRating(userId, movieId);

        if (rating == null) {
            throw ApiException.notFound("Rating not found");
        }

        return rating;
    }
}
