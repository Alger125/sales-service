package com.jonathan.gamestore.sales.controller;

import com.jonathan.gamestore.sales.dto.SaleOrderRequest;
import com.jonathan.gamestore.sales.model.SaleOrder;
import com.jonathan.gamestore.sales.service.SaleOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 🌐 CAPA DE CONTROLADOR REST: SaleOrderController
 *
 * ¿Qué representa esta clase?
 * Es el "Mesero" del microservicio. Es el punto de contacto directo con el mundo exterior
 * a través de peticiones HTTP (desde una app móvil, un frontend de React o herramientas como Postman).
 *
 * Responsabilidad Única (SRP):
 * El controlador SOLO debe:
 * 1. Recibir la petición HTTP y deserializar el JSON al DTO.
 * 2. Validar que la petición venga bien con @Valid.
 * 3. Delegar la tarea al Service (la cocina).
 * 4. Devolver la respuesta con el código de estado HTTP adecuado (200, 201, 404, etc.).
 * ¡El controlador NUNCA debe hacer cálculos matemáticos ni tocar la base de datos!
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class SaleOrderController {

    // Inyección de dependencias del servicio de órdenes
    private final SaleOrderService orderService;

    /**
     * 🟢 POST /api/orders
     * Crea una nueva orden de compra.
     *
     * @Valid: Le ordena al interceptor de validación de Spring que revise todas las anotaciones
     *         (@NotNull, @Positive, etc.) del SaleOrderRequest. Si algo no cumple, se corta
     *         la ejecución aquí mismo y salta al GlobalExceptionHandler devolviendo HTTP 400.
     * @RequestBody: Convierte el texto JSON que viene en la petición a nuestro objeto Java Record.
     * @return HTTP 201 (Created) junto con el JSON de la orden creada.
     */
    @PostMapping
    public ResponseEntity<SaleOrder> createOrder(@Valid @RequestBody SaleOrderRequest request) {
        SaleOrder createdOrder = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    /**
     * 🔵 GET /api/orders
     * Devuelve la lista completa de todas las órdenes del sistema.
     * @return HTTP 200 (OK) con un arreglo JSON de órdenes.
     */
    @GetMapping
    public ResponseEntity<List<SaleOrder>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * 🔵 GET /api/orders/{id}
     * Busca una orden por su identificador único (ej: /api/orders/1).
     *
     * @PathVariable: Extrae el valor de la variable en la URL ({id}) y lo pasa como parámetro.
     * @return HTTP 200 (OK) si la encontró, o HTTP 404 (Not Found) si no existe.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SaleOrder> getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🔵 GET /api/orders/user/{userId}
     * Consulta todas las órdenes pertenecientes a un cliente (ej: /api/orders/user/101).
     * @return HTTP 200 (OK) con la lista de compras del usuario.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SaleOrder>> getOrdersByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
    }

    /**
     * 🟡 PUT /api/orders/{id}
     * Actualiza una orden de compra existente.
     * @return HTTP 200 (OK) si se actualizó, o HTTP 404 (Not Found) si no existía.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SaleOrder> updateOrder(@PathVariable Long id, @Valid @RequestBody SaleOrderRequest request) {
        return orderService.updateOrder(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 🔴 DELETE /api/orders/{id}
     * Elimina una orden y sus ítems asociados de la base de datos.
     * @return HTTP 204 (No Content) indicando eliminación exitosa sin cuerpo de respuesta,
     *         o HTTP 404 (Not Found) si no existía.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        if (orderService.deleteOrder(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
