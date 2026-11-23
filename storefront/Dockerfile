# ---- Stage 1: build the app ----
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Stage 2: serve it with nginx (runs as a non-root user) ----
FROM nginxinc/nginx-unprivileged:1.27-alpine
ENV GATEWAY_URL=http://gateway:8080 \
    LIVE_URL=http://live-service:8085
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080