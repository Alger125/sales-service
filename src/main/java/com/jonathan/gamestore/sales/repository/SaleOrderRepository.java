package com.jonathan.gamestore.sales.repository;

import com.jonathan.gamestore.sales.model.SaleOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CAPA DE PERSISTENCIA: SaleOrderRepository
 *
 * ¿Qué es y cómo funciona?
 * Es una interfaz que se comunica directamente con la base de datos SQL H2.
 *
 * ¿Por qué no tiene código adentro?
 * Al extender de `JpaRepository<SaleOrder, Long>`, Spring Data JPA crea dinámicamente
 * la implementación en memoria en tiempo de ejecución. Nos regala métodos listos como:
 * - save(SaleOrder order): Inserta o actualiza la orden en la tabla.
 * - findById(Long id): Busca por clave primaria (devuelve un Optional).
 * - findAll(): Trae todas las filas de la tabla.
 * - deleteById(Long id): Borra una fila por su clave primaria.
 *
 * Consultas Derivadas (Query Methods):
 * Spring Boot lee el nombre del método y escribe el SQL por ti:
 * 'findByUserId(Long userId)' genera automáticamente:
 * SELECT * FROM sale_orders WHERE user_id = ?
 */
@Repository
public interface SaleOrderRepository extends JpaRepository<SaleOrder, Long> {

 /**
 * Busca todas las órdenes de compra pertenecientes a un usuario específico.
 * @param userId Identificador del usuario comprador.
 * @return Lista de órdenes asociadas a ese usuario.
 */
 List<SaleOrder> findByUserId(Long userId);
}
