FROM redis:7-alpine

# Install dos2unix to convert line endings
RUN apk add --no-cache dos2unix

# Copy script and fix line endings
COPY init-redis.sh /usr/local/bin/
RUN dos2unix /usr/local/bin/init-redis.sh && \
    chmod +x /usr/local/bin/init-redis.sh

ENTRYPOINT ["/usr/local/bin/init-redis.sh"]

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]