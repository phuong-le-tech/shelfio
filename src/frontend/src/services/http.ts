import axios from 'axios';

const http = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
});

// Unwrap Google JSON Style Guide response envelope and redirect on 401
http.interceptors.response.use(
  (response) => {
    if (response.data && typeof response.data === 'object' && 'data' in response.data) {
      response.data = response.data.data;
    }
    return response;
  },
  (error) => {
    if (error.response?.status === 401 && !window.location.pathname.includes('/login')) {
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default http;
