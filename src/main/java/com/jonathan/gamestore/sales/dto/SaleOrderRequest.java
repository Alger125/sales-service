package com.jonathan.gamestore.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO: SaleOrderRequest
 *
 * ¿Qué representa?
 * Es el molde exacto que debe tener el cuerpo (Body JSON) cuando un cliente envía una petición
 * HTTP POST a "/api/orders" para comprar videojuegos.
 *
 * REGLA DE ORO DE SEGURIDAD (Senior Tip):
 * ¿Notaste que este DTO NO tiene un campo 'totalAmount'?
 * Si le pidiéramos al frontend que nos mande el total a pagar, un hacker podría inspeccionar el navegador,
 * cambiar el JSON y mandar: { "totalAmount": 0.01 }.
 * Por eso, el cliente SOLO envía los juegos y sus cantidades; el cálculo matemático del total se hace
 * estrictamente en el backend (SaleOrderService).
 */
public record SaleOrderRequest(

 // Valida que venga el ID del usuario y que sea un número positivo.
 @NotNull(message = "El ID del usuario es obligatorio")
 @Positive(message = "El ID del usuario debe ser un numero positivo")
 Long userId,

 // @NotEmpty: La orden debe tener al menos un videojuego para comprar.
 // @Valid: ¡SÚPER IMPORTANTE! Le dice a Spring Boot: "No solo revises que la lista exista,
 // entra a cada elemento de la lista y valida también las reglas de OrderItemRequest".
 @NotEmpty(message = "La orden debe contener al menos un item")
 @Valid
 List<OrderItemRequest> items
) {
}
