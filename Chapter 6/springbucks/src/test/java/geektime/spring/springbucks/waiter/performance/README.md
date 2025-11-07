# Springbucks 性能测试工具

## 简介

本目录包含两个性能测试工具，用于测试 Springbucks 项目的接口性能。

## 工具列表

### 1. PerformanceTest

基于定时任务的性能测试工具，可以按照指定的每秒请求数发送请求。

#### 功能特点
- 可以配置每秒请求次数
- 可以配置测试持续时间
- 支持所有接口
- 提供基本的统计信息

#### 使用方法

```bash
java PerformanceTest <requestsPerSecond> <durationSeconds> <endpoint> [params]
```

#### 示例

```bash
# 每秒发送10个请求，持续60秒，测试获取所有咖啡接口
java PerformanceTest 10 60 coffee-get-all

# 每秒发送5个请求，持续30秒，测试根据ID获取咖啡接口
java PerformanceTest 5 30 coffee-get-by-id 1

# 每秒发送20个请求，持续10秒，测试创建咖啡接口
java PerformanceTest 20 10 coffee-post-json "Latte" 30.0
```

### 2. ConcurrentPerformanceTest

基于固定线程池的性能测试工具，可以并发发送请求。

#### 功能特点
- 可以配置并发线程数
- 可以配置每个线程发送的请求数
- 支持所有接口
- 提供详细的统计信息，包括响应时间分布

#### 使用方法

```bash
java ConcurrentPerformanceTest <threads> <requestsPerThread> <endpoint> [params]
```

#### 示例

```bash
# 使用10个线程，每个线程发送100个请求，测试获取所有咖啡接口
java ConcurrentPerformanceTest 10 100 coffee-get-all

# 使用5个线程，每个线程发送50个请求，测试创建订单接口
java ConcurrentPerformanceTest 5 50 order-post "张三" "Latte,Americano"

# 使用20个线程，每个线程发送200个请求，测试批量上传咖啡接口
java ConcurrentPerformanceTest 20 200 coffee-post-batch "Latte 30.0" "Americano 25.0" "Cappuccino 35.0"
```

## 支持的端点

### Coffee 接口
- `coffee-get-all`: GET /coffee
- `coffee-get-by-id`: GET /coffee/{id}
- `coffee-get-by-name`: GET /coffee?name=...
- `coffee-post-json`: POST /coffee (JSON格式)
- `coffee-post-form`: POST /coffee (表单格式)
- `coffee-post-batch`: POST /coffee (批量上传)

### Order 接口
- `order-get-by-id`: GET /order/{id}
- `order-post`: POST /order

## 构建和运行

### 构建项目

```bash
cd /Users/Cynthia/Documents/workspace/TRAE/geektime-spring-family/Chapter 6/springbucks
mvn clean package -DskipTests
```

### 启动应用

```bash
java -jar target/waiter-service-0.0.1-SNAPSHOT.jar
```

### 运行性能测试

使用 IDE 运行测试类，或者使用以下命令：

```bash
# 运行 ModernPerformanceTest
java -cp "target/waiter-service-0.0.1-SNAPSHOT.jar:target/lib/*" geektime.spring.springbucks.waiter.performance.ModernPerformanceTest 10 100 coffee-get-all
```

## 统计信息

### PerformanceTest
- 总请求数
- 成功请求数和成功率
- 失败请求数和失败率
- 平均每秒请求数

### ConcurrentPerformanceTest
- 总耗时
- 总请求数
- 成功请求数和成功率
- 失败请求数和失败率
- 平均响应时间
- 吞吐量（请求/秒）
- 响应时间分布（50%、90%、95%、99%）

## 注意事项

1. 在运行性能测试之前，请确保 Springbucks 应用已经启动，并且可以正常访问。
2. 建议在测试环境中运行性能测试，避免影响生产环境。
3. 可以根据实际情况调整并发线程数和请求数，以模拟真实的负载情况。
4. 对于批量上传接口，参数格式为 "咖啡名称 价格"，例如 "Latte 30.0"。
5. 对于创建订单接口，商品列表使用逗号分隔，例如 "Latte,Americano"。
