package com.neo4flix.rating_service.model;

import java.time.ZonedDateTime;

import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@RelationshipProperties
public class Watchlist {
    @RelationshipId
    private Long id;

    private ZonedDateTime addedAt;

    @TargetNode
    private Movie movie;
}
