FROM node:24.22.3-alpine AS build

WORKDIR /app

COPY package*.json ./

RUN npm ci --legacy-peer-deps

COPY . .

RUN npm run build -- --configuration production


FROM nginx:alpine

RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /app/dist/neo4flix-frontend/browser /usr/share/nginx/html

# SSL certificates
RUN mkdir -p /etc/nginx/ssl

COPY certs/cert.pem /etc/nginx/ssl/cert.pem
COPY certs/key.pem /etc/nginx/ssl/key.pem

EXPOSE 80 443

CMD ["nginx", "-g", "daemon off;"]