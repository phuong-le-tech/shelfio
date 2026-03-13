export type Role = 'USER' | 'PREMIUM_USER' | 'ADMIN';

export function isPremium(role: Role): boolean {
  return role === 'PREMIUM_USER' || role === 'ADMIN';
}

export interface User {
  id: string;
  email: string;
  role: Role;
  pictureUrl?: string;
  hasGoogleAccount: boolean;
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

export interface AdminDashboardStats {
  totalUsers: number;
  activeUsers: number;
  premiumUsers: number;
  adminUsers: number;
  premiumConversionRate: number;
  registrationTrend: Record<string, number>;
  topUsersByLists: { email: string; count: number }[];
  topUsersByItems: { email: string; count: number }[];
}

export interface AdminUserDetail {
  id: string;
  email: string;
  role: Role;
  pictureUrl?: string;
  hasGoogleAccount: boolean;
  enabled: boolean;
  listCount: number;
  itemCount: number;
  hasStripePayment: boolean;
  stripeCustomerId?: string;
  createdAt: string;
  updatedAt: string;
}
