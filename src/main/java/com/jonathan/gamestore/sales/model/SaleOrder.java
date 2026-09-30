package com.jonathan.gamestore.sales.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ENTIDAD PADRE: SaleOrder (Mapea a la tabla SQL: 'sale_orders')
 *
 * ¿Qué representa?
 * Representa la orden de venta completa (el ticket o factura general de compra).
 * Contiene el ID del comprador, el estado de la venta, el total a pagar y la lista de todos
 * los videojuegos incluidos en el pedido.
 */
@Entity
@Table(name = "sale_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleOrder {

 /**
 * Identificador único de la orden de compra autogenerado por SQL (1, 2, 3...).
 */
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 /**
 * Identificador del usuario que realizó la compra.
 */
 @Column(nullable = false)
 private Long userId;

 /**
 * Importe monetario total calculado sumando (precioUnitario * cantidad) de cada ítem.
 * Se usa BigDecimal con 10 dígitos en total y 2 decimales para precisión de centavos.
 */
 @Column(nullable = false, precision = 10, scale = 2)
 private BigDecimal totalAmount;

 /**
 * Estado de la orden (PENDING, COMPLETED, CANCELLED).
 * @Enumerated(EnumType.STRING): Guarda la palabra legible en la base de datos ("PENDING")
 * en lugar del índice numérico (0, 1, 2), facilitando auditorías visuales en la consola de H2.
 */
 @Enumerated(EnumType.STRING)
 @Column(nullable = false)
 private OrderStatus status;

 /**
 * Fecha y hora exacta en la que se generó la compra.
 */
 @Column(nullable = false, updatable = false)
 private LocalDateTime createdAt;

 /**
 * RELACIÓN UNO A MUCHOS: @OneToMany
 * Una orden de venta (SaleOrder) tiene muchos productos comprados (OrderItem).
 *
 * Conceptos Senior explicados para un Junior:
 * - mappedBy = "order": Le dice a Hibernate: "La dueña de la relación y de la clave foránea
 * en la base de datos es la propiedad 'order' dentro de la clase OrderItem".
 * - cascade = CascadeType.ALL: ¡Súper útil! Si guardamos la orden (orderRepository.save),
 * Hibernate automáticamente inserta todos los ítems en su propia tabla sin que tengamos
 * que hacer un loop manual guardando ítem por ítem. Si borramos la orden, borra sus ítems.
 * - orphanRemoval = true: Si quitamos un ítem de la lista de Java, Hibernate lo elimina
 * automáticamente de la base de datos para no dejar filas huérfanas.
 * - @Builder.Default: Evita que al usar el patrón Builder la lista sea null, inicializándola como un ArrayList vacío.
 */
 @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
 @Builder.Default
 private List<OrderItem> items = new ArrayList<>();

 /**
 * HOOK DE AUDITORÍA: @PrePersist
 * Este método se ejecuta automáticamente justo un microsegundo antes de que Hibernate
 * guarde la fila por primera vez en la base de datos.
 * Garantiza que nunca se nos olvide poner la fecha y hora de compra actual.
 */
 @PrePersist
 public void prePersist() {
 if (this.createdAt == null) {
 this.createdAt = LocalDateTime.now();
 }
 }

 /**
 * MÉTODO AYUDANTE BIDIRECCIONAL: addItem
 * ¿Por qué es necesario?
 * En relaciones bidireccionales de JPA, siempre se debe sincronizar ambos lados del objeto:
 * 1. Agrega el ítem a la lista de esta orden.
 * 2. Le asigna al ítem su referencia a esta orden (item.setOrder(this)).
 */
 public void addItem(OrderItem item) {
 items.add(item);
 item.setOrder(this);
 }
}
