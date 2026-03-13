import http from './http';

interface CheckoutRequest {
  withdrawalWaiverAccepted: boolean;
}

export const stripeApi = {
  createCheckoutSession: async (data: CheckoutRequest): Promise<{ url: string }> => {
    const response = await http.post('/stripe/checkout', data);
    return response.data;
  },
};
