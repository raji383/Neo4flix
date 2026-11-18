package com.neo4flix.rating_service.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.rating_service.dto.response.RatedMovieResponse;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RatingQueryRepository {

    private final Neo4jClient neo4jClient;

    public List<RatedMovieResponse> findMoviesRatedByUser(String userId) {

        String query = """
                MATCH (:User {id: $userId})-[:RATED]->(m:Movie)

                CALL (m) {
                    OPTIONAL MATCH (m)<-[r:RATED]-(:User)
                    RETURN avg(r.value) AS averageRating,
                           count(r) AS ratingCount
                }

                CALL (m) {
                    OPTIONAL MATCH (m)-[:HAS_GENRE]->(g:Genre)
                    RETURN collect(g.name) AS genres
                }

                RETURN m.id AS id,
                       m.title AS title,
                       m.director AS director,
                       m.description AS description,
                       m.releaseYear AS releaseYear,
                       genres AS genres,
                       m.imageUrl AS imageUrl,
                       averageRating AS averageRating,
                       ratingCount AS ratingCount
                """;

        return List.copyOf(neo4jClient.query(query)
                .bind(userId).to("userId")
                .fetchAs(RatedMovieResponse.class)
                .mappedBy((typeSystem, record) -> new RatedMovieResponse(
                        record.get("id").asString(),
                        record.get("title").asString(),
                        record.get("director").asString(),
                        record.get("description").asString(),
                        record.get("releaseYear").asInt(),
                        Set.copyOf(record.get("genres").asList(value -> value.asString())),
                        record.get("imageUrl").isNull()
                                ? null
                                : record.get("imageUrl").asString(),
                        record.get("averageRating").isNull()
                                ? null
                                : record.get("averageRating").asDouble(),
                        record.get("ratingCount").asLong()))
                .all());
    }
}
