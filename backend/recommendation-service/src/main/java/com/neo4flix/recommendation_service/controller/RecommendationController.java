
package com.neo4flix.recommendation_service.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neo4flix.recommendation_service.dto.MovieRecommendation;
import com.neo4flix.recommendation_service.service.RecommendationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @GetMapping
    public List<MovieRecommendation> recommendMovies(
            @RequestParam String currentUserId) {

        return recommendationService.recommendMovies(currentUserId);
    }
}
