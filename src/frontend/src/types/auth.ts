export type Role = 'USER' | 'PREMIUM_USER' | 'ADMIN';

export interface User {
  id: string;
  email: string;
  role: Role;
  pictureUrl?: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface AuthResponse {
  user: User;
  message: string;
}

export interface SignupCredentials {
  email: string;
  password: string;
  confirmPassword: string;
}

export interface CreateUserRequest {
  email: string;
  password: string;
  role?: Role;
}
