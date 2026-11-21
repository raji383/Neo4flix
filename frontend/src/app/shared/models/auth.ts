export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
    confirmPassword: string;
}

export interface LoginChallengeResponse {
    challengeId: string;
    requiresTwoFactor: boolean;
}

export interface VerifyTwoFactorRequest {
    challengeId: string;
    otp: string;
}

export interface RegistrationResponse {
    id: string;
    username: string;
    email: string;
    role: string | null;
}

export interface AuthResponse {
    id: string;
    username: string;
    email: string;
    role: string | null;
    jwtToken: string;
}
