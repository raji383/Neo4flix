
package com.neo4flix.recommendation_service.repository;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Repository;

import com.neo4flix.recommendation_service.dto.AvailableMovie;
import com.neo4flix.recommendation_service.dto.CandidateMovieRating;
import com.neo4flix.recommendation_service.dto.MovieGenreScore;
import com.neo4flix.recommendation_service.dto.RatingComparison;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class RecommendationRepository {

    private final Neo4jClient neo4jClient;

    public boolean userExists(String userId) {

        String query = """
                MATCH (u:User {id: $userId})
                RETURN count(u) > 0 AS userExists
                """;

        return neo4jClient.query(query)
                .bind(userId).to("userId")
                .fetchAs(Boolean.class)
                .mappedBy((typeSystem, record) -> record.get("userExists").asBoolean())
                .one()
                .orElse(false);
    }

    public List<AvailableMovie> findAvailableMovies(String currentUserId) {

        String query = """
                MATCH (movie:Movie)
                WHERE NOT EXISTS {
                    MATCH (:User {id: $userId})-[:RATED]->(movie)
                }

                OPTIONAL MATCH (:User)-[r:RATED]->(movie)

                RETURN
                    movie.id AS movieId,
                    movie.title AS title,
                    coalesce(avg(r.value), 0.0) AS averageRating
                """;

        return neo4jClient.query(query)
                .bind(currentUserId).to("userId")
                .fetchAs(AvailableMovie.class)
                .mappedBy((typeSystem, record) -> new AvailableMovie(
                        record.get("movieId").asString(),
                        record.get("title").asString(),
                        record.get("averageRating").asDouble()))
                .all()
                .stream()
                .toList();
    }

    public List<MovieGenreScore> findMovieGenreScores(String currentUserId) {

        String query = """
                MATCH (u:User {id: $userId})-[r:RATED]->(ratedMovie:Movie)
                      -[:HAS_GENRE]->(g:Genre)

                WITH u, g, avg(r.value) / 5.0 AS preferenceScore

                MATCH (candidate:Movie)-[:HAS_GENRE]->(g)

                WHERE NOT EXISTS {
                    MATCH (u)-[:RATED]->(candidate)
                }

                WITH candidate, avg(preferenceScore) AS genreScore

                RETURN
                    candidate.id AS movieId,
                    candidate.title AS title,
                    genreScore
                ORDER BY genreScore DESC
                """;

        return neo4jClient.query(query)
                .bind(currentUserId).to("userId")
                .fetchAs(MovieGenreScore.class)
                .mappedBy((typeSystem, record) -> new MovieGenreScore(
                        record.get("movieId").asString(),
                        record.get("title").asString(),
                        record.get("genreScore").asDouble()))
                .all()
                .stream()
                .toList();
    }

    public List<RatingComparison> findCommonRatings(
            String currentUserId) {

        String query = """
                MATCH (currentUser:User {id: $userId})
                      -[r1:RATED]->(movie:Movie)

                MATCH (otherUser:User)-[r2:RATED]->(movie)

                WHERE otherUser <> currentUser

                RETURN
                    otherUser.id AS userId,
                    otherUser.username AS username,
                    movie.id AS movieId,
                    r1.value AS currentRating,
                    r2.value AS otherRating
                """;

        return neo4jClient.query(query)
                .bind(currentUserId).to("userId")
                .fetchAs(RatingComparison.class)
                .mappedBy((typeSystem, record) -> new RatingComparison(
                        record.get("userId").asString(),
                        record.get("username").asString(),
                        record.get("movieId").asString(),
                        record.get("currentRating").asDouble(),
                        record.get("otherRating").asDouble()))
                .all()
                .stream()
                .toList();
    }

    public List<CandidateMovieRating> findCandidateMovies(
            String currentUserId,
            List<String> similarUserIds) {

        if (similarUserIds == null || similarUserIds.isEmpty()) {
            return List.of();
        }

        String query = """
                MATCH (currentUser:User {id: $userId})
                MATCH (similarUser:User)-[r:RATED]->(movie:Movie)

                WHERE similarUser.id IN $similarUserIds
                  AND NOT EXISTS {
                      MATCH (currentUser)-[:RATED]->(movie)
                  }

                RETURN
                    movie.id AS movieId,
                    movie.title AS title,
                    similarUser.id AS userId,
                    r.value AS rating
                """;

        return neo4jClient.query(query)
                .bind(currentUserId).to("userId")
                .bind(similarUserIds).to("similarUserIds")
                .fetchAs(CandidateMovieRating.class)
                .mappedBy((typeSystem, record) -> new CandidateMovieRating(
                        record.get("movieId").asString(),
                        record.get("title").asString(),
                        record.get("userId").asString(),
                        record.get("rating").asDouble()))
                .all()
                .stream()
                .toList();
    }
}
