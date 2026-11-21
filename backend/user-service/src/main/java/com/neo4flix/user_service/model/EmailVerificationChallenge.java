
package com.neo4flix.user_service.model;

import java.time.Instant;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Getter
@Setter
@Node("EmailVerificationChallenge")
public class EmailVerificationChallenge {

    @Id
    private String id;

    private String userId;

    private String purpose;

    private String otpHash;

    private Instant createdAt;

    private Instant expiresAt;

    private int attempts;

    private boolean consumed;
}
