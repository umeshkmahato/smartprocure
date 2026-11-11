import client from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, ApiResponse } from '@/types'
import { mockAuthApi } from '@/mocks/auth.mock'

const USE_MOCK = import.meta.env.VITE_USE_MOCK_DATA === 'true'

export const authApi = USE_MOCK ? mockAuthApi : {
  login: (req: LoginRequest) =>
    client.post<ApiResponse<AuthResponse>>('/auth/login', req).then((r) => r.data.data),

  register: (req: RegisterRequest) =>
    client.post<ApiResponse<AuthResponse>>('/auth/register', req).then((r) => r.data.data),

  refresh: (refreshToken: string) =>
    client
      .post<ApiResponse<AuthResponse>>('/auth/refresh', { refreshToken })
      .then((r) => r.data.data),

  logout: () => client.post('/auth/logout'),
}
