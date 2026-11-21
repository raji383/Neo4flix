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
    challengeId: string | null;
    requiresTwoFactor: boolean;
    jwtToken?: string | null;
}

export interface VerifyTwoFactorRequest {
    challengeId: string;
    otp: string;
}

export interface VerifyEmailRequest {
    challengeId: string;
    otp: string;
}

export interface RegistrationResponse {
    id: string;
    username: string;
    email: string;
    role: string | null;
    message: string;
}

export interface TwoFactorSetupResponse {
    challengeId: string;
    message: string;
}

export interface AuthResponse {
    id: string;
    username: string;
    email: string;
    role: string | null;
    jwtToken: string;
}
