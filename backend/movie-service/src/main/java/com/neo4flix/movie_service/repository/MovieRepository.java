package com.neo4flix.movie_service.repository;

import java.util.Collection;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import com.neo4flix.movie_service.dto.response.RatingSummary;
import com.neo4flix.movie_service.model.Movie;
import com.neo4flix.movie_service.repository.embded.MovieRepositoryCustom;

public interface MovieRepository extends Neo4jRepository<Movie, String>, MovieRepositoryCustom {

        @Query(value = """
                        MATCH (m:Movie)
                        WHERE ($q IS NULL OR toLower(m.title) CONTAINS toLower($q)
                                          OR toLower(m.director) CONTAINS toLower($q))
                          AND ($year IS NULL OR m.releaseYear = $year)
                          AND ($genre IS NULL OR EXISTS { (m)-[:HAS_GENRE]->(:Genre {name: $genre}) })
                        OPTIONAL MATCH (m)-[r:HAS_GENRE]->(g:Genre)
                        RETURN m, collect(r), collect(g)
                        ORDER BY m.title
                        SKIP $skip LIMIT $limit
                        """, countQuery = """
                        MATCH (m:Movie)
                        WHERE ($q IS NULL OR toLower(m.title) CONTAINS toLower($q)
                                          OR toLower(m.director) CONTAINS toLower($q))
                          AND ($year IS NULL OR m.releaseYear = $year)
                          AND ($genre IS NULL OR EXISTS { (m)-[:HAS_GENRE]->(:Genre {name: $genre}) })
                        RETURN count(m)
                        """)
        Page<Movie> search(String q, String genre, Integer year, Pageable pageable);

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

}