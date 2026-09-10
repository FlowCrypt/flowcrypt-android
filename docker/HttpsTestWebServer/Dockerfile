FROM nginx:1.27-alpine

RUN apk add --no-cache dnsmasq bind-tools iputils

COPY docker/HttpsTestWebServer/nginx.conf /etc/nginx/conf.d/default.conf

COPY FlowCrypt/src/androidTest/resources/ssl/server_combined.pem \
     /etc/nginx/certs/server_combined.pem

COPY docker/HttpsTestWebServer/dnsmasq.conf /etc/dnsmasq.d/flowcrypt-test.conf
COPY docker/HttpsTestWebServer/entrypoint.sh /entrypoint.sh

RUN chmod +x /entrypoint.sh

EXPOSE 53/udp 53/tcp 443

ENTRYPOINT ["/entrypoint.sh"]
