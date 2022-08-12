package com.sbytestream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class App {
    private static void help() {
        System.out.println("Syntax:");
        System.out.println("java -jar App url1 url2");
    }

    private static void requestWithApacheHttpClient(String url1, String url2) throws IOException {
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger n1 = new AtomicInteger();
        AtomicInteger n2 = new AtomicInteger();

        m_executor.execute(()-> {
            try {
                HttpGet getReq = new HttpGet(url1);
                CloseableHttpResponse resp = m_closableHttpClient.execute(getReq);
                InputStream is = resp.getEntity().getContent();
                String result = EntityUtils.toString(resp.getEntity());
                n1.set(Integer.parseInt(result));
                latch.countDown();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });

        m_executor.execute(()-> {
            try {
                HttpGet getReq = new HttpGet(url2);
                CloseableHttpResponse resp = m_closableHttpClient.execute(getReq);
                InputStream is = resp.getEntity().getContent();
                String result = EntityUtils.toString(resp.getEntity());
                n2.set(Integer.parseInt(result));
                latch.countDown();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });

        try {
            latch.await();
            int n = n1.get() + n2.get();
            System.out.printf("\nCurrentMS: %d, result: %d", System.currentTimeMillis(), n);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void requestWithJava11HttpClient(String url1, String url2) {
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create(url1))
                .timeout(Duration.ofMillis(5000))
                .GET()
                .build();

        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create(url2))
                .timeout(Duration.ofMillis(5000))
                .GET()
                .build();

        CompletableFuture<HttpResponse<String>> f1;
        f1 = m_javaClient.sendAsync(req1, HttpResponse.BodyHandlers.ofString());

        CompletableFuture<HttpResponse<String>> f2;
        f2 = m_javaClient.sendAsync(req2, HttpResponse.BodyHandlers.ofString());

        f1.thenCombine(f2, (s1, s2) -> {
            int n1 = Integer.parseInt(s1.body());
            int n2 = Integer.parseInt(s2.body());
            return (n1 + n2);
        }).thenAccept((n) -> {
            try {
                m_totalResponses.incrementAndGet();
                System.out.printf("\nCurrentMS: %d, result: %d", System.currentTimeMillis(), n);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void requestWithApacheAsyncHttpClient(String url1, String url2) throws Exception {
        Future<org.apache.http.HttpResponse> f1 = m_apacheAsyncHttpClient.execute(new HttpGet(url1), null);
        Future<org.apache.http.HttpResponse> f2 = m_apacheAsyncHttpClient.execute(new HttpGet(url2), null);

        String r1 = EntityUtils.toString(f1.get().getEntity());
        int n1 = Integer.parseInt(r1);

        String r2 = EntityUtils.toString(f2.get().getEntity());
        int n2 = Integer.parseInt(r2);

        int n = n1 + n2;
        System.out.printf("\nCurrentMS: %d, result: %d", System.currentTimeMillis(), n);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            help();
            return;
        }

        System.out.println("Press <ENTER> to start");
        System.in.read();

        String url1 = args[0];
        String url2 = args[1];
        int burstRequestCount = 500;
        m_latch = new CountDownLatch(burstRequestCount);
        long startMs = System.currentTimeMillis();

        m_apacheAsyncHttpClient.start();
        m_closableHttpClient = m_builder.build();

        for(int i=0; i < 5; i++) {
            System.out.printf("\nIteration: %d", i);
            for (int n = 0; n < burstRequestCount; n++) {
                m_totalRequests++;
                requestWithApacheAsyncHttpClient(url1, url2);
                //requestWithJava11HttpClient(url1, url2);
                //requestWithApacheHttpClient(url1, url2);
            }
            Thread.sleep(5000);
        }

        System.out.printf("\nStart MS: %d, Request count: %d, total requests sent: %d",
                startMs, burstRequestCount, m_totalRequests);
        System.in.read();
        System.out.printf("\nResponse count: %d", m_totalResponses.get());
    }

    private static CountDownLatch m_latch;
    private static volatile int m_totalRequests = 0;
    private static AtomicInteger m_totalResponses = new AtomicInteger();
    private static ExecutorService m_executor = Executors.newFixedThreadPool(1000);
    private static HttpClient m_javaClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).build();
    private static CloseableHttpAsyncClient m_apacheAsyncHttpClient = HttpAsyncClients.createDefault();
    private static HttpClientBuilder m_builder = HttpClientBuilder.create();
    private static CloseableHttpClient m_closableHttpClient = null;;
}
