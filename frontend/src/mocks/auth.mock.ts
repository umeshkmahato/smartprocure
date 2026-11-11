import type { AuthResponse, LoginRequest, RegisterRequest } from '@/types'
import { delay } from './utils'

export const mockAuthApi = {
  login: async (req: LoginRequest): Promise<AuthResponse> => {
    await delay(500)
    if (!req.username?.trim() || !req.password?.trim()) {
      throw Object.assign(new Error('Username and password are required'), {
        response: { data: { message: 'Username and password are required' } },
      })
    }
    return {
      accessToken: `mock-access-${Date.now()}`,
      refreshToken: `mock-refresh-${Date.now()}`,
      username: req.username,
      email: `${req.username}@smartprocure.dev`,
      roles: ['ROLE_ADMIN'],
    }
  },

  register: async (req: RegisterRequest): Promise<AuthResponse> => {
    await delay(500)
    if (!req.username?.trim() || !req.email?.trim() || !req.password?.trim()) {
      throw Object.assign(new Error('All fields are required'), {
        response: { data: { message: 'All fields are required' } },
      })
    }
    return {
      accessToken: `mock-access-${Date.now()}`,
      refreshToken: `mock-refresh-${Date.now()}`,
      username: req.username,
      email: req.email,
      roles: ['ROLE_USER'],
    }
  },

  refresh: async (): Promise<AuthResponse> => {
    await delay(200)
    return {
      accessToken: `mock-access-refreshed-${Date.now()}`,
      refreshToken: `mock-refresh-${Date.now()}`,
      username: 'admin',
      email: 'admin@smartprocure.dev',
      roles: ['ROLE_ADMIN'],
    }
  },

  logout: async (): Promise<void> => {
    await delay(200)
  },
}
