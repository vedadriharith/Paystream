package com.paystream.payment.config;

import java.util.TimeZone;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Logs key runtime settings once the application is fully started,
 * so a quick look at the logs confirms the service is configured as expected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupInfoLogger {

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo() {
        log.info("{} is ready on port {} (virtual threads enabled: {}, JVM timezone: {})",
                environment.getProperty("spring.application.name"),
                environment.getProperty("local.server.port"),
                environment.getProperty("spring.threads.virtual.enabled", "false"),
                TimeZone.getDefault().getID());
    }
}