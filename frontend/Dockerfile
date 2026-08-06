# Build stage
FROM node:22-alpine AS build

WORKDIR /app

# Install dependencies first to maximize Docker layer caching
COPY package*.json ./
RUN npm ci

# Build the Vite app
COPY . .
ARG VITE_API_BASE_URL=/
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

# Runtime stage
FROM nginx:1.27-alpine AS runtime

# Replace default nginx config with SPA + API proxy config
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
