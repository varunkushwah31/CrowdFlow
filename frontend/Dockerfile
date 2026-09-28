# ==============================================================================
# CrowdFlow Frontend - Multi-Stage Production Build (Node 22 + Nginx Alpine)
# ==============================================================================

# Stage 1: Build React TypeScript Bundle
FROM node:22-alpine AS builder

WORKDIR /app

# Copy dependency manifests
COPY package*.json ./

# Install all dependencies
RUN npm ci

# Copy source and config
COPY . .

# Compile TypeScript and Vite production bundle
RUN npm run build

# Stage 2: Hardened, Minimal Production Nginx Runtime
FROM nginx:alpine

LABEL org.opencontainers.image.title="CrowdFlow React TypeScript Frontend" \
      org.opencontainers.image.description="Dedicated React TypeScript frontend client for WaterWatch India" \
      org.opencontainers.image.version="1.0.0"

# Support running as unprivileged user (UID/GID nginx)
RUN apk add --no-cache libcap && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /var/log/nginx /etc/nginx/conf.d && \
    setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx

# Copy Nginx configuration template
COPY --chown=nginx:nginx nginx.conf /etc/nginx/templates/default.conf.template

# Copy built React application assets from builder stage
COPY --chown=nginx:nginx --from=builder /app/dist /usr/share/nginx/html

USER nginx

EXPOSE 80

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
    CMD wget -qO- http://127.0.0.1/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
