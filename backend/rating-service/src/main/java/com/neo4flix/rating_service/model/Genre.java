package com.neo4flix.rating_service.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Node("Genre")
public class Genre {
    @Id
    private String name;
}