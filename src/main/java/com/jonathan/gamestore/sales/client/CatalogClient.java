package com.jonathan.gamestore.sales.client;

import com.jonathan.gamestore.sales.dto.GameResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * CLIENTE DECLARATIVO OPENFEIGN: CatalogClient
 *
 * ¿Qué representa esta interfaz?
 * Es el "puente de comunicación" directo entre 'sales-service' y 'catalog-service'.
 *
 * ¿Por qué es solo una interfaz sin código adentro?
 * En tiempo de ejecución, Spring Cloud OpenFeign lee esta interfaz y genera automáticamente
 * la clase que implementa la conexión HTTP real.
 *
 * Anotaciones explicadas para un programador Junior:
 * 1. @FeignClient(name = "catalog-service"):
 * - 'name': Nombre lógico registrado en Eureka Server.
 * - OpenFeign consulta a Eureka: "¿Dónde está CATALOG-SERVICE?".
 * - Eureka responde con la IP y puerto actual (ej: localhost:8082).
 * - Si hay varios servidores de catálogo, reparte el tráfico automáticamente (Load Balancing).
 *
 * 2. @GetMapping("/api/games/{id}"):
 * - Coincide exactamente con el endpoint expuesto en GameController de catalog-service.
 * - Recibe el JSON de MongoDB y lo mapea automáticamente a nuestro DTO GameResponse.
 */
@FeignClient(name = "catalog-service")
public interface CatalogClient {

 /**
 * Consulta la información de un videojuego en el catálogo por su identificador único de MongoDB.
 *
 * @param id Identificador hexadecimal del juego en MongoDB (ej: "650c1f1e9b1d8b2bad000001").
 * @return El objeto GameResponse con el precio oficial, título, stock y estado activo.
 */
 @GetMapping("/api/games/{id}")
 GameResponse getGameById(@PathVariable("id") String id);
}
