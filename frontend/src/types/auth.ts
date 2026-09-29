export type AuthResponse = {
  userId: string;
  email: string;
  name: string;
  accessToken: string;
  expiresIn: number;
  refreshToken: string;
};
