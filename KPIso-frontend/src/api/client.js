import axios from 'axios';

const api = axios.create({
  // Utiliza la variable inyectada por Docker o cae en el puerto local por defecto
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json'
  }
});

// Interceptor para añadir el token de seguridad automáticamente en cada llamada futura
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;