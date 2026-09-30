package com.jonathan.gamestore.sales.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO DE RESPUESTA REMOTA: GameResponse
 *
 * ¿Qué representa esta clase?
 * Es el molde en el que OpenFeign deposita la información que recibe desde 'catalog-service'.
 *
 * ¿Por qué existe en sales-service si el juego pertenece a catalog-service?
 * Principio de Desacoplamiento (Microservicios Independientes):
 * 'sales-service' NO tiene acceso al código ni a la base de datos de 'catalog-service'.
 * Solo se comunican compartiendo contratos JSON a través de la red.
 * Este record modela los datos del juego que necesitamos para validar la venta.
 *
 * Campos que utilizaremos para la lógica de negocio:
 * @param id Identificador único del juego en MongoDB (String de 24 caracteres).
 * @param title Nombre oficial del videojuego.
 * @param description Sinopsis o resumen.
 * @param genre Categoría o género.
 * @param price PRECIO OFICIAL de la base de datos (clave para evitar fraudes).
 * @param stock Existencias disponibles en almacén.
 * @param platforms Plataformas compatibles (Steam, PS5, etc.).
 * @param active Estado del juego (true = activo para venta, false = descontinuado).
 */
public record GameResponse(
 String id,
 String title,
 String description,
 String genre,
 BigDecimal price,
 Integer stock,
 List<String> platforms,
 Boolean active
) {
}
