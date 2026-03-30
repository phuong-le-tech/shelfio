import axios from 'axios';
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
  ItemListSearchParams,
  ImageAnalysisResult,
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

  exportCsv: async (id: string): Promise<{ blob: Blob; filename: string }> => {
    const response = await http.get(`/lists/${id}/export`, {
      responseType: 'blob',
    });

    let filename = 'export.csv';
    const disposition = response.headers['content-disposition'];
    if (disposition) {
      // Try RFC 5987 filename* first, then plain filename
      const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/i);
      const plainMatch = disposition.match(/filename="?([^";\n]+)"?/i);
      const extracted = utf8Match?.[1] ?? plainMatch?.[1];
      if (extracted) {
        filename = decodeURIComponent(extracted.trim());
      }
    }

    return { blob: response.data as Blob, filename };
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

  bulkDelete: async (ids: string[]): Promise<void> => {
    await http.post('/items/bulk-delete', { ids });
  },

  getByBarcode: async (barcode: string, signal?: AbortSignal): Promise<Item | null> => {
    try {
      const response = await http.get<Item>(`/items/barcode/${encodeURIComponent(barcode)}`, {
        signal,
      });
      return response.data;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 404) {
        return null;
      }
      throw error;
    }
  },
};

export const imageAnalysisApi = {
  checkStatus: async (): Promise<{ available: boolean }> => {
    const response = await http.get<{ available: boolean }>('/items/analyze-image/status');
    return response.data;
  },

  analyze: async (image: File, listId?: string): Promise<{ analysisId: string }> => {
    const formData = new FormData();
    formData.append('image', image);
    if (listId) formData.append('listId', listId);
    const response = await http.post<{ analysisId: string }>('/items/analyze-image', formData);
    return response.data;
  },

  getResult: async (analysisId: string): Promise<ImageAnalysisResult> => {
    const response = await http.get<ImageAnalysisResult>(`/items/analyze-image/${analysisId}`);
    return response.data;
  },
};

export const dashboardApi = {
  getStats: async (signal?: AbortSignal): Promise<DashboardStats> => {
    const response = await http.get<DashboardStats>('/items/stats', { signal });
    return response.data;
  },
};
