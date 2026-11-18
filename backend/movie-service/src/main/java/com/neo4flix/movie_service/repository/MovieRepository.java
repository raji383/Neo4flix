package com.neo4flix.movie_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import com.neo4flix.movie_service.dto.response.RatingSummary;
import com.neo4flix.movie_service.model.Movie;
import com.neo4flix.movie_service.repository.embded.MovieRepositoryCustom;

public interface MovieRepository extends Neo4jRepository<Movie, String>, MovieRepositoryCustom {

  Optional<Movie> findFirstByTmdbId(Long tmdbId);

  Optional<Movie> findFirstByTitleIgnoreCaseAndReleaseYearAndDirectorIgnoreCaseAndCreatedBy(
      String title, int releaseYear, String director, String createdBy);

  @Query("""
      MATCH (m:Movie)
      WHERE m.tmdbVoteAverage IS NOT NULL
      OPTIONAL MATCH (m)-[r:HAS_GENRE]->(g:Genre)
      RETURN m, collect(r), collect(g)
      ORDER BY coalesce(m.tmdbPopularity, toFloat(m.tmdbVoteCount), 0.0) DESC,
               m.tmdbVoteCount DESC,
               m.title
      LIMIT $limit
      """)
  List<Movie> topTmdbPopular(int limit);

  @Query(value = """
      MATCH (m:Movie)
      WHERE ($q IS NULL OR toLower(m.title) CONTAINS toLower($q)
                        OR toLower(m.director) CONTAINS toLower($q))
        AND ($year IS NULL OR m.releaseYear = $year)
        AND (size($genres) = 0 OR EXISTS {
          MATCH (m)-[:HAS_GENRE]->(g:Genre)
          WHERE g.name IN $genres
        })
      OPTIONAL MATCH (m)-[r:HAS_GENRE]->(g:Genre)
      RETURN m, collect(r), collect(g)
      ORDER BY m.title
      SKIP $skip LIMIT $limit
      """, countQuery = """
      MATCH (m:Movie)
      WHERE ($q IS NULL OR toLower(m.title) CONTAINS toLower($q)
                        OR toLower(m.director) CONTAINS toLower($q))
        AND ($year IS NULL OR m.releaseYear = $year)
        AND (size($genres) = 0 OR EXISTS {
          MATCH (m)-[:HAS_GENRE]->(g:Genre)
          WHERE g.name IN $genres
        })
      RETURN count(m)
      """)
  Page<Movie> search(String q, List<String> genres, Integer year, Pageable pageable);

  @Query("""
      MATCH (:User)-[r:RATED]->(:Movie {id: $id})
      RETURN avg(r.value)
      """)
  Double findAverageRating(String id);

  @Query("""
      MATCH (:User)-[r:RATED]->(:Movie {id: $id})
      RETURN count(r)
      """)
  Long countRatings(String id);

  Map<String, RatingSummary> summaries(Collection<String> movieIds);

  List<RatingSummary> topRated(int minVotes, int limit);

}