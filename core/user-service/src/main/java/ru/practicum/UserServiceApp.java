package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableFeignClients
@EnableDiscoveryClient
@EntityScan(basePackages = "ru.practicum.web.user.entity")
@EnableJpaRepositories(basePackages = "ru.practicum.web.user.repository")
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.user",
        "ru.practicum.internal",
        "ru.practicum.web.user",
        "ru.practicum.web.exception",
        "ru.practicum.web.validation"
})
public class UserServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApp.class, args);
    }
}
