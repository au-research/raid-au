FROM nginxinc/nginx-unprivileged:stable-alpine
COPY dist/ /usr/share/nginx/html
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]