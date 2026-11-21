package com.neo4flix.user_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TwoFactorSetupResponse {
	private String challengeId;
	private String message;
}
