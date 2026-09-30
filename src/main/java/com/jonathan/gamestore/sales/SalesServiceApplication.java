package com.jonathan.gamestore.sales;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * CLASE PRINCIPAL: SalesServiceApplication
 *
 * ¿Qué hace esta clase?
 * Es la puerta de entrada (punto de arranque) de todo el microservicio de Ventas.
 * Al ejecutar el método main(), Spring Boot inicia y levanta un servidor web interno
 * (Tomcat) en el puerto configurado (8081).
 *
 * Anotaciones clave explicadas para un programador Junior:
 * 1. @SpringBootApplication: Enciende la "magia" de Spring Boot. Combina 3 cosas:
 * - @Configuration: Permite registrar componentes y beans.
 * - @EnableAutoConfiguration: Configura automáticamente la base de datos H2, JPA, etc.
 * - @ComponentScan: Escanea todas las carpetas hermanas e hijas en busca de Controllers,
 * Services, Repositories y los inyecta donde sea necesario.
 *
 * 2. @EnableDiscoveryClient: Le da la orden al microservicio de buscar al servidor
 * de descubrimiento (Eureka Server en el puerto 8761) y registrarse automáticamente
 * con el nombre "SALES-SERVICE". Gracias a esto, otros microservicios pueden
 * encontrarlo sin saber su dirección IP fija.
 *
 * 3. @EnableFeignClients: Activa el soporte de Spring Cloud OpenFeign.
 * Le indica a Spring que busque todas las interfaces anotadas con @FeignClient y genere
 * en tiempo de ejecución las clases proxy que harán las llamadas HTTP hacia otros microservicios.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class SalesServiceApplication {

 public static void main(String[] args) {
 // Inicia el contenedor de inversión de control (IoC) y arranca la aplicación
 SpringApplication.run(SalesServiceApplication.class, args);
 }
}
