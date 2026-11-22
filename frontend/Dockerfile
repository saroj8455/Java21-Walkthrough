# ---------- Stage 1: build the Angular app ----------
# Angular 17 supports Node ^18.13 || ^20.9, so we pin Node 20 for the build.
FROM node:20-alpine AS build
WORKDIR /app

# Install deps first (cached layer while package*.json is unchanged).
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY . .
RUN npm run build -- --configuration production

# ---------- Stage 2: serve with nginx ----------
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
# The "application" builder outputs to dist/<project>/browser
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
