import axios from 'axios';
import { Item, ItemFormData, DashboardStats, PageResponse, ItemSearchParams } from '../types/item';

const api = axios.create({
  baseURL: '/api/v1',
});

export const itemsApi = {
  getAll: async (params: ItemSearchParams = {}): Promise<PageResponse<Item>> => {
    const response = await api.get<PageResponse<Item>>('/items', { params });
    return response.data;
  },

  getById: async (id: string): Promise<Item> => {
    const response = await api.get<Item>(`/items/${id}`);
    return response.data;
  },

  create: async (data: ItemFormData, image?: File): Promise<Item> => {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
      formData.append('image', image);
    }
    const response = await api.post<Item>('/items/', formData);
    return response.data;
  },

  update: async (id: string, data: ItemFormData, image?: File): Promise<Item> => {
    const formData = new FormData();
    formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }));
    if (image) {
      formData.append('image', image);
    }
    const response = await api.patch<Item>(`/items/${id}`, formData);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await api.delete(`/items/${id}`);
  },
};

export const dashboardApi = {
  getStats: async (): Promise<DashboardStats> => {
    const response = await api.get<DashboardStats>('/items/stats');
    return response.data;
  },
};
