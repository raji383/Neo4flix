export interface LoginRequest {
    username: string;
    password: string;
}
export interface AuthResponse {
    id: string;
    username: string;
    email: string;
    jwtToken: string;
}