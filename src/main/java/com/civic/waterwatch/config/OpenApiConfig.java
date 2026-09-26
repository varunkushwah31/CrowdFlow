package com.civic.waterwatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WaterWatch Civic Crowdsourcing & Infrastructure Platform API")
                        .version("1.0.0")
                        .description("Civic-tech enterprise monolith for crowdsourced water hazard reporting, spatial DBSCAN clustering, automated root cause pattern correlation, and municipal escalation.")
                        .contact(new Contact()
                                .name("Civic WaterWatch Operations")
                                .email("contact@waterwatch.civic.local"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
