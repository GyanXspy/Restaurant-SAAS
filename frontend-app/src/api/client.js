import axios from 'axios';

// Configure axios clients to use the Vite proxy
export const userClient = axios.create({ baseURL: '/api' });
export const restaurantClient = axios.create({ baseURL: '/api' });
export const cartClient = axios.create({ baseURL: '/api/v1' });
export const orderClient = axios.create({ baseURL: '/api' });
export const paymentClient = axios.create({ baseURL: '/api' });
