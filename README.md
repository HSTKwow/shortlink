# Short Link System

A Spring Boot backend project for generating and managing short links. It supports short link creation, 302 redirect, expiration control, enable/disable status, access logging, visit statistics, paginated management queries, Redis-based redirect cache, cache penetration protection, rate limiting, and asynchronous visit log recording.

## Tech Stack

- Java 17
- Spring Boot 4
- Spring MVC
- MyBatis
- MySQL
- Redis
- Validation
- Lombok
- Maven

## Features

- Create short links from original URLs.
- Redirect short links to original URLs with HTTP 302.
- Support optional expiration time for short links.
- Enable or disable short links by status.
- Validate request parameters with `@Valid`.
- Return unified API responses with `ApiResponse`.
- Handle business and validation errors through a global exception handler.
- Record visit logs including IP, User-Agent, Referer, and visit time.
- Query recent visit logs for a short link.
- Query short link statistics including total visits and today's visits.
- Query short link list with pagination and optional status filtering.
- Cache short link redirect mappings in Redis to reduce MySQL queries.
- Cache nonexistent short codes for a short time to reduce cache penetration.
- Use Redis `INCR` and TTL to limit frequent redirect requests from the same IP.
- Fall back to MySQL when Redis is unavailable, keeping redirect flow available.
- Record visit count and visit logs asynchronously with a dedicated thread pool.

## Project Structure

```text
src/main/java/com/hstk/shortlink
├── config
│   └── AsyncConfig.java
├── common
│   ├── ApiResponse.java
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   └── PageResult.java
├── controller
│   └── ShortLinkController.java
├── mapper
│   ├── ShortLinkMapper.java
│   └── ShortLinkVisitLogMapper.java
├── model
│   ├── dto
│   │   ├── CreateShortLinkRequest.java
│   │   ├── ShortLinkStatsResponse.java
│   │   └── UpdateShortLinkStatusRequest.java
│   └── entity
│       ├── ShortLink.java
│       └── ShortLinkVisitLog.java
└── service
    ├── VisitLogService.java
    ├── ShortLinkService.java
    └── impl
        └── ShortLinkServiceImpl.java
```

## Database

Create database:

```sql
CREATE DATABASE IF NOT EXISTS short_link_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

Create short link table:

```sql
CREATE TABLE short_link (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(32) NOT NULL UNIQUE,
    original_url TEXT NOT NULL,
    status TINYINT DEFAULT 1,
    expire_time DATETIME DEFAULT NULL,
    visit_count BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

Create visit log table:

```sql
CREATE TABLE short_link_visit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code VARCHAR(32) NOT NULL,
    ip VARCHAR(64),
    user_agent VARCHAR(512),
    referer VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

Recommended indexes:

```sql
CREATE INDEX idx_short_link_visit_log_code_time
ON short_link_visit_log(short_code, create_time);
```

## Configuration

The project reads the database password from environment variables.

`src/main/resources/application.properties`:

```properties
spring.application.name=short-link-system
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/short_link_system?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

mybatis.configuration.map-underscore-to-camel-case=true
mybatis.mapper-locations=classpath:mapper/*.xml

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
```

Set environment variables before running:

```bash
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

In IntelliJ IDEA, add the same variables in:

```text
Run/Debug Configurations -> Environment variables
```

Example:

```text
DB_USERNAME=root;DB_PASSWORD=your_mysql_password;REDIS_HOST=localhost;REDIS_PORT=6379
```

## Run

Start MySQL and Redis, then make sure the database tables have been created.

Check Redis:

```bash
redis-cli ping
```

Expected output:

```text
PONG
```

Then run:

```bash
./mvnw spring-boot:run
```

The service starts on:

```text
http://localhost:8080
```

## API

### Create Short Link

```http
POST /api/short-links
Content-Type: application/json
```

Request:

```json
{
  "originalUrl": "https://www.baidu.com",
  "expireTime": "2026-05-01 12:00:00"
}
```

`expireTime` is optional. If it is not provided, the short link does not expire.

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortCode": "abc123",
    "shortUrl": "http://localhost:8080/abc123"
  }
}
```

### Redirect

```http
GET /{shortCode}
```

Example:

```text
GET http://localhost:8080/abc123
```

If the short link exists, is enabled, and has not expired, the server returns:

```http
302 Found
Location: https://www.baidu.com
```

### Get Short Link Detail

```http
GET /api/short-links/{shortCode}
```

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "shortCode": "abc123",
    "originalUrl": "https://www.baidu.com",
    "status": 1,
    "expireTime": "2026-05-01T12:00:00",
    "visitCount": 3,
    "createTime": "2026-04-30T13:00:00",
    "updateTime": "2026-04-30T13:00:00"
  }
}
```

### Update Short Link Status

```http
PATCH /api/short-links/{shortCode}/status
Content-Type: application/json
```

Request:

```json
{
  "status": 0
}
```

Status values:

```text
1: enabled
0: disabled
```

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### Query Visit Logs

```http
GET /api/short-links/{shortCode}/visits?limit=20
```

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "shortCode": "abc123",
      "ip": "127.0.0.1",
      "userAgent": "Mozilla/5.0 ...",
      "referer": null,
      "createTime": "2026-04-30T13:20:00"
    }
  ]
}
```

### Query Short Link List

```http
GET /api/short-links?page=1&pageSize=10&status=1
```

`status` is optional.

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "shortCode": "abc123",
        "originalUrl": "https://www.baidu.com",
        "status": 1,
        "expireTime": "2026-05-01T12:00:00",
        "visitCount": 3,
        "createTime": "2026-04-30T13:00:00",
        "updateTime": "2026-04-30T13:00:00"
      }
    ],
    "total": 1,
    "page": 1,
    "pageSize": 10
  }
}
```

### Query Statistics

```http
GET /api/short-links/{shortCode}/stats
```

Response:

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "shortCode": "abc123",
    "originalUrl": "https://www.baidu.com",
    "status": 1,
    "totalVisitCount": 3,
    "todayVisitCount": 2
  }
}
```

## Error Response

The project uses a unified response structure for business and validation errors.

Example:

```json
{
  "code": 404,
  "message": "短链不存在",
  "data": null
}
```

Common error codes:

```text
400: invalid request parameter
403: short link disabled
404: short link not found
410: short link expired
429: too many redirect requests
500: server error
```

## Redis Design

### Redirect Cache

The redirect API is the highest-frequency read path in the system. The project uses Redis to cache short code mappings.

```text
key:   shortlink:redirect:{shortCode}
value: originalUrl
```

Flow:

```text
GET /{shortCode}
    -> check Redis
    -> cache hit: return originalUrl
    -> cache miss: query MySQL
    -> validate status and expiration
    -> write Redis cache
    -> return 302 Location
```

TTL strategy:

```text
short link with expireTime: Redis TTL aligns with expireTime
short link without expireTime: Redis TTL is set to 24 hours
```

### Null Cache

For nonexistent short codes, the project writes a short-lived null marker to Redis.

```text
key:   shortlink:redirect:{shortCode}
value: __NULL__
TTL:   5 minutes
```

This reduces repeated MySQL queries when users or scripts request nonexistent short codes.

### Rate Limiting

The redirect API uses Redis `INCR` and TTL for simple rate limiting.

```text
key: shortlink:rate:{shortCode}:{ip}
TTL: 1 second
limit: 10 requests per second
```

If the count exceeds the limit, the API returns:

```json
{
  "code": 429,
  "message": "访问过于频繁",
  "data": null
}
```

### Redis Fallback

Redis is treated as an optimization layer instead of a required dependency for redirect correctness.

```text
Redis get failed       -> fallback to MySQL
Redis set failed       -> skip cache write and continue redirect
Redis null-cache failed -> still return short link not found
Redis rate limit failed -> allow request
Redis delete failed    -> log warning and rely on TTL for eventual consistency
```

## Async Visit Log

Visit count increment and visit log insertion are handled asynchronously.

```text
GET /{shortCode}
    -> resolve originalUrl
    -> submit visit log task to visitLogExecutor
    -> return 302 immediately

visitLogExecutor
    -> increase visit_count
    -> insert short_link_visit_log
```

Thread pool:

```text
corePoolSize: 4
maxPoolSize: 8
queueCapacity: 1000
threadNamePrefix: visit-log-
```

This keeps the redirect path lightweight and prevents visit log writes from blocking the user-facing redirect response.

## Core Flow

```text
Create:
originalUrl -> generate shortCode -> save to MySQL -> return shortUrl

Redirect:
shortCode -> Redis rate limit -> query Redis -> fallback to MySQL if needed -> check status and expiration -> async visit log task -> return 302 Location

Statistics:
shortCode -> query short_link.visit_count -> count today's visit logs -> return stats response
```

## Roadmap

- Add Docker Compose for MySQL and Redis.
- Add request examples or API collection for easier testing.
- Add simple benchmark results for Redis hit and miss paths.
- Consider replacing thread pool async visit logging with RabbitMQ when stronger reliability is needed.

## Resume Description

```text
基于 Spring Boot + MyBatis 实现短链系统，支持短链创建、302 跳转、过期控制、启用禁用、访问日志、访问统计和分页查询。

基于 Redis 缓存短链码与原始链接映射，降低高频跳转场景下的数据库查询压力，并通过空值缓存处理不存在短链导致的缓存穿透问题。

基于 Redis INCR 与 TTL 实现短链跳转接口的简单限流，限制同一 IP 对同一短链的高频访问。

通过线程池异步记录访问日志，将访问次数更新和访问日志写入与短链跳转主链路解耦，降低跳转接口响应耗时。

设计统一返回结构、参数校验和全局异常处理机制，提升接口响应一致性和异常可维护性。
```
