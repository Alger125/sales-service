package com.jonathan.gamestore.sales.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ENTIDAD HIJA: OrderItem (Mapea a la tabla SQL: 'order_items')
 *
 * ¿Qué representa?
 * Representa cada videojuego específico que forma parte de una orden de compra.
 * Por ejemplo: si compraste "Elden Ring" y "Hollow Knight", cada uno será un registro OrderItem
 * vinculado a la misma orden padre (SaleOrder).
 *
 * Anotaciones explicadas para un Junior:
 * - @Entity: Le indica a JPA / Hibernate que esta clase es una tabla en la base de datos SQL.
 * - @Table(name = "order_items"): Especifica el nombre de la tabla en SQL.
 * - @Getter / @Setter: Lombok genera en segundo plano getGameTitle(), setQuantity(), etc.
 * - @NoArgsConstructor / @AllArgsConstructor: Constructores vacíos y completos requeridos por JPA.
 * - @Builder: Permite construir objetos de forma legible: OrderItem.builder().gameTitle("Elden Ring").build()
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

 /**
 * Clave primaria única (ID) autoincremental en la base de datos (1, 2, 3...).
 */
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 /**
 * Identificador del juego en el microservicio de catálogo (catalog-service).
 * Se define como String para ser 100% compatible con identificadores NoSQL de MongoDB
 * (ObjectIds hexadecimales de 24 caracteres como "650c1f1e9b1d8b2bad000001").
 */
 @Column(nullable = false)
 private String gameId;

 /**
 * Título del juego al momento de la compra (ej: "Elden Ring").
 * Guardar el título aquí es una buena práctica de diseño (desnormalización controlada):
 * si el día de mañana el juego cambia de nombre en el catálogo, la factura histórica de esta venta
 * sigue mostrando el nombre con el que el usuario lo compró originalmente.
 */
 @Column(nullable = false)
 private String gameTitle;

 /**
 * Precio unitario al que se vendió en ese instante exacto.
 * Usamos BigDecimal SIEMPRE para dinero, NUNCA float o double (porque estos últimos
 * tienen problemas de redondeo y pierden centavos).
 */
 @Column(nullable = false, precision = 10, scale = 2)
 private BigDecimal unitPrice;

 /**
 * Cantidad de copias adquiridas de este juego (ej: 1, 2, 3).
 */
 @Column(nullable = false)
 private Integer quantity;

 /**
 * Clave de activación digital única generada para el usuario (ej: "STEAM-F5DB-406E-...").
 */
 @Column(nullable = false)
 private String digitalKey;

 /**
 * RELACIÓN MUCHOS A UNO: @ManyToOne
 * Muchos ítems de videojuegos pertenecen a una sola orden de compra (SaleOrder).
 *
 * - fetch = FetchType.LAZY: Optimización de rendimiento. No carga la orden padre en memoria
 * a menos que explícitamente llamemos a item.getOrder().
 * - @JoinColumn(name = "order_id"): En la tabla SQL 'order_items' crea la clave foránea (FK) llamada order_id.
 * - @JsonIgnore: ¡CRUCIAL! Cuando Spring convierte este objeto a JSON para enviarlo por HTTP,
 * evita un bucle infinito mortal: SaleOrder contiene OrderItem -> OrderItem contiene SaleOrder -> etc.
 */
 @ManyToOne(fetch = FetchType.LAZY)
 @JoinColumn(name = "order_id", nullable = false)
 @JsonIgnore
 private SaleOrder order;
}
