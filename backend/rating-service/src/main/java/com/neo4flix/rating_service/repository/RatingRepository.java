package com.neo4flix.rating_service.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.neo4flix.rating_service.model.User;


@Repository
public interface RatingRepository extends Neo4jRepository<User, String> {

    // Check if user already rated this movie
    @Query("""
        MATCH (u:User {id: $userId})
        MATCH (m:Movie {id: $movieId})
        RETURN EXISTS {
            MATCH (u)-[:RATED]->(m)
        }
        """)
    boolean existsRating(
        @Param("userId") String userId,
        @Param("movieId") String movieId
    );

    // Create Rating
    @Query("""
        MATCH (u:User {id: $userId})
        MATCH (m:Movie {id: $movieId})
        WHERE NOT EXISTS {
            MATCH (u)-[:RATED]->(m)
        }
        CREATE (u)-[:RATED {value: $value}]->(m)
        RETURN 1
        """)
    Long createRating(
        @Param("userId") String userId,
        @Param("movieId") String movieId,
        @Param("value") Integer value
    );

    // Update Rating
    @Query("""
        MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie {id: $movieId})
        SET r.value = $value
        RETURN 1
        """)
    Long updateRating(
        @Param("userId") String userId,
        @Param("movieId") String movieId,
        @Param("value") Integer value
    );

    // Delete Rating
    @Query("""
        MATCH (u:User {id: $userId})-[r:RATED]->(m:Movie {id: $movieId})
        DELETE r
        RETURN 1
        """)
    Long deleteRating(
        @Param("userId") String userId,
        @Param("movieId") String movieId
    );

    // Get one user's rating for one movie
    @Query("""
        MATCH (:User {id: $userId})-[r:RATED]->(:Movie {id: $movieId})
        RETURN r.value
        """)
    Integer getMyRating(
        @Param("userId") String userId,
        @Param("movieId") String movieId
    );

}
