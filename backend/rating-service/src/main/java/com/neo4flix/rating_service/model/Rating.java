package com.neo4flix.rating_service.model;

import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import lombok.Data;

@Data
@RelationshipProperties
public class Rating {

    @RelationshipId
    private Long id;

    @Property("value")
    private Integer value;

    @TargetNode
    private Movie movie;
}