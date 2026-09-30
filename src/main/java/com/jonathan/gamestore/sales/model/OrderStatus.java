package com.jonathan.gamestore.sales.model;

/**
 * 🗃️ ENUMERADOR: OrderStatus
 *
 * ¿Qué es un enum?
 * Es un tipo de dato especial que define un conjunto cerrado y seguro de opciones válidas.
 * En lugar de usar Strings libres como "pendiente", "PENDIENTE" o "completado" (lo que causaría
 * muchos errores por faltas de ortografía o mayúsculas), usamos un Enum para que el compilador
 * nos obligue a usar únicamente estos 3 estados oficiales:
 *
 * 1. PENDING: La orden fue creada por el cliente, pero el pago todavía no se ha confirmado.
 * 2. COMPLETED: El pago fue aprobado con éxito y las claves digitales (CD-Keys) están liberadas.
 * 3. CANCELLED: La compra fue rechazada por fondos insuficientes o cancelada por el usuario.
 */
public enum OrderStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
