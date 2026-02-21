import http from './http';
import { User, LoginCredentials, AuthResponse, CreateUserRequest } from '../types/auth';
import { PageResponse } from '../types/item';

export const authApi = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await http.post<AuthResponse>('/auth/login', credentials);
    return response.data;
  },

  logout: async (): Promise<void> => {
    await http.post('/auth/logout');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await http.get<User>('/auth/me');
    return response.data;
  },

  loginWithGoogle: (): void => {
    window.location.href = '/oauth2/authorization/google';
  },
};

export const adminApi = {
  getUsers: async (
    params: { page?: number; size?: number; sortBy?: string; sortDir?: string } = {},
    signal?: AbortSignal
  ): Promise<PageResponse<User>> => {
    const response = await http.get<PageResponse<User>>('/admin/users', { params, signal });
    return response.data;
  },

  createUser: async (data: CreateUserRequest): Promise<User> => {
    const response = await http.post<User>('/admin/users', data);
    return response.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await http.delete(`/admin/users/${id}`);
  },

  updateUserRole: async (id: string, role: string): Promise<User> => {
    const response = await http.patch<User>(`/admin/users/${id}/role`, { role });
    return response.data;
  },
};
