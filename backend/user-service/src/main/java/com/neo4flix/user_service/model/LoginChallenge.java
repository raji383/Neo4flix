
package com.neo4flix.user_service.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Node("LoginChallenge")
public class LoginChallenge {

    @Id
    private String id;

    private String userId;

    // Hashed OTP, never store the original code
    private String otpHash;

    private Instant expiresAt;

    private Instant createdAt;

    private int attempts;

    private boolean consumed;
}
