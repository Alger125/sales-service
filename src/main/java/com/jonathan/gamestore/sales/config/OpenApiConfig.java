package com.jonathan.gamestore.sales.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURACION DE SWAGGER / OPENAPI 3
 *
 * Esta clase personaliza los metadatos globales que se muestran
 * en la cabecera de la interfaz grafica interactiva de Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI salesServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sales Service API - GameStore")
                        .description("Microservicio encargado de la gestion de ordenes de compra, " +
                                "facturacion y generacion de claves digitales (CD-Keys). " +
                                "Integrado con catalog-service via OpenFeign y blindado con Resilience4j.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Alger125")
                                .url("https://github.com/Alger125/sales-service"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://spring.io")));
    }
}