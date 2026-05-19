FROM node:20-alpine AS builder
WORKDIR /app
COPY package.json ./
RUN npm install --silent
COPY vite.config.js ./
COPY tsconfig.json ./
COPY src/ ./src/
COPY index.html ./
RUN npm run build

FROM nginx:1.25-alpine
COPY --from=builder /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 3000
CMD ["nginx", "-g", "daemon off;"]
