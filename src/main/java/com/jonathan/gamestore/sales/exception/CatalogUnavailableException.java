package com.jonathan.gamestore.sales.exception;

/**
 * EXCEPCION DE RESILIENCIA: CatalogUnavailableException
 *
 * Se lanza cuando catalog-service esta apagado, fuera de linea o
 * cuando el Circuit Breaker entra en estado OPEN (abierto).
 * Permite que el GlobalExceptionHandler responda con un codigo HTTP 503 limpio.
 */
public class CatalogUnavailableException extends RuntimeException {

    public CatalogUnavailableException(String message) {
        super(message);
    }

    public CatalogUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}