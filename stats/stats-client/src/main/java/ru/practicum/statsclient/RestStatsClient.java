package ru.practicum.statsclient;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация StatsClient на основе RestTemplate и DiscoveryClient.
 * Использует службу обнаружения для динамического получения адреса stats-server.
 */
public class RestStatsClient implements StatsClient {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final DiscoveryClient discoveryClient;
    private final String statsServiceId;
    private final RetryTemplate retryTemplate;

    public RestStatsClient(RestTemplate restTemplate, DiscoveryClient discoveryClient, String statsServiceId) {
        Assert.notNull(restTemplate, "restTemplate must not be null");
        Assert.notNull(discoveryClient, "discoveryClient must not be null");
        Assert.hasText(statsServiceId, "statsServiceId must not be blank");
        this.restTemplate = restTemplate;
        this.discoveryClient = discoveryClient;
        this.statsServiceId = statsServiceId;
        this.retryTemplate = buildRetryTemplate();
    }

    private RetryTemplate buildRetryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        template.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    private ServiceInstance getInstance() {
        try {
            return discoveryClient
                    .getInstances(statsServiceId)
                    .getFirst();
        } catch (Exception exception) {
            throw new StatsServerUnavailable(
                    "Ошибка обнаружения адреса сервиса статистики с id: " + statsServiceId,
                    exception
            );
        }
    }

    private URI makeUri(String path) {
        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        return URI.create("http://" + instance.getHost() + ":" + instance.getPort() + path);
    }

    @Override
    public void hit(EndpointHitDto dto) {
        Assert.notNull(dto, "EndpointHitDto must not be null");
        URI uri = makeUri("/hit");
        try {
            restTemplate.postForLocation(uri, dto);
        } catch (RestClientException ex) {
            throw new StatsClientException("Ошибка вызова POST /hit: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        Assert.notNull(start, "start must not be null");
        Assert.notNull(end, "end must not be null");

        ServiceInstance instance = retryTemplate.execute(ctx -> getInstance());
        String baseUrl = "http://" + instance.getHost() + ":" + instance.getPort();

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/stats")
                .queryParam("start", start.format(FORMATTER))
                .queryParam("end", end.format(FORMATTER))
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            for (String u : uris) {
                builder.queryParam("uris", u);
            }
        }

        URI uri = builder.build(true).toUri();

        try {
            ResponseEntity<List<ViewStatsDto>> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    new ParameterizedTypeReference<List<ViewStatsDto>>() {}
            );
            List<ViewStatsDto> body = response.getBody();
            return body != null ? body : new ArrayList<>();
        } catch (RestClientException ex) {
            throw new StatsClientException("Ошибка вызова GET /stats: " + ex.getMessage(), ex);
        }
    }
}
