# syntax=docker/dockerfile:1
# ---------- Build stage ----------
FROM node:20-alpine@sha256:fb4cd12c85ee03686f6af5362a0b0d56d50c58a04632e6c0fb8363f609372293 AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci

COPY . .

ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=$BACKEND_URL

RUN npm run build

# ---------- Runtime stage ----------
FROM node:20-alpine@sha256:fb4cd12c85ee03686f6af5362a0b0d56d50c58a04632e6c0fb8363f609372293
WORKDIR /app
ENV NODE_ENV=production

# node:alpine ships a built-in non-root "node" user (uid/gid 1000) - use it
# instead of running the server as root.
RUN mkdir -p .next/static && chown -R node:node .

# output: 'standalone' (next.config.ts) traces only the deps actually needed
# at runtime into .next/standalone/node_modules - no npm ci / full
# node_modules copy needed here, which is what kept devDependencies
# (typescript, eslint, tailwind, ...) out of the runtime image.
COPY --from=build --chown=node:node /app/public ./public
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static

USER node
EXPOSE 3000

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=5 \
    CMD node -e "fetch('http://localhost:3000/').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"

CMD ["node", "server.js"]