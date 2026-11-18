package com.neo4flix.rating_service.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import lombok.Data;

@Node("User")
@Data
public class User {

    @Id
    private String id;

    @Relationship(type = "RATED", direction = Relationship.Direction.OUTGOING)
    private Set<Rating> ratings = new HashSet<>();
}