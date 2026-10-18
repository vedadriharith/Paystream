package com.paystream.payment.config;

import com.paystream.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the topics this service produces to. Spring's KafkaAdmin creates them at startup if missing.
 * In production, topics are usually managed by infrastructure tooling instead.
 */
@Configuration
public class KafkaTopicsConfig {

    @Bean
    public NewTopic paymentsInitiatedTopic(@Value("${paystream.kafka.topic-replicas:1}") int replicas) {
        return TopicBuilder.name(Topics.PAYMENTS_INITIATED)
                .partitions(3)
                .replicas(replicas)
                .build();
    }
}
