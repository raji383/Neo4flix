package com.neo4flix.movie_service.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Node("Movie")
public class Movie {
    @Id
    @GeneratedValue(UUIDStringGenerator.class)
    private String id;
    private String title;
    private String description;
    private String director;
    private int releaseYear;
    private String createdBy;
    private String imageUrl;
    @Builder.Default
    @Relationship(type = "HAS_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres = new HashSet<>();
}