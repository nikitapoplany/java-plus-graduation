package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableFeignClients
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "ru.practicum.web.admin.entity",
        "ru.practicum.web.event.entity",
        "ru.practicum.web.request.entity",
        "ru.practicum.web.user.entity"
})
@EnableJpaRepositories(basePackages = {
        "ru.practicum.web.request.repository"
})
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.internal",
        "ru.practicum.request",
        "ru.practicum.web.exception",
        "ru.practicum.web.request.controller",
        "ru.practicum.web.validation"
})
public class RequestServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(RequestServiceApp.class, args);
    }
}
