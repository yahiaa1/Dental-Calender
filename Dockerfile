# Multi-stage build for Dental Calendar Web Portal
FROM node:20-alpine AS build

WORKDIR /app

# Copy dependency manifests
COPY web/package*.json ./
RUN npm ci

# Copy web source files and build
COPY web/ ./
RUN npm run build

# Production server stage with Nginx
FROM nginx:alpine

# Copy Nginx SPA configuration
COPY web/nginx.conf /etc/nginx/conf.d/default.conf

# Copy production build output
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
