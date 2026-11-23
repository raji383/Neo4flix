package com.neo4flix.movie_service.repository.embded;

import java.util.Collection;
import java.util.Map;

import com.neo4flix.movie_service.dto.response.RatingSummary;

public interface MovieRepositoryCustom {
    Map<String, RatingSummary> summaries(Collection<String> movieIds);
}
