ARG DOCKER_REGISTRY=docker.io/

FROM ${DOCKER_REGISTRY}node:18-alpine AS builder

ARG DOCKER_REGISTRY
ENV DOCKER_REGISTRY=${DOCKER_REGISTRY}

WORKDIR /app

COPY .npmrc ./

COPY package.json package-lock.json* ./

RUN npm ci --no-cache

COPY . .

RUN npm run build

FROM ${DOCKER_REGISTRY}nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
