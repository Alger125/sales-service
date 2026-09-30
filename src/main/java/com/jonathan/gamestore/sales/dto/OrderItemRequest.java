package com.jonathan.gamestore.sales.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object): OrderItemRequest
 *
 * ¿Qué es un DTO y por qué usamos un Java Record?
 * - Un DTO es un objeto de transferencia: solo sirve para transportar datos limpios entre el cliente
 * (Frontend / Postman) y el microservicio.
 * - Usamos `record` de Java 17 porque crea clases compactas, inmutables (nadie puede alterar sus datos
 * una vez creadas) y con constructor, getters, equals y hashCode automáticos en una sola línea.
 *
 * Validaciones explicadas para un Junior (Bean Validation):
 * Estas anotaciones actúan como "guardias de seguridad en la puerta":
 */
public record OrderItemRequest(

 // @NotBlank: El ID del juego de MongoDB es un String y no puede venir vacío o en blanco.
 @NotBlank(message = "El ID del videojuego es obligatorio")
 String gameId,

 // @NotBlank: El título no puede ser nulo, ni estar vacío "", ni contener solo espacios " ".
 @NotBlank(message = "El titulo del juego es obligatorio")
 String gameTitle,

 // @DecimalMin("0.01"): El precio unitario debe ser positivo (mínimo un centavo).
 @NotNull(message = "El precio unitario es obligatorio")
 @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor a 0")
 BigDecimal unitPrice,

 // @Positive: Obliga a que la cantidad de copias sea 1 o más (nunca 0 ni números negativos).
 @NotNull(message = "La cantidad es obligatoria")
 @Positive(message = "La cantidad debe ser al menos 1")
 Integer quantity
) {
}
