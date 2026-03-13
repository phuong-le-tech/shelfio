import http from './http';
import { User, LoginCredentials, SignupCredentials, AuthResponse, CreateUserRequest, AdminUserDetail, AdminDashboardStats, Role } from '../types/auth';
import { PageResponse, AdminItemList, AdminItem, ItemListWithItems } from '../types/item';

export const authApi = {
  login: async (credentials: LoginCredentials): Promise<AuthResponse> => {
    const response = await http.post<AuthResponse>('/auth/login', credentials);
    return response.data;
  },

  signup: async (credentials: SignupCredentials): Promise<AuthResponse> => {
    const response = await http.post<AuthResponse>('/auth/signup', credentials);
    return response.data;
  },

  googleAuth: async (credential: string): Promise<AuthResponse> => {
    const response = await http.post<AuthResponse>('/auth/google', { credential });
    return response.data;
  },

  logout: async (): Promise<void> => {
    await http.post('/auth/logout');
  },

  getCurrentUser: async (): Promise<User> => {
    const response = await http.get<User>('/auth/me');
    return response.data;
  },

  verifyEmail: async (token: string): Promise<void> => {
    await http.post('/auth/verify', { token });
  },

  resendVerification: async (email: string): Promise<void> => {
    await http.post('/auth/resend-verification', { email });
  },

  forgotPassword: async (email: string): Promise<void> => {
    await http.post('/auth/forgot-password', { email });
  },

  resetPassword: async (token: string, password: string): Promise<void> => {
    await http.post('/auth/reset-password', { token, password });
  },

  deleteAccount: async (): Promise<void> => {
    await http.delete('/account');
  },

  changePassword: async (currentPassword: string, newPassword: string): Promise<void> => {
    await http.patch('/account/password', { currentPassword, newPassword });
  },

  exportData: async (): Promise<unknown> => {
    const response = await http.get('/account/export');
    return response.data;
  },
};

export const adminApi = {
  getUsers: async (
    params: { page?: number; size?: number; sortBy?: string; sortDir?: string; search?: string; role?: Role; enabled?: boolean } = {},
    signal?: AbortSignal
  ): Promise<PageResponse<User>> => {
    const response = await http.get<PageResponse<User>>('/admin/users', { params, signal });
    return response.data;
  },

  getUser: async (id: string): Promise<AdminUserDetail> => {
    const response = await http.get<AdminUserDetail>(`/admin/users/${id}`);
    return response.data;
  },

  createUser: async (data: CreateUserRequest): Promise<User> => {
    const response = await http.post<User>('/admin/users', data);
    return response.data;
  },

  deleteUser: async (id: string): Promise<void> => {
    await http.delete(`/admin/users/${id}`);
  },

  updateUserRole: async (id: string, role: Role): Promise<User> => {
    const response = await http.patch<User>(`/admin/users/${id}/role`, { role });
    return response.data;
  },

  updateUserStatus: async (id: string, enabled: boolean): Promise<User> => {
    const response = await http.patch<User>(`/admin/users/${id}/status`, { enabled });
    return response.data;
  },

  triggerPasswordReset: async (id: string): Promise<void> => {
    await http.post(`/admin/users/${id}/reset-password`);
  },

  getLists: async (
    params: { page?: number; size?: number; sortBy?: string; sortDir?: string; search?: string; category?: string; ownerId?: string } = {},
    signal?: AbortSignal
  ): Promise<PageResponse<AdminItemList>> => {
    const response = await http.get<PageResponse<AdminItemList>>('/admin/lists', { params, signal });
    return response.data;
  },

  getListDetail: async (id: string): Promise<ItemListWithItems> => {
    const response = await http.get<ItemListWithItems>(`/admin/lists/${id}`);
    return response.data;
  },

  getStats: async (): Promise<AdminDashboardStats> => {
    const response = await http.get<AdminDashboardStats>('/admin/stats');
    return response.data;
  },

  getItems: async (
    params: { page?: number; size?: number; search?: string; itemListId?: string; status?: string } = {},
    signal?: AbortSignal
  ): Promise<PageResponse<AdminItem>> => {
    const response = await http.get<PageResponse<AdminItem>>('/admin/items', { params, signal });
    return response.data;
  },
};
