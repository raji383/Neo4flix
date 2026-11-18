# syntax=docker/dockerfile:1.7

FROM node:24.22.3-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked npm ci

COPY . .
RUN npm run build

FROM nginx:alpine

COPY --from=build /app/dist/neo4flix-frontend/browser/ /usr/share/nginx/html/
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 4000
