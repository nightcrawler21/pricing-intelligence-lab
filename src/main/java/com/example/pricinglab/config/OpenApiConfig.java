package com.example.pricinglab.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for API documentation.
 *
 * Access the documentation at: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pricingLabOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Pricing Intelligence Lab API")
                .description("""
                    Internal API for managing store-level pricing experiments.

                    **Important Disclaimers:**
                    - This is a SIMULATION tool only
                    - No real pricing changes are made to POS systems
                    - No customer-level pricing is supported
                    - All experiments require approval before simulation

                    For questions, contact the Pricing Analytics team.
                    """)
                .version("0.0.1-SNAPSHOT")
                .contact(new Contact()
                    .name("Pricing Analytics Team")
                    .email("pricing-analytics@example.com")))
            .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
            .components(new Components()
                .addSecuritySchemes("basicAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("basic")));
    }
}
