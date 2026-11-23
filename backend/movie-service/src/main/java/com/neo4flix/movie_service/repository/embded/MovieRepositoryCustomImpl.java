package com.neo4flix.movie_service.repository.embded;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.movie_service.dto.response.RatingSummary;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class MovieRepositoryCustomImpl implements MovieRepositoryCustom {

    private final Neo4jClient neo4jClient;

    @Override
    public Map<String, RatingSummary> summaries(Collection<String> movieIds) {

        if (movieIds.isEmpty()) {
            return Map.of();
        }

        return neo4jClient.query("""
                MATCH (m:Movie)
                WHERE m.id IN $ids
                OPTIONAL MATCH (:User)-[r:RATED]->(m)
                RETURN m.id AS movieId,
                       avg(r.value) AS average,
                       count(r) AS count
                """)
                .bind(movieIds).to("ids")
                .fetchAs(RatingSummary.class)
                .mappedBy((ts, record) -> new RatingSummary(
                        record.get("movieId").asString(),
                        record.get("average").isNull()
                                ? null
                                : record.get("average").asDouble(),
                        record.get("count").asLong()
                ))
                .all()
                .stream()
                .collect(Collectors.toMap(
                        RatingSummary::movieId,
                        s -> s
                ));
    }

}