package ru.practicum.statsclient.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stats.client")
public class StatsClientProperties {

    /**
     * ID сервиса статистики в Eureka (spring.application.name сервера статистики).
     */
    private String statsServiceId = "stats-server";

    /**
     * Таймаут установки соединения (мс).
     */
    private Integer connectTimeoutMillis = 5000;

    /**
     * Таймаут чтения данных (мс).
     */
    private Integer readTimeoutMillis = 5000;

    public String getStatsServiceId() {
        return statsServiceId;
    }

    public void setStatsServiceId(String statsServiceId) {
        this.statsServiceId = statsServiceId;
    }

    public Integer getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public Integer getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(Integer readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }
}
