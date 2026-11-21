export interface UserProfile {
  id?: string | null;
  username: string;
  email: string;
  role: string ;
  twoFactorEnabled: boolean;
}