import http from './http';

export const stripeApi = {
  createCheckoutSession: async (): Promise<{ url: string }> => {
    const response = await http.post('/stripe/checkout');
    return response.data;
  },

  getPaymentStatus: async (): Promise<{ premium: boolean }> => {
    const response = await http.get('/stripe/status');
    return response.data;
  },
};
