package com.jonathan.gamestore.sales.controller;

import com.jonathan.gamestore.sales.dto.SaleOrderRequest;
import com.jonathan.gamestore.sales.model.SaleOrder;
import com.jonathan.gamestore.sales.service.SaleOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLADOR REST: SaleOrderController
 * Expone las operaciones comerciales de compras y emision de claves digitales.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Ordenes de Venta", description = "Endpoints para gestion de compras, facturacion y claves digitales")
public class SaleOrderController {

 private final SaleOrderService orderService;

 @Operation(summary = "Crear nueva orden de compra",
         description = "Registra una orden validando stock y precio oficial contra catalog-service con proteccion de Circuit Breaker")
 @ApiResponses(value = {
         @ApiResponse(responseCode = "201", description = "Orden creada exitosamente con claves digitales generadas"),
         @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos o stock insuficiente"),
         @ApiResponse(responseCode = "404", description = "Videojuego no encontrado en el catalogo"),
         @ApiResponse(responseCode = "503", description = "Servicio de catalogo no disponible (Resilience4j Fallback activado)")
 })
 @PostMapping
 public ResponseEntity<SaleOrder> createOrder(@Valid @RequestBody SaleOrderRequest request) {
  SaleOrder createdOrder = orderService.createOrder(request);
  return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
 }

 @Operation(summary = "Listar todas las ordenes", description = "Obtiene el historial completo de ordenes registradas en el sistema")
 @ApiResponse(responseCode = "200", description = "Lista de ordenes obtenida correctamente")
 @GetMapping
 public ResponseEntity<List<SaleOrder>> getAllOrders() {
  return ResponseEntity.ok(orderService.getAllOrders());
 }

 @Operation(summary = "Consultar orden por ID", description = "Busca los detalles de una compra especifica por su ID numerico")
 @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Orden encontrada"),
         @ApiResponse(responseCode = "404", description = "Orden no encontrada con el ID proporcionado")
 })
 @GetMapping("/{id}")
 public ResponseEntity<SaleOrder> getOrderById(
         @Parameter(description = "Identificador numerico de la orden", example = "1")
         @PathVariable Long id) {
  return orderService.getOrderById(id)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
 }

 @Operation(summary = "Consultar ordenes por usuario", description = "Obtiene todas las compras realizadas por un cliente especifico")
 @ApiResponse(responseCode = "200", description = "Historial de compras del usuario")
 @GetMapping("/user/{userId}")
 public ResponseEntity<List<SaleOrder>> getOrdersByUserId(
         @Parameter(description = "ID del usuario comprador", example = "101")
         @PathVariable Long userId) {
  return ResponseEntity.ok(orderService.getOrdersByUserId(userId));
 }

 @Operation(summary = "Actualizar orden de compra", description = "Modifica los articulos y recalcula el monto oficial contra catalog-service")
 @ApiResponses(value = {
         @ApiResponse(responseCode = "200", description = "Orden actualizada exitosamente"),
         @ApiResponse(responseCode = "400", description = "Datos invalidos o stock insuficiente"),
         @ApiResponse(responseCode = "404", description = "Orden o videojuego no encontrado"),
         @ApiResponse(responseCode = "503", description = "Catalogo no disponible temporalmente")
 })
 @PutMapping("/{id}")
 public ResponseEntity<SaleOrder> updateOrder(
         @Parameter(description = "ID de la orden a actualizar", example = "1")
         @PathVariable Long id,
         @Valid @RequestBody SaleOrderRequest request) {
  return orderService.updateOrder(id, request)
          .map(ResponseEntity::ok)
          .orElse(ResponseEntity.notFound().build());
 }

 @Operation(summary = "Eliminar orden de compra", description = "Elimina fisicamente la orden y sus articulos asociados en cascada")
 @ApiResponses(value = {
         @ApiResponse(responseCode = "204", description = "Orden eliminada exitosamente"),
         @ApiResponse(responseCode = "404", description = "Orden no encontrada")
 })
 @DeleteMapping("/{id}")
 public ResponseEntity<Void> deleteOrder(
         @Parameter(description = "ID de la orden a eliminar", example = "1")
         @PathVariable Long id) {
  if (orderService.deleteOrder(id)) {
   return ResponseEntity.noContent().build();
  }
  return ResponseEntity.notFound().build();
 }
}