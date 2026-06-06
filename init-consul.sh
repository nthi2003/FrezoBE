#!/bin/sh
export CONSUL_HTTP_ADDR="http://consul:8500"

echo "=== Frezo Consul Config Loader ==="
echo "Waiting for Consul to become ready..."
until consul kv put config/test-connection ok > /dev/null 2>&1; do
  sleep 1
done
consul kv delete config/test-connection
echo "Consul is ready!"

# ─────────────────────────────────────────────────────────────────────────────
# Backend (frezo-server) configuration
# ─────────────────────────────────────────────────────────────────────────────
echo "Loading Backend (frezo-server) config..."
consul kv put config/frezo-server/data - <<'EOF'
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/frezo
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  flyway:
    enabled: false
  cache:
    type: caffeine

server:
  port: 7410
  servlet:
    context-path: /api

minio:
  url: http://minio:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: frezo-files

app:
  security:
    encryption-key: CHANGE_ME_PLEASE_32PLUS_CHARS_123456

logging:
  level:
    com.frezo: INFO
EOF

# ─────────────────────────────────────────────────────────────────────────────
# Gateway (frezo-gateway) configuration
# ─────────────────────────────────────────────────────────────────────────────
echo "Loading Gateway (frezo-gateway) config..."
consul kv put config/frezo-gateway/data - <<'EOF'
spring:
  cloud:
    gateway:
      routes:
        - id: frezo-backend
          uri: lb://frezo-server
          predicates:
            - Path=/api/frezo/**
          filters:
            - RewritePath=/api/frezo/(?<segment>.*), /api/$\{segment}

        - id: frezo-ai
          uri: http://frezo-ai:8001
          predicates:
            - Path=/ai/**
          filters:
            - StripPrefix=1

      default-filters:
        - DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE

      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - DELETE
              - OPTIONS
              - PATCH
            allowedHeaders: "*"

app:
  gateway:
    backend-url: lb://frezo-server
    rate-limit:
      requests-per-minute: 100
    block:
      violation-threshold: 3
      block-duration-minutes: 30
      block-reason: RATE_LIMIT
    cache:
      blacklist-sync-interval-ms: 30000
      whitelist-sync-interval-ms: 30000

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
  endpoint:
    gateway:
      enabled: true

logging:
  level:
    com.frezo.gw: INFO
    org.springframework.cloud.gateway: INFO
EOF

echo ""
echo "=== All configurations loaded successfully! ==="
echo "  - config/frezo-server/data   (Backend)"
echo "  - config/frezo-gateway/data  (Gateway)"
echo ""
echo "Verify at: http://consul:8500/ui/dc1/kv/config/"
