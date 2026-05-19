FROM nginxinc/nginx-unprivileged:stable-alpine
COPY dist/ /usr/share/nginx/html
COPY ./default.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]