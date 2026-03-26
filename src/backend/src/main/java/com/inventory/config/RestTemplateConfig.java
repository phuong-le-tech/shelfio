package com.inventory.config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);
        return new RestTemplate(factory);
    }

    @Bean
    Executor emailExecutor() {
        return new ThreadPoolExecutor(
            1, 4, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static final int MAX_READ_TIMEOUT_SECONDS = 300;
    private static final int MAX_CONNECT_TIMEOUT_SECONDS = 30;

    @Bean("ollamaRestTemplate")
    public RestTemplate ollamaRestTemplate(
            @Value("${app.ai.ollama.timeout-seconds:60}") int readTimeoutSeconds,
            @Value("${app.ai.ollama.connect-timeout-seconds:10}") int connectTimeoutSeconds) {
        int effectiveReadTimeout = Math.min(readTimeoutSeconds, MAX_READ_TIMEOUT_SECONDS);
        int effectiveConnectTimeout = Math.min(connectTimeoutSeconds, MAX_CONNECT_TIMEOUT_SECONDS);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(effectiveConnectTimeout * 1_000);
        factory.setReadTimeout(effectiveReadTimeout * 1_000);
        return new RestTemplate(factory);
    }

    @Bean("geminiRestTemplate")
    public RestTemplate geminiRestTemplate(
            @Value("${app.ai.gemini.timeout-seconds:30}") int readTimeoutSeconds) {
        int effectiveReadTimeout = Math.min(readTimeoutSeconds, MAX_READ_TIMEOUT_SECONDS);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(effectiveReadTimeout * 1_000);
        return new RestTemplate(factory);
    }
}
