# =============================================================================
# SmartProcure Frontend – Multi-Stage Docker Build
# Node 20 LTS  |  Vite + React  |  Nginx runtime
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: Build  (Node 20 Alpine)
# -----------------------------------------------------------------------------
FROM node:20-alpine AS builder

WORKDIR /app

# 1. Install dependencies (cached layer — only invalidated when package.json changes)
COPY package*.json ./
RUN npm ci --silent

# 2. Copy source and build
COPY . .
RUN npm run build

# -----------------------------------------------------------------------------
# Stage 2: Runtime  (Nginx Alpine – minimal, non-root)
# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy built assets from builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Non-root nginx: nginx master still needs root to bind port 80,
# so we expose 80 and let docker handle port mapping
EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget -q --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
