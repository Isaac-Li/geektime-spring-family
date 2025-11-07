package geektime.spring.springbucks.waiter.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ConcurrentPerformanceTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final OkHttpClient client = new OkHttpClient();
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
            System.out.println("使用: java ConcurrentPerformanceTest <threads> <requestsPerThread> <endpoint> [params]");
        } else {
            threads = Integer.parseInt(args[0]);
            requestsPerThread = Integer.parseInt(args[1]);
            endpoint = args[2];
            params = Arrays.asList(args).subList(3, args.length);
        }

        System.out.printf("=== 性能测试配置 ===\n");
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
        System.out.printf("=== 性能测试结果 ===\n");
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

    private static void executeRequest(String endpoint, List<String> params) throws IOException {
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

    private static void getCoffeeAll() throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "/coffee").build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void getCoffeeById(long id) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "/coffee/" + id).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void getCoffeeByName(String name) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/coffee").newBuilder()
                .addQueryParameter("name", name)
                .build();
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void postCoffeeJson(String name, Money price) throws IOException {
        Map<String, Object> coffee = new HashMap<>();
        coffee.put("name", name);
        coffee.put("price", price);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(coffee));
        Request request = new Request.Builder()
                .url(BASE_URL + "/coffee")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void postCoffeeForm(String name, Money price) throws IOException {
        FormBody formBody = new FormBody.Builder()
                .add("name", name)
                .add("price", price.toString())
                .build();
        Request request = new Request.Builder()
                .url(BASE_URL + "/coffee")
                .post(formBody)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void postCoffeeBatch(List<String> params) throws IOException {
        StringBuilder fileContent = new StringBuilder();
        for (String param : params) {
            fileContent.append(param).append("\n");
        }

        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "coffees.txt",
                        RequestBody.create(MediaType.parse("text/plain"), fileContent.toString()))
                .build();
        Request request = new Request.Builder()
                .url(BASE_URL + "/coffee")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void getOrderById(long id) throws IOException {
        Request request = new Request.Builder().url(BASE_URL + "/order/" + id).build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }

    private static void postOrder(String customer, List<String> items) throws IOException {
        Map<String, Object> order = new HashMap<>();
        order.put("customer", customer);
        order.put("items", items);

        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(JSON, objectMapper.writeValueAsString(order));
        Request request = new Request.Builder()
                .url(BASE_URL + "/order")
                .post(body)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("请求失败: " + response.code());
        }
    }
}