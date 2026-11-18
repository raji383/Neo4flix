package com.neo4flix.rating_service.repository;

import java.util.Set;

public interface RatedMovieProjection {

    String getId();

    String getTitle();

    String getDirector();

    String getDescription();

    int getReleaseYear();

    Set<String> getGenres();

    String getImageUrl();

    Double getAverageRating();

    Long getRatingCount();
}