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
        "ru.practicum.web.comment.entity",
        "ru.practicum.web.event.entity",
        "ru.practicum.web.user.entity"
})
@EnableJpaRepositories(basePackages = {
        "ru.practicum.web.comment.repository",
        "ru.practicum.web.event.repository",
        "ru.practicum.web.user.repository"
})
@SpringBootApplication(scanBasePackages = {
        "ru.practicum.comment",
        "ru.practicum.internal",
        "ru.practicum.web.comment",
        "ru.practicum.web.exception",
        "ru.practicum.web.validation"
})
public class CommentServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApp.class, args);
    }
}
