package com.jonathan.gamestore.sales.service;

import com.jonathan.gamestore.sales.client.CatalogClient;
import com.jonathan.gamestore.sales.dto.GameResponse;
import com.jonathan.gamestore.sales.dto.OrderItemRequest;
import com.jonathan.gamestore.sales.dto.SaleOrderRequest;
import com.jonathan.gamestore.sales.model.OrderItem;
import com.jonathan.gamestore.sales.model.OrderStatus;
import com.jonathan.gamestore.sales.model.SaleOrder;
import com.jonathan.gamestore.sales.repository.SaleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * CAPA DE NEGOCIO: SaleOrderService
 *
 * Es el cerebro de la aplicacion. Aqui se encuentra toda la logica y reglas del negocio de compras.
 * Integrado con OpenFeign y protegido con el Fallback de Resilience4j.
 */
@Service
@RequiredArgsConstructor
public class SaleOrderService {

 // Dependencia al repositorio para hablar con la base de datos SQL H2
 private final SaleOrderRepository orderRepository;

 // Dependencia OpenFeign protegida con CatalogClientFallback
 private final CatalogClient catalogClient;

 /**
  * METODO PRINCIPAL: createOrder
  * Crea una orden de compra, valida la existencia y stock en catalog-service,
  * blinda el precio oficial del juego y genera las claves de activacion.
  */
 @Transactional
 public SaleOrder createOrder(SaleOrderRequest request) {
  // 1. Instanciamos la orden padre con estado inicial PENDING
  SaleOrder order = SaleOrder.builder()
          .userId(request.userId())
          .status(OrderStatus.PENDING)
          .build();

  // 2. Variable acumuladora para el total de la compra (inicia en 0.00)
  BigDecimal total = BigDecimal.ZERO;

  // 3. Recorremos cada juego solicitado por el usuario
  for (OrderItemRequest itemReq : request.items()) {

   // LLAMADA A OPENFEIGN (Protegida por CatalogClientFallback si el catalogo esta caido)
   GameResponse game = catalogClient.getGameById(itemReq.gameId());

   // REGLA 1: Verificar que el juego exista y este activo en tienda
   if (game == null || !Boolean.TRUE.equals(game.active())) {
    throw new IllegalArgumentException(
            "El videojuego con ID '" + itemReq.gameId() + "' no existe o se encuentra descontinuado en el catalogo."
    );
   }

   // REGLA 2: Verificar stock disponible en almacen
   if (game.stock() == null || game.stock() < itemReq.quantity()) {
    int stockDisponible = (game.stock() != null) ? game.stock() : 0;
    throw new IllegalArgumentException(
            "Stock insuficiente para el juego '" + game.title() + "'. Disponibles: " + stockDisponible + ", Solicitados: " + itemReq.quantity()
    );
   }

   // REGLA 3 (BLINDAJE DE PRECIO):
   // Usamos SIEMPRE el precio oficial de MongoDB (game.price()), NUNCA el que envio el cliente
   BigDecimal officialPrice = game.price();

   // Generamos una clave de activacion digital unica (CD-Key) estilo STEAM
   String generatedKey = "STEAM-" + UUID.randomUUID().toString().toUpperCase();

   // Construimos la entidad OrderItem con el titulo y precio oficial
   OrderItem item = OrderItem.builder()
           .gameId(itemReq.gameId())
           .gameTitle(game.title())
           .unitPrice(officialPrice)
           .quantity(itemReq.quantity())
           .digitalKey(generatedKey)
           .build();

   // Asociamos el item con la orden padre
   order.addItem(item);

   // Calculamos subtotal = precioOficial * cantidad
   BigDecimal itemSubtotal = officialPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));

   // Acumulamos en el total: total = total + subtotal
   total = total.add(itemSubtotal);
  }

  // 4. Asignamos el total exacto calculado en el servidor
  order.setTotalAmount(total);

  // 5. Guardamos en la base de datos SQL H2 en cascada
  return orderRepository.save(order);
 }

 /**
  * Recupera todas las ordenes existentes en el sistema.
  */
 @Transactional(readOnly = true)
 public List<SaleOrder> getAllOrders() {
  return orderRepository.findAll();
 }

 /**
  * Busca una orden especifica por su ID numerico.
  */
 @Transactional(readOnly = true)
 public Optional<SaleOrder> getOrderById(Long id) {
  return orderRepository.findById(id);
 }

 /**
  * Obtiene el historial de todas las ordenes de un usuario en particular.
  */
 @Transactional(readOnly = true)
 public List<SaleOrder> getOrdersByUserId(Long userId) {
  return orderRepository.findByUserId(userId);
 }

 /**
  * Actualiza el contenido de una orden existente si fue encontrada por su ID.
  */
 @Transactional
 public Optional<SaleOrder> updateOrder(Long id, SaleOrderRequest request) {
  return orderRepository.findById(id).map(existingOrder -> {
   existingOrder.setUserId(request.userId());

   existingOrder.getItems().clear();

   BigDecimal total = BigDecimal.ZERO;
   for (OrderItemRequest itemReq : request.items()) {
    GameResponse game = catalogClient.getGameById(itemReq.gameId());

    if (game == null || !Boolean.TRUE.equals(game.active())) {
     throw new IllegalArgumentException("El videojuego con ID '" + itemReq.gameId() + "' no existe o esta inactivo.");
    }

    BigDecimal officialPrice = game.price();
    String generatedKey = "STEAM-" + UUID.randomUUID().toString().toUpperCase();

    OrderItem item = OrderItem.builder()
            .gameId(itemReq.gameId())
            .gameTitle(game.title())
            .unitPrice(officialPrice)
            .quantity(itemReq.quantity())
            .digitalKey(generatedKey)
            .build();

    existingOrder.addItem(item);

    BigDecimal itemSubtotal = officialPrice.multiply(BigDecimal.valueOf(itemReq.quantity()));
    total = total.add(itemSubtotal);
   }

   existingOrder.setTotalAmount(total);
   return orderRepository.save(existingOrder);
  });
 }

 /**
  * Elimina fisicamente una orden de compra por su ID.
  */
 @Transactional
 public boolean deleteOrder(Long id) {
  if (orderRepository.existsById(id)) {
   orderRepository.deleteById(id);
   return true;
  }
  return false;
 }
}