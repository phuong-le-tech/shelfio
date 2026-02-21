import http from './http';
import {
  Item,
  ItemFormData,
  DashboardStats,
  PageResponse,
  ItemSearchParams,
  ItemList,
  ItemListWithItems,
  ItemListFormData,
  ItemListSearchParams
} from '../types/item';

export const listsApi = {
  getAll: async (params: ItemListSearchParams = {}, signal?: AbortSignal): Promise<PageResponse<ItemList>> => {
    const response = await http.get<PageResponse<ItemList>>('/lists', { params, signal });
    return response.data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<ItemListWithItems> => {
    const response = await http.get<ItemListWithItems>(`/lists/${id}`, { signal });
    return response.data;
  },

  create: async (data: ItemListFormData): Promise<ItemList> => {
    const response = await http.post<ItemList>('/lists', data);
    return response.data;
  },

  update: async (id: string, data: ItemListFormData): Promise<ItemList> => {
    const response = await http.patch<ItemList>(`/lists/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await http.delete(`/lists/${id}`);
  },
};

export const itemsApi = {
  getAll: async (params: ItemSearchParams = {}, signal?: AbortSignal): Promise<PageResponse<Item>> => {
    const response = await http.get<PageResponse<Item>>('/items', { params, signal });
    return response.data;
  },

  getById: async (id: string, signal?: AbortSignal): Promise<Item> => {
    const response = await http.get<Item>(`/items/${id}`, { signal });
    return response.data;
  },

  create: async (data: ItemFormData, image?: File): Promise<Item> => {
    const formData = new FormData();
    formData.append('data', JSON.stringify(data));
    if (image) {
      formData.append('image', image);
    }
    const response = await http.post<Item>('/items', formData);
    return response.data;
  },

  update: async (id: string, data: ItemFormData, image?: File): Promise<Item> => {
    const formData = new FormData();
    formData.append('data', JSON.stringify(data));
    if (image) {
      formData.append('image', image);
    }
    const response = await http.patch<Item>(`/items/${id}`, formData);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await http.delete(`/items/${id}`);
  },
};

export const dashboardApi = {
  getStats: async (signal?: AbortSignal): Promise<DashboardStats> => {
    const response = await http.get<DashboardStats>('/items/stats', { signal });
    return response.data;
  },
};
