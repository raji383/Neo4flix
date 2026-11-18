package com.neo4flix.rating_service.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Data;

@Node("Movie")
@Data 
public class Movie {

    @Id
    private String id;

    private String title;
    private String description;
    private String director;
    private int releaseYear;
    private String createdBy;
    private String imageUrl;

    @Relationship(type = "HAS_GENRE", direction = Relationship.Direction.OUTGOING)
    private Set<Genre> genres = new HashSet<>();
}