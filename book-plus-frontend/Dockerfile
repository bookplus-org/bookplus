# ── Stage 1: build the Angular app ────────────────────────────────────────
FROM node:20-alpine AS build
WORKDIR /app

# Install dependencies (cached layer)
COPY package.json package-lock.json* ./
RUN npm ci || npm install

# Build for production
COPY . .
RUN npm run build

# ── Stage 2: serve with Nginx ─────────────────────────────────────────────
FROM nginx:1.27-alpine
# Angular's application builder emits to dist/<project>/browser
COPY --from=build /app/dist/book-plus-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1
CMD ["nginx", "-g", "daemon off;"]
