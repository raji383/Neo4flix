package com.neo4flix.user_service.model;
import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
public class User {
    @Id
    private String id;

    private String username;
    private String email;
    private String password;

    private String firstName;
    private String lastName;

    private String role;
    private boolean enabled;

    private Instant createdAt;
}