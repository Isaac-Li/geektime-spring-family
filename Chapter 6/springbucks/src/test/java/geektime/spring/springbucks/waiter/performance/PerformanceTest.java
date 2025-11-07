package geektime.spring.springbucks.waiter.performance;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceTest {
    private static final String BASE_URL = "http://localhost:8080";
    private static final OkHttpClient client = new OkHttpClient();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AtomicInteger requestCount = new AtomicInteger(0);
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger failureCount = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.out.println("Usage: PerformanceTest <requestsPerSecond> <durationSeconds> <endpoint> [params]");
            System.out.println("Endpoints:");
            System.out.println("  coffee-get-all");
            System.out.println("  coffee-get-by-id <id>");
            System.out.println("  coffee-get-by-name <name>");
            System.out.println("  coffee-post-json <name> <price>");
            System.out.println("  coffee-post-form <name> <price>");
            System.out.println("  coffee-post-batch <coffee1> <coffee2> ...");
            System.out.println("  order-get-by-id <id>");
            System.out.println("  order-post <customer> <item1>,<item2>,...");
            return;
        }

        int requestsPerSecond = Integer.parseInt(args[0]);
        int durationSeconds = Integer.parseInt(args[1]);
        String endpoint = args[2];
        List<String> params = Arrays.asList(args).subList(3, args.length);

        System.out.printf("Starting performance test: %d requests/second for %d seconds to %s%n",
                requestsPerSecond, durationSeconds, endpoint);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(requestsPerSecond);
        long startTime = System.currentTimeMillis();

        // Schedule requests
        for (int i = 0; i < requestsPerSecond; i++) {
            final int delay = i * 1000 / requestsPerSecond;
            scheduler.scheduleAtFixedRate(() -> {
                if (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
                    requestCount.incrementAndGet();
                    try {
                        executeRequest(endpoint, params);
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        failureCount.incrementAndGet();
                        System.err.printf("Request failed: %s%n", e.getMessage());
                    }
                }
            }, delay, 1000, TimeUnit.MILLISECONDS);
        }

        // Wait for test to complete
        Thread.sleep(durationSeconds * 1000);
        scheduler.shutdown();
        scheduler.awaitTermination(1, TimeUnit.SECONDS);

        // Print results
        long totalTime = System.currentTimeMillis() - startTime;
        System.out.printf("Test completed in %d ms%n", totalTime);
        System.out.printf("Total requests: %d%n", requestCount.get());
        System.out.printf("Success: %d (%.2f%%)%n", successCount.get(), (successCount.get() * 100.0) / requestCount.get());
        System.out.printf("Failure: %d (%.2f%%)%n", failureCount.get(), (failureCount.get() * 100.0) / requestCount.get());
        System.out.printf("Average requests per second: %.2f%n", (requestCount.get() * 1000.0) / totalTime);
    }

    private static void executeRequest(String endpoint, List<String> params) throws IOException {
        switch (endpoint) {
            case "coffee-get-all":
                getCoffeeAll();
                break;
            case "coffee-get-by-id":
                if (params.size() != 1) throw new IllegalArgumentException("Need 1 parameter: id");
                getCoffeeById(Long.parseLong(params.get(0)));
                break;
            case "coffee-get-by-name":
                if (params.size() != 1) throw new IllegalArgumentException("Need 1 parameter: name");
                getCoffeeByName(params.get(0));
                break;
            case "coffee-post-json":
                if (params.size() != 2) throw new IllegalArgumentException("Need 2 parameters: name price");
                postCoffeeJson(params.get(0), Money.of(CurrencyUnit.of("CNY"), Double.parseDouble(params.get(1))));
                break;
            case "coffee-post-form":
                if (params.size() != 2) throw new IllegalArgumentException("Need 2 parameters: name price");
                postCoffeeForm(params.get(0), Money.of(CurrencyUnit.of("CNY"), Double.parseDouble(params.get(1))));
                break;
            case "coffee-post-batch":
                if (params.size() < 1) throw new IllegalArgumentException("Need at least 1 parameter: coffee1 coffee2 ...");
                postCoffeeBatch(params);
                break;
            case "order-get-by-id":
                if (params.size() != 1) throw new IllegalArgumentException("Need 1 parameter: id");
                getOrderById(Long.parseLong(params.get(0)));
                break;
            case "order-post":
                if (params.size() < 2) throw new IllegalArgumentException("Need at least 2 parameters: customer items");
                String customer = params.get(0);
                List<String> items = Arrays.asList(params.get(1).split(","));
                postOrder(customer, items);
                break;
            default:
                throw new IllegalArgumentException("Unknown endpoint: " + endpoint);
        }
    }

    private static void getCoffeeAll() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/coffee")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }

    private static void getCoffeeById(long id) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/coffee/" + id)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }

    private static void getCoffeeByName(String name) throws IOException {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/coffee").newBuilder()
                .addQueryParameter("name", name)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
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
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }

    private static void getOrderById(long id) throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/order/" + id)
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
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
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
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
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }

    private static void postCoffeeBatch(List<String> params) throws IOException {
        // Create a temporary file content with coffee data
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
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        }
    }
}