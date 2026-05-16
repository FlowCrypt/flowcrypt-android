FROM nginx:1.27-alpine

COPY docker/HttpsTestWebServer/nginx.conf /etc/nginx/conf.d/default.conf

COPY FlowCrypt/src/androidTest/resources/ssl/server_combined.pem \
     /etc/nginx/certs/server_combined.pem

EXPOSE 443