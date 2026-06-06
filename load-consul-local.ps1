# Script import cấu hình Frezo lên local Consul (Windows PowerShell)
$consulAddr = "http://localhost:8500"

Write-Host "=== Đang kiểm tra kết nối tới Consul tại $consulAddr ===" -ForegroundColor Cyan
try {
    $leader = Invoke-RestMethod -Uri "$consulAddr/v1/status/leader" -UseBasicParsing -ErrorAction Stop
    Write-Host "Consul đã sẵn sàng! Leader: $leader" -ForegroundColor Green
} catch {
    Write-Host "Không thể kết nối tới Consul tại $consulAddr. Vui lòng bật Consul trước!" -ForegroundColor Red
    Exit
}

# ─────────────────────────────────────────────────────────────────────────────
# Cấu hình Backend (frezo-server)
# ─────────────────────────────────────────────────────────────────────────────
$beConfig = @"
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/frezo
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
  url: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: frezo-files

app:
  security:
    encryption-key: CHANGE_ME_PLEASE_32PLUS_CHARS_123456

logging:
  level:
    com.frezo: INFO
"@

# ─────────────────────────────────────────────────────────────────────────────
# Cấu hình Gateway (frezo-gateway)
# ─────────────────────────────────────────────────────────────────────────────
$gwConfig = @"
spring:
  cloud:
    gateway:
      routes:
        - id: frezo-backend
          uri: lb://frezo-server
          predicates:
            - Path=/api/frezo/**
          filters:
            - RewritePath=/api/frezo/(?<segment>.*), /api/`${segment}

        - id: frezo-ai
          uri: http://localhost:8001
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
"@

Write-Host "Đang đẩy cấu hình backend..." -ForegroundColor Yellow
$resBE = Invoke-RestMethod -Uri "$consulAddr/v1/kv/config/frezo-server/data" -Method Put -Body $beConfig
if ($resBE) { Write-Host "Backend config -> OK!" -ForegroundColor Green }

Write-Host "Đang đẩy cấu hình gateway..." -ForegroundColor Yellow
$resGW = Invoke-RestMethod -Uri "$consulAddr/v1/kv/config/frezo-gateway/data" -Method Put -Body $gwConfig
if ($resGW) { Write-Host "Gateway config -> OK!" -ForegroundColor Green }

Write-Host "=== Hoàn thành! Kiểm tra cấu hình tại: $consulAddr/ui/dc1/kv/config/ ===" -ForegroundColor Green
