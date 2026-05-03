# 短链系统

基于 Spring Boot 实现的短链接服务。系统支持长链接生成短码、短链 302 跳转、过期控制、启用禁用、访问限流、Redis 缓存、RabbitMQ 异步访问日志、访问统计和分页查询。


## 技术栈

- Java 17
- Spring Boot 4
- Spring MVC
- MyBatis
- MySQL
- Redis
- RabbitMQ
- Maven
- Lombok
- Jakarta Validation

## 核心功能

- 创建短链：提交原始 URL，生成唯一短码并保存到 MySQL。
- 短链跳转：访问短码地址后返回 HTTP 302，跳转到原始 URL。
- 过期控制：支持设置短链过期时间，过期后返回业务异常。
- 状态控制：支持启用、禁用短链。
- Redis 缓存：缓存短码和原始 URL 的映射，降低数据库查询压力。
- 空链缓存：不存在的短码短时间缓存特殊值，减少恶意短码穿透数据库。
- 访问限流：基于 Redis 统计短时间内的访问次数，限制高频请求。
- Redis 降级：Redis 操作失败时记录日志，主流程继续回源 MySQL。
- RabbitMQ 异步日志：跳转请求只投递访问日志消息，消费者异步写入访问记录。
- 访问统计：支持总访问量、今日访问量、最近访问日志查询。
- 统一响应：接口返回统一 `ApiResponse` 结构。
- 全局异常：业务异常、参数校验异常、系统异常统一处理。

## 项目结构

```text
src/main/java/com/hstk/shortlink
├── ShortLinkSystemApplication.java
├── common
│   ├── ApiResponse.java
│   ├── BusinessException.java
│   ├── GlobalExceptionHandler.java
│   └── PageResult.java
├── config
│   └── RabbitMqConfig.java
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
│   ├── entity
│   │   ├── ShortLink.java
│   │   └── ShortLinkVisitLog.java
│   └── message
│       └── VisitLogMessage.java
├── mq
│   ├── VisitLogProducer.java
│   └── VisitLogConsumer.java
└── service
    ├── ShortLinkService.java
    ├── VisitLogService.java
    └── impl
        └── ShortLinkServiceImpl.java
```

## 核心链路

### 创建短链

```text
客户端提交原始 URL
-> 生成 6 位短码
-> 检查短码是否已存在
-> 写入 short_link 表
-> 返回 shortCode 和 shortUrl
```

### 短链跳转

```text
访问 /{shortCode}
-> Redis 限流
-> 查询 Redis 缓存
-> 缓存命中：返回原始 URL
-> 缓存未命中：查询 MySQL
-> 校验状态和过期时间
-> 写入 Redis 缓存
-> 投递访问日志消息到 RabbitMQ
-> 返回 302 跳转
```

### 访问日志

```text
跳转接口
-> VisitLogProducer 投递消息
-> RabbitMQ 队列
-> VisitLogConsumer 消费消息
-> VisitLogService 更新访问次数
-> 写入 short_link_visit_log 表
```

当前访问日志异步化使用 RabbitMQ，线程池异步方案不再作为主链路。

## 本地环境

需要提前安装并启动：

- JDK 17
- Maven
- MySQL
- Redis
- RabbitMQ

本地默认端口：

```text
MySQL:    127.0.0.1:3306
Redis:    127.0.0.1:6379
RabbitMQ: 127.0.0.1:5672
管理后台: http://localhost:15672
```

启动本地服务：

```bash
brew services start mysql
brew services start redis
brew services start rabbitmq
```

检查 Redis：

```bash
redis-cli -h 127.0.0.1 -p 6379 ping
```

期望输出：

```text
PONG
```

检查 RabbitMQ：

```bash
rabbitmqctl status
```

如果 `rabbitmqctl` 找不到，可以使用 Homebrew 路径：

```bash
/opt/homebrew/sbin/rabbitmqctl status
```

开启 RabbitMQ 管理后台：

```bash
rabbitmq-plugins enable rabbitmq_management
```

## 配置说明

项目通过根目录下的 `.env` 文件读取本地配置。`.env` 不要提交到 Git。

可以在项目根目录创建 `.env`：

```properties
DB_HOST=127.0.0.1
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=change_me

REDIS_HOST=127.0.0.1
REDIS_PORT=6379

RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
```

`application.properties` 中通过占位符读取配置：

```properties
spring.config.import=optional:file:.env[.properties]

spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/short_link_system?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}

spring.rabbitmq.host=${RABBITMQ_HOST:localhost}
spring.rabbitmq.port=${RABBITMQ_PORT:5672}
spring.rabbitmq.username=${RABBITMQ_USERNAME:guest}
spring.rabbitmq.password=${RABBITMQ_PASSWORD:guest}
```

## 数据库

创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS short_link_system
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

创建短链表：

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

创建访问日志表：

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

推荐索引：

```sql
CREATE INDEX idx_short_link_visit_log_code_time
ON short_link_visit_log(short_code, create_time);
```

## Redis 设计

### 短链跳转缓存

```text
key:   shortlink:redirect:{shortCode}
value: originalUrl
ttl:   未过期短链按过期时间设置；永久短链默认缓存 24 小时
```

作用：短链跳转是高频读场景，优先查 Redis，减少 MySQL 压力。

### 空链缓存

```text
key:   shortlink:redirect:{shortCode}
value: __NULL__
ttl:   5 分钟
```

作用：当短码不存在时，也写入一个短时间缓存。下次再访问同一个不存在的短码，可以直接从 Redis 判断不存在，避免反复打到数据库。

### 访问限流

```text
key:   shortlink:rate:{shortCode}:{ip}
value: 当前 1 秒内访问次数
ttl:   1 秒
```

作用：限制同一个 IP 对同一个短码的高频访问，超过阈值后返回访问过于频繁。

## RabbitMQ 设计

RabbitMQ 用于异步处理访问日志，避免跳转接口直接等待数据库写日志完成。

```text
exchange:    shortlink.visit.exchange
queue:       shortlink.visit.log.queue
routing key: shortlink.visit.log
```

生产者：

```text
VisitLogProducer
-> rabbitTemplate.convertAndSend(exchange, routingKey, message)
```

消费者：

```text
VisitLogConsumer
-> @RabbitListener 监听队列
-> 调用 VisitLogService.recordVisit
-> 更新 visit_count
-> 插入 short_link_visit_log
```

消息对象使用 JSON 转换器，避免 Java 原生序列化带来的安全限制和跨版本问题。

如果修改过消息转换器后，队列里还残留旧的 Java 序列化消息，可以清空队列：

```bash
rabbitmqctl purge_queue shortlink.visit.log.queue
```

## 启动项目

确认 MySQL、Redis、RabbitMQ 已启动，并且 `.env` 已配置。

运行：

```bash
./mvnw spring-boot:run
```

服务地址：

```text
http://localhost:8080
```

运行测试：

```bash
./mvnw test
```

## 接口说明

### 1. 创建短链

```http
POST /api/short-links
Content-Type: application/json
```

请求示例：

```json
{
  "originalUrl": "https://www.baidu.com",
  "expireTime": "2026-05-01 12:00:00"
}
```

`expireTime` 可选。不传时表示不主动过期。

响应示例：

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

### 2. 短链跳转

```http
GET /{shortCode}
```

示例：

```text
GET http://localhost:8080/abc123
```

短链存在、启用且未过期时，返回：

```http
302 Found
Location: https://www.baidu.com
```

### 3. 查询短链详情

```http
GET /api/short-links/{shortCode}
```

响应示例：

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

### 4. 修改短链状态

```http
PATCH /api/short-links/{shortCode}/status
Content-Type: application/json
```

请求示例：

```json
{
  "status": 0
}
```

状态说明：

```text
1: 启用
0: 禁用
```

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 5. 查询访问日志

```http
GET /api/short-links/{shortCode}/visits?limit=20
```

`limit` 可选，默认 20，最大 100。

响应示例：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "shortCode": "abc123",
      "ip": "127.0.0.1",
      "userAgent": "Mozilla/5.0",
      "referer": null,
      "createTime": "2026-04-30T13:20:00"
    }
  ]
}
```

### 6. 分页查询短链

```http
GET /api/short-links?page=1&pageSize=10&status=1
```

参数说明：

```text
page:     页码，默认 1
pageSize: 每页数量，默认 10，最大 100
status:   可选，1 表示启用，0 表示禁用
```

响应示例：

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

### 7. 查询访问统计

```http
GET /api/short-links/{shortCode}/stats
```

响应示例：

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

## 错误响应

项目使用统一响应结构返回业务错误。

示例：

```json
{
  "code": 404,
  "message": "短链不存在",
  "data": null
}
```

常见错误码：

```text
400: 请求参数错误
403: 短链已禁用
404: 短链不存在
410: 短链已过期
429: 访问过于频繁
500: 系统异常
```

## 项目亮点

- 使用 Redis 缓存短链映射，优化高频跳转场景下的读性能。
- 使用空链缓存降低不存在短码对数据库的穿透压力。
- 使用 Redis 实现简单限流，保护短链跳转接口。
- Redis 操作增加异常捕获，Redis 故障时主链路可回源 MySQL。
- 使用 RabbitMQ 解耦跳转流程和访问日志写入，提高跳转接口响应速度。
- 使用统一响应结构和全局异常处理，提高接口可维护性。
- 使用 MyBatis XML 管理 SQL，便于针对访问统计场景优化查询。

## 简历描述参考

```text
基于 Spring Boot + MyBatis + MySQL 实现短链系统，支持短链创建、302 跳转、过期控制、启用禁用、访问统计和分页查询。

引入 Redis 缓存短链映射，设计空链缓存和基于短码/IP 的访问限流，并对 Redis 异常进行降级处理，保证缓存故障时主流程可回源数据库。

引入 RabbitMQ 异步处理访问日志，将跳转请求与数据库写日志解耦，消费者异步更新访问次数并写入访问日志表，降低跳转接口耗时。

设计统一返回结构、参数校验和全局异常处理机制，提升接口响应一致性和系统可维护性。
```
