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
export interface AuthResponse {
    id: string;
    username: string;
    email: string;
    role: string | null;
    jwtToken: string;
}
