# Stage 1: fetch data + build the static site
FROM node:20-alpine AS builder
WORKDIR /app

# Install deps (cached layer)
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Non-secret config is read from public/app-config.json (already COPY'd above).
# Override by bind-mounting your own file before building:
#   docker build --build-context config=path/to/my-app-config.json ...
# or by replacing public/app-config.json in your fork before docker build.
#
# Secrets must be provided as build-time env vars — they are never stored in
# public/app-config.json.
ARG IAM_CLIENT_SECRET
ARG RAID_DUMPER_CLIENT_SECRET
ENV IAM_CLIENT_SECRET=$IAM_CLIENT_SECRET \
    RAID_DUMPER_CLIENT_SECRET=$RAID_DUMPER_CLIENT_SECRET

# Fetch RAiD data then build the static site
RUN npm run build

# Stage 2: serve the pre-built static files
FROM nginx:alpine
RUN apk add --no-cache gettext

COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx/nginx.conf /etc/nginx/conf.d/default.conf
COPY nginx/nginx.conf.template /etc/nginx/templates/nginx.conf.template
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/docker-entrypoint.sh"]
