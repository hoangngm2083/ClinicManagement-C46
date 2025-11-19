FROM redis:7-alpine
COPY init-redis.sh /usr/local/bin/

ENTRYPOINT ["/usr/local/bin/init-redis.sh"]

CMD ["redis-server", "/usr/local/etc/redis/redis.conf"]