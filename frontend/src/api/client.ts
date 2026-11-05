import axios from 'axios'
import { useAuthStore } from '@/store/authStore'

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

// Attach JWT on every request
client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On 401 → clear auth and redirect to login
client.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      useAuthStore.getState().clear()
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

export default client
