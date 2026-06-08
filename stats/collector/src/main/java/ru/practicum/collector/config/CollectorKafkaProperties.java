package ru.practicum.collector.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public class CollectorKafkaProperties {

    private Topics topics = new Topics();

    public Topics getTopics() {
        return topics;
    }

    public void setTopics(Topics topics) {
        this.topics = topics;
    }

    public static class Topics {
        private String userActions = "stats.user-actions.v1";

        public String getUserActions() {
            return userActions;
        }

        public void setUserActions(String userActions) {
            this.userActions = userActions;
        }
    }
}
