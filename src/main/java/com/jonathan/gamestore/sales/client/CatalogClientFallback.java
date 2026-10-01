package com.jonathan.gamestore.sales.client;

import com.jonathan.gamestore.sales.dto.GameResponse;
import com.jonathan.gamestore.sales.exception.CatalogUnavailableException;
import org.springframework.stereotype.Component;

/**
 * IMPLEMENTACION DE FALLBACK PARA OPENFEIGN:
 * Si catalog-service esta apagado, fuera de linea o el Circuit Breaker se abre,
 * OpenFeign desvia la llamada aqui de forma 100% automatica.
 */
@Component
public class CatalogClientFallback implements CatalogClient {

    @Override
    public GameResponse getGameById(String id) {
        throw new CatalogUnavailableException(
                "El catalogo de videojuegos no esta disponible temporalmente. " +
                        "No se pudo consultar el juego con ID '" + id + "'. Intente mas tarde."
        );
    }
}