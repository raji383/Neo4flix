package com.neo4flix.rating_service.repository;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.rating_service.dto.response.WatchlistItemDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WatchlistQueryRepository {

    private final Neo4jClient neo4jClient;

    public List<WatchlistItemDTO> findByUser(String userId) {
        String query = """
                MATCH (:User {id: $userId})-[w:WATCHLISTED]->(m:Movie)
                RETURN m.id AS id, m.title AS title, m.imageUrl AS imageUrl,
                       m.releaseYear AS releaseYear, w.addedAt AS addedAt
                ORDER BY w.addedAt DESC
                """;

        return List.copyOf(neo4jClient.query(query)
                .bind(userId).to("userId")
                .fetchAs(WatchlistItemDTO.class)
                .mappedBy((typeSystem, record) -> new WatchlistItemDTO(
                        record.get("id").asString(),
                        record.get("title").asString(),
                        record.get("imageUrl").isNull()
                                ? null
                                : record.get("imageUrl").asString(),
                        record.get("releaseYear").isNull()
                                ? null
                                : record.get("releaseYear").asInt(),
                        record.get("addedAt").isNull()
                                ? null
                                : record.get("addedAt").asZonedDateTime()))
                .all());
    }
}
