#!/bin/sh
set -e

if [ -n "$APP_CONFIG_URL" ]; then
  echo "[entrypoint] APP_CONFIG_URL set — proxying /app-config.json to $APP_CONFIG_URL"
  envsubst '${APP_CONFIG_URL}' \
    < /etc/nginx/templates/nginx.conf.template \
    > /etc/nginx/conf.d/default.conf
else
  echo "[entrypoint] No APP_CONFIG_URL — serving /app-config.json from disk"
fi

exec nginx -g "daemon off;"
