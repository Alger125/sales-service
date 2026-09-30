package com.jonathan.gamestore.sales.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 🚨 DTO DE RESPUESTA DE ERROR: ErrorResponse
 *
 * ¿Por qué existe esta clase?
 * Por defecto, cuando ocurre un error en Spring Boot, el framework devuelve una página HTML fea
 * o un JSON con campos confusos ("trace", "path", "status").
 * Para que las aplicaciones móviles o el frontend de React puedan mostrarle al usuario final
 * exactamente qué falló (ej: "El precio debe ser mayor a 0"), creamos este molde estándar
 * que unifica el formato de todas las respuestas de error de nuestra API.
 *
 * Campos explicados para un Junior:
 * @param status Código numérico de estado HTTP (ej: 400 Bad Request, 404 Not Found).
 * @param error Nombre corto y oficial del tipo de error (ej: "Bad Request").
 * @param message Explicación legible en lenguaje humano de lo ocurrido.
 * @param validationErrors Mapa clave-valor donde la clave es el nombre del campo que falló
 *                         (ej: "quantity") y el valor es la regla rota (ej: "La cantidad debe ser al menos 1").
 * @param timestamp Fecha y hora exacta del momento en que ocurrió el error.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> validationErrors,
        LocalDateTime timestamp
) {
}
