export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: 'ROLE_TRADER' | 'ROLE_ANALYST' | 'ROLE_ADMIN';
}

export interface LoginDTO {
  emailOrUsername: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface RegisterDTO {
  fullName: string;
  username: string;
  email: string;
  phoneNumber: string;
  password: string;
}