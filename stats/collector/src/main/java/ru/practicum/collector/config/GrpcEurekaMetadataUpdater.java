package ru.practicum.collector.config;

import com.netflix.appinfo.ApplicationInfoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.event.GrpcServerStartedEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class GrpcEurekaMetadataUpdater {

    private final ObjectProvider<ApplicationInfoManager> applicationInfoManagerProvider;
    private final ObjectProvider<EurekaRegistration> eurekaRegistrationProvider;
    private volatile String grpcPort;

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onGrpcServerStarted(GrpcServerStartedEvent event) {
        grpcPort = Integer.toString(event.getPort());
        publishMetadata(grpcPort);
    }

    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onApplicationReady(ApplicationReadyEvent event) {
        if (grpcPort != null) {
            publishMetadata(grpcPort);
        }
    }

    private void publishMetadata(String port) {
        Map<String, String> metadata = Map.of(
                "grpcPort", port,
                "gRPC_port", port
        );

        EurekaRegistration registration = eurekaRegistrationProvider.getIfAvailable();
        if (registration != null) {
            registration.getInstanceConfig().getMetadataMap().putAll(metadata);
        }

        ApplicationInfoManager applicationInfoManager = applicationInfoManagerProvider.getIfAvailable();
        if (applicationInfoManager != null) {
            Map<String, String> updatedMetadata = new HashMap<>(applicationInfoManager.getInfo().getMetadata());
            updatedMetadata.putAll(metadata);
            applicationInfoManager.registerAppMetadata(updatedMetadata);
        }

        log.info("Published gRPC port {} to Eureka metadata", port);
    }
}
