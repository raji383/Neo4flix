# syntax=docker/dockerfile:1.7

FROM node:24.22.3-alpine AS build

WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm,sharing=locked npm ci

COPY . .
RUN npm run build

FROM node:24.22.3-alpine

ENV NODE_ENV=production \
    PORT=4000

WORKDIR /app
COPY --from=build --chown=node:node /app/dist ./dist

USER node
EXPOSE 4000

CMD ["node", "dist/neo4flix-frontend/server/server.mjs"]
