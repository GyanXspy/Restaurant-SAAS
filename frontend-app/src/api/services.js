import { userClient, restaurantClient, cartClient, orderClient, paymentClient } from './client';

export const RestaurantService = {
  getRestaurants: () => restaurantClient.get('/restaurants'),
  getRestaurant: (id) => restaurantClient.get(`/restaurants/${id}`),
  getByCuisine: (cuisine) => restaurantClient.get(`/restaurants/cuisine/${cuisine}`),
  register: (data) => restaurantClient.post('/restaurants', data),
};

export const CartService = {
  getCart: (customerId) => cartClient.get(`/carts/${customerId}`),
  addItem: (customerId, item) => cartClient.post(`/carts/${customerId}/items`, item),
  updateItem: (customerId, itemId, quantity) => cartClient.put(`/carts/${customerId}/items/${itemId}?quantity=${quantity}`),
  removeItem: (customerId, itemId) => cartClient.delete(`/carts/${customerId}/items/${itemId}`),
  clearCart: (customerId) => cartClient.delete(`/carts/${customerId}`),
};

export const OrderService = {
  createOrder: (data) => orderClient.post('/orders', data),
  getOrder: (orderId) => orderClient.get(`/orders/${orderId}`),
  getCustomerOrders: (customerId) => orderClient.get(`/orders/customer/${customerId}`),
  confirmOrder: (orderId, paymentId) => orderClient.put(`/orders/${orderId}/confirm?paymentId=${paymentId}`),
};

export const PaymentService = {
  initiatePayment: (data) => paymentClient.post('/payments', data),
  completePayment: (paymentId, data) => paymentClient.post(`/payments/${paymentId}/complete`, data),
  getPaymentStatus: (paymentId) => paymentClient.get(`/payments/${paymentId}/status`),
};

export const UserService = {
  getUser: (userId) => userClient.get(`/users/${userId}`),
  getAllUsers: () => userClient.get('/users'),
  register: (data) => userClient.post('/users', data),
};
