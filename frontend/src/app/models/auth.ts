export interface User {
  id?: number;
  username: string;
  email: string;
  fullName: string;
  phoneNumber?: string;
  role: 'ROLE_TRADER' | 'ROLE_ANALYST' | 'ROLE_ADMIN';
  status: 'PENDING_VERIFICATION' | 'ACTIVE' | 'SUSPENDED' | 'DEACTIVATED';
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: User;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}