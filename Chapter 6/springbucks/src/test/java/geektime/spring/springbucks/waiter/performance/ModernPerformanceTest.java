package geektime.spring.springbucks.waiter.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ModernPerformanceTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger successRequests = new AtomicInteger(0);
    private static final AtomicInteger failedRequests = new AtomicInteger(0);
    private static final AtomicLong totalResponseTime = new AtomicLong(0);
    private static final List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws InterruptedException, IOException {
        // 默认参数
        int threads = 10;
        int requestsPerThread = 100;
        String endpoint = "coffee-get-all";
        List<String> params = new ArrayList<>();

        // 解析命令行参数
        if (args.length < 1) {
            System.out.println("使用默认参数运行测试...");
            System.out.println("默认: 10线程, 每个线程100请求, 端点: coffee-get-all");
            System.out.println("使用: java ModernPerformanceTest <threads> <requestsPerThread> <endpoint> [params]");
        } else {
            threads = Integer.parseInt(args[0]);
            requestsPerThread = Integer.parseInt(args[1]);
            endpoint = args[2];
            params = Arrays.asList(args).subList(3, args.length);
        }

        System.out.printf("=== 现代性能测试配置 ===\n");
        System.out.printf("并发线程数: %d\n", threads);
        System.out.printf("每个线程请求数: %d\n", requestsPerThread);
        System.out.printf("总请求数: %d\n", threads * requestsPerThread);
        System.out.printf("测试端点: %s\n", endpoint);
        System.out.printf("请求参数: %s\n", params);
        System.out.printf("====================\n");

        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        long startTime = System.currentTimeMillis();

        // 提交任务
        for (int i = 0; i < threads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < requestsPerThread; j++) {
                        long requestStartTime = System.nanoTime();
                        try {
                            executeRequest(endpoint, params);
                            long responseTime = (System.nanoTime() - requestStartTime) / 1000000;
                            totalResponseTime.addAndGet(responseTime);
                            responseTimes.add(responseTime);
                            successRequests.incrementAndGet();
                        } catch (Exception e) {
                            failedRequests.incrementAndGet();
                            System.err.printf("线程 %d 请求 %d 失败: %s%n", threadId, j, e.getMessage());
                        } finally {
                            totalRequests.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        // 等待所有线程完成
        latch.await();
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 计算统计数据
        double avgResponseTime = totalResponseTime.get() / (double) successRequests.get();
        double throughput = (totalRequests.get() * 1000.0) / totalTime;

        // 计算响应时间分布
        Collections.sort(responseTimes);
        long p50 = responseTimes.size() > 0 ? responseTimes.get((int) (responseTimes.size() * 0.5)) : 0;
        long p90 = responseTimes.size() > 0 ? responseTimes.get((int) (responseTimes.size() * 0.9)) : 0;
        long p95 = responseTimes.size() > 0 ? responseTimes.get((int) (responseTimes.size() * 0.95)) : 0;
        long p99 = responseTimes.size() > 0 ? responseTimes.get((int) (responseTimes.size() * 0.99)) : 0;

        // 打印结果
        System.out.printf("=== 现代性能测试结果 ===\n");
        System.out.printf("总耗时: %d ms\n", totalTime);
        System.out.printf("总请求数: %d\n", totalRequests.get());
        System.out.printf("成功请求: %d (%.2f%%)\n", successRequests.get(), (successRequests.get() * 100.0) / totalRequests.get());
        System.out.printf("失败请求: %d (%.2f%%)\n", failedRequests.get(), (failedRequests.get() * 100.0) / totalRequests.get());
        System.out.printf("平均响应时间: %.2f ms\n", avgResponseTime);
        System.out.printf("吞吐量: %.2f 请求/秒\n", throughput);
        System.out.printf("响应时间分布:\n");
        System.out.printf("  50%% 响应时间: %d ms\n", p50);
        System.out.printf("  90%% 响应时间: %d ms\n", p90);
        System.out.printf("  95%% 响应时间: %d ms\n", p95);
        System.out.printf("  99%% 响应时间: %d ms\n", p99);
        System.out.printf("====================\n");

        executor.shutdown();
    }

    private static void executeRequest(String endpoint, List<String> params) throws Exception {
        switch (endpoint) {
            case "coffee-get-all":
                getCoffeeAll();
                break;
            case "coffee-get-by-id":
                if (params.size() != 1) throw new IllegalArgumentException("需要参数: id");
                getCoffeeById(Long.parseLong(params.get(0)));
                break;
            case "coffee-get-by-name":
                if (params.size() != 1) throw new IllegalArgumentException("需要参数: name");
                getCoffeeByName(params.get(0));
                break;
            case "coffee-post-json":
                if (params.size() != 2) throw new IllegalArgumentException("需要参数: name price");
                postCoffeeJson(params.get(0), Money.of(CurrencyUnit.of("CNY"), Double.parseDouble(params.get(1))));
                break;
            case "coffee-post-form":
                if (params.size() != 2) throw new IllegalArgumentException("需要参数: name price");
                postCoffeeForm(params.get(0), Money.of(CurrencyUnit.of("CNY"), Double.parseDouble(params.get(1))));
                break;
            case "coffee-post-batch":
                if (params.size() < 1) throw new IllegalArgumentException("需要参数: coffee1 coffee2 ...");
                postCoffeeBatch(params);
                break;
            case "order-get-by-id":
                if (params.size() != 1) throw new IllegalArgumentException("需要参数: id");
                getOrderById(Long.parseLong(params.get(0)));
                break;
            case "order-post":
                if (params.size() < 2) throw new IllegalArgumentException("需要参数: customer items");
                String customer = params.get(0);
                List<String> items = Arrays.asList(params.get(1).split(","));
                postOrder(customer, items);
                break;
            default:
                throw new IllegalArgumentException("未知端点: " + endpoint);
        }
    }

    private static void getCoffeeAll() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coffee"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void getCoffeeById(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coffee/" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void getCoffeeByName(String name) throws Exception {
        String url = BASE_URL + "/coffee?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void postCoffeeJson(String name, Money price) throws Exception {
        Map<String, Object> coffee = new HashMap<>();
        coffee.put("name", name);
        coffee.put("price", price);

        String json = objectMapper.writeValueAsString(coffee);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coffee"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void postCoffeeForm(String name, Money price) throws Exception {
        String formData = "name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&price=" + URLEncoder.encode(price.toString(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coffee"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void postCoffeeBatch(List<String> params) throws Exception {
        // 创建multipart/form-data请求
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        StringBuilder content = new StringBuilder();
        content.append("--").append(boundary).append("\r\n");
        content.append("Content-Disposition: form-data; name=\"file\"; filename=\"coffees.txt\"\r\n");
        content.append("Content-Type: text/plain\r\n\r\n");
        for (String param : params) {
            content.append(param).append("\r\n");
        }
        content.append("\r\n--").append(boundary).append("--");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/coffee"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(content.toString()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void getOrderById(long id) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/order/" + id))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("请求失败: " + response.statusCode());
    }

    private static void postOrder(String customer, List<String> items) throws Exception {
        Map<String, Object> order = new HashMap<>();
        order.put("customer", customer);
        order.put("items", items);

        String json = objectMapper.writeValueAsString(order);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/order"))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) throw new IOException("请求失败: " + response.statusCode());
    }
}