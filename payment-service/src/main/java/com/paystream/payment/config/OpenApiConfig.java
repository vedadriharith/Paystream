package com.paystream.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API metadata shown at the top of Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("PayStream Payment Service API")
                .version("v1")
                .description("Accepts payment requests and exposes payment status. "
                        + "Balances are owned by ledger-service, not this service."));
    }
}