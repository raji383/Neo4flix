package com.neo4flix.rating_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import com.neo4flix.rating_service.model.Watchlist;

public interface WatchlistRepository extends Neo4jRepository<Watchlist, Long> {
    
    @Query("""
        MATCH (u:User {id: $userId}), (m:Movie {id: $movieId})
        MERGE (u)-[w:WATCHLISTED]->(m)
        ON CREATE SET w.addedAt = datetime()
        """)
    void add(String userId, String movieId);

    @Query("""
        MATCH (:User {id: $userId})-[w:WATCHLISTED]->(:Movie {id: $movieId})
        DELETE w
        """)
    void remove(String userId, String movieId);

    @Query("""
        RETURN EXISTS { (:User {id: $userId})-[:WATCHLISTED]->(:Movie {id: $movieId}) }
        """)
    boolean isInWatchlist(String userId, String movieId);
}
