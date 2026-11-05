import client from './client'
import type { AuthResponse, LoginRequest, RegisterRequest, ApiResponse } from '@/types'

export const authApi = {
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
