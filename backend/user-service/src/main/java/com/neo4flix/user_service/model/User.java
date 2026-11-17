package com.neo4flix.user_service.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Getter
@Setter
@Node("User")
public class User {
    @Id
    private String id;

    private String username;
    private String email;
    private String passwordHash;

    // private String firstName;
    // private String lastName;

    private String role;
    // private boolean enabled;

    // private Instant createdAt;
}