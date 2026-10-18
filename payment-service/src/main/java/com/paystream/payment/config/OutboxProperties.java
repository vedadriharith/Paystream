package com.paystream.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for the outbox publisher, bound from paystream.outbox.* properties.
 */
@ConfigurationProperties(prefix = "paystream.outbox")
public record OutboxProperties(
        @DefaultValue("500") long pollIntervalMs,
        @DefaultValue("50") int batchSize,
        @DefaultValue("10000") long sendTimeoutMs,
        @DefaultValue("5") int alertAfterAttempts
) {
}
