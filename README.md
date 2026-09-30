# Sales Service (`sales-service`)

> **Microservicio de Gestión de Ventas, Órdenes y Generación de Claves Digitales de Videojuegos.** 
> Desarrollado con **Java 17**, **Spring Boot 4**, **Spring Data JPA**, **H2 Database**, **Spring Cloud Netflix Eureka** y **Spring Cloud OpenFeign**.

---

## 1. ¿Por qué es un Microservicio Independiente? (Autonomía y Desacoplamiento)

Una de las preguntas más importantes para un programador que da el salto a la arquitectura empresarial es: 
**¿Por qué no metimos todo esto en un solo proyecto gigante (monolito)?**

### Principio de Base de Datos Propia (Database-per-Service Pattern)
En un sistema monolítico tradicional, si la base de datos se satura o se cae, **toda la tienda muere**. 
En nuestra arquitectura de microservicios:
* `sales-service` **posee su propia base de datos relacional (H2 / SQL)**. Ningún otro microservicio tiene permitido conectarse directamente por JDBC a las tablas de ventas.
* **¿Por qué SQL para Ventas?** Porque las ventas involucran dinero y requieren garantías **ACID** (transacciones bancarias estrictas donde nada se puede perder ni duplicar).
* Si mañana `catalog-service` se apaga o sufre una caída por mantenimiento, `sales-service` **sigue vivo**; puede consultar cachés locales o devolver un mensaje de reintento controlado sin tumbar el sistema completo.

### Despliegue y Escalabilidad Independiente
Si se acerca el *Black Friday*, la cantidad de personas pagando órdenes se dispara por 100x, pero la cantidad de administradores subiendo nuevos juegos al catálogo sigue siendo baja. 
Al ser independiente, podemos levantar **5 instancias de `sales-service` en servidores distintos** sin tener que gastar memoria RAM levantando copias innecesarias del catálogo.

---

## 2. Mapa de Relación: ¿Cómo se comunica con los demás Microservicios?

Los microservicios **NUNCA** deben compartir bases de datos. Se comunican exclusivamente a través de la red usando protocolos ligeros:

```
 
 EUREKA SERVER 
 (Directorio Telefónico) 
 Puerto 8761 
 
 
 
 1. Heartbeat ("Estoy vivo en 8081") 1. Heartbeat ("Estoy vivo en 8082")
 
 
 SALES-SERVICE CATALOG-SERVICE 
 Puerto 8081 Puerto 8082 
 Base de Datos H2 Base de Datos Mongo 
 (Ventas y CD-Keys) (Fichas de Juegos) 
 
 
 2. Pregunta por HTTP vía OpenFeign 
 "¿El juego X existe y cuánto cuesta?" 
 
```

### 1. Relación con `eureka-server` (Puerto 8761 - Discovery Server)
* **¿Qué problema resuelve?**: En la nube o en contenedores Docker, las IPs y puertos cambian todo el tiempo. Si ponemos URLs fijas (*hardcoded*) como `http://192.168.1.50:8082`, el día que esa máquina cambie de IP, todo se rompe.
* **¿Cómo interactúan?**:
 1. Al arrancar `sales-service`, lee su archivo `application.properties` y busca a `eureka-server`.
 2. Le dice: *"Hola Eureka, me llamo `SALES-SERVICE` y estoy escuchando en el puerto 8081"*.
 3. Cada 30 segundos le manda un latido (*heartbeat*). Si `sales-service` se apaga, Eureka lo tacha de la lista para que nadie le envíe tráfico.

### 2. Relación con `catalog-service` (Puerto 8082 - Inventario NoSQL vía OpenFeign)
* **¿Qué problema resuelve?**: **Prevención de Fraude y Validación de Inventario**. 
 Si permitiéramos que el usuario o el frontend nos diga: *"Cobrame el Elden Ring a $0.01 centavos"*, cualquiera podría hackear la tienda modificando el JSON en el navegador.
* **¿Cómo interactúan (Paso a Paso con OpenFeign)?**:
 1. El cliente web manda a `sales-service`: *Quiero comprar 2 copias del juego ID "650c1f1e..."*.
 2. `sales-service` no confía en nadie: mediante `CatalogClient` acude a Eureka y le pide la dirección viva de `CATALOG-SERVICE`.
 3. Hace una llamada HTTP interna `GET /api/games/{id}` a `catalog-service`.
 4. `catalog-service` responde con el objeto `GameResponse` con el precio oficial de MongoDB ($59.99), el stock disponible y si el juego está activo.
 5. `sales-service` valida:
 * ¿El juego existe y `active == true`? Si no, rechaza la compra con `HTTP 400`.
 * ¿Hay suficiente stock (`stock >= quantity`)? Si no, rechaza la compra por inventario agotado.
 * **Blindaje de precio**: Multiplica la cantidad por el precio oficial traído de MongoDB (`game.price()`), ignorando cualquier precio manipulado por el usuario.
 6. Si todo es correcto, genera claves digitales únicas (`STEAM-XXXX`) y guarda la orden en la base de datos SQL H2.

---

## 3. Guía Pedagógica: ¿Cómo viaja una petición dentro de `sales-service`?

Imagina que este microservicio funciona exactamente igual a un **restaurante de alta cocina**:

```
[Cliente HTTP / Postman / Frontend]
 1. Envía JSON de compra al puerto 8081
 

 1. CAPA WEB: SaleOrderController 
 El "Mesero": Recibe la comanda del cliente. 
 Verifica con @Valid que no venga vacía o con letras raras
 y de inmediato se la entrega a la cocina (Service). 

 2. Llama al método createOrder()
 

 2. CAPA NEGOCIO: SaleOrderService 
 El "Chef": Tiene las recetas y las reglas del negocio. 
 - Llama a CatalogClient (OpenFeign) para pedir precio real
 - Verifica existencias en inventario 
 - Multiplica precios x cantidades con BigDecimal 
 - Genera claves digitales únicas estilo STEAM-XXXX 
 - Maneja la transacción con @Transactional (Rollback) 

 3. Llama a orderRepository.save()
 

 3. CAPA DATOS: SaleOrderRepository 
 La "Despensa": Es la única autorizada para tocar la base 
 de datos. Genera las instrucciones SQL automáticamente. 

 4. Sentencias SQL Hibernate
 

 4. BASE DE DATOS: H2 SQL (Tablas sale_orders y order_items) 

```

---

## 4. Anatomía Detallada de Clases (Clase por Clase)

### Paquete: `com.jonathan.gamestore.sales`

#### `SalesServiceApplication.java`
* **¿Qué es?**: La puerta principal de entrada y punto de arranque del microservicio.
* **Anotaciones clave**:
 * `@SpringBootApplication`: Enciende todo el ecosistema de Spring (inyección de dependencias, autoconfiguración, Tomcat interno).
 * `@EnableDiscoveryClient`: Registra la aplicación en Eureka Server (`http://localhost:8761`) con el nombre `SALES-SERVICE`.
 * `@EnableFeignClients`: Enciende el motor de OpenFeign para escanear y generar clientes HTTP automáticos hacia otros microservicios.
* **¿Quién la manda a llamar?**: El comando de ejecución de consola (`java -jar`) o el botón Play de IntelliJ.

---

### Paquete: `com.jonathan.gamestore.sales.client` (Comunicación entre Microservicios)

#### `CatalogClient.java` (Interfaz Feign)
* **¿Qué es?**: Cliente HTTP declarativo que permite llamar a `catalog-service` sin escribir código de conexión manual.
* **Anotaciones clave**:
 * `@FeignClient(name = "catalog-service")`: Le dice a Spring que busque en Eureka el microservicio llamado `catalog-service`.
 * `@GetMapping("/api/games/{id}")`: Mapea el método `getGameById(String id)` al endpoint de MongoDB del catálogo.

---

### Paquete: `com.jonathan.gamestore.sales.config`

#### `H2Config.java`
* **¿Qué es?**: Configuración del panel visual de la base de datos H2 en memoria.
* **¿Por qué existe?**: En versiones modernas de Spring Boot (Jakarta EE), la consola `/h2-console` requiere registrar manualmente el servlet `JakartaWebServlet` para poder ver las tablas desde el navegador en `http://localhost:8081/h2-console`.

---

### Paquete: `com.jonathan.gamestore.sales.model` (Entidades de Base de Datos)

#### `OrderStatus.java` (Enum)
* **¿Qué es?**: Catálogo fijo de opciones válidas para el estado de una compra (`PENDING`, `COMPLETED`, `CANCELLED`). Evita errores tipográficos.

#### `SaleOrder.java` (Entidad Padre)
* **¿Qué es?**: La tabla principal en SQL (`sale_orders`). Representa la factura o ticket de compra general.
* **Campos clave**:
 * `@Id @GeneratedValue`: Clave primaria autoincremental (1, 2, 3...).
 * `userId`: Identificador del usuario que compró.
 * `totalAmount`: Importe total a pagar calculado en servidor con `BigDecimal`.
 * `status`: Estado actual (`PENDING`, etc.).
 * `createdAt`: Fecha y hora de creación automática (`@PrePersist`).
 * `items`: Lista de productos contenidos en esta orden (`List<OrderItem>`).
* **Relación `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`**:
 * Significa: *"Una orden tiene muchos ítems"*.
 * `CascadeType.ALL`: Al guardar la orden en Java, Hibernate automáticamente guarda todos sus ítems en la tabla hija. Si borras la orden, se borran sus ítems en cascada.
* **Método `addItem(OrderItem item)`**: Método ayudante que asegura la relación bidireccional asignando este objeto orden como padre del ítem.

#### `OrderItem.java` (Entidad Hija)
* **¿Qué es?**: La tabla detalle en SQL (`order_items`). Representa cada juego individual dentro del carrito de compra.
* **Campos clave**:
 * `gameId`: Identificador del juego en el catálogo (de tipo `String` para ser compatible con los ObjectIds de 24 caracteres de MongoDB).
 * `gameTitle`: Nombre del juego para conservar el histórico (ej: *"Elden Ring"*).
 * `unitPrice`: Precio oficial al que se vendió en ese momento exacto.
 * `quantity`: Número de copias compradas.
 * `digitalKey`: La clave secreta de activación generada (`STEAM-XXXX`).
 * `order`: Referencia a la orden padre (`@ManyToOne` con clave foránea `order_id`).
 * `@JsonIgnore`: Evita que el serializador JSON entre en un ciclo infinito de recursión (Orden -> Ítem -> Orden -> Ítem...).

---

### Paquete: `com.jonathan.gamestore.sales.dto` (Data Transfer Objects)

#### `OrderItemRequest.java` (Record)
* **¿Qué es?**: Molde de datos que define qué información envía el cliente por cada juego solicitado (`gameId`, `gameTitle`, `unitPrice`, `quantity`).
* **Validaciones**: Anotado con `@NotBlank`, `@NotNull`, `@DecimalMin` y `@Positive`.

#### `SaleOrderRequest.java` (Record)
* **¿Qué es?**: Molde de datos del cuerpo de la petición de compra (`userId`, `items`).
* **Regla de oro de seguridad**: ¡Este DTO **NO** contiene el campo `totalAmount`! El cliente jamás puede decidir cuánto va a pagar.

#### `GameResponse.java` (Record)
* **¿Qué es?**: Molde para deserializar la respuesta JSON enviada por `catalog-service` a través de OpenFeign (`id`, `title`, `price`, `stock`, `active`, etc.).

---

### Paquete: `com.jonathan.gamestore.sales.repository` (Persistencia)

#### `SaleOrderRepository.java`
* **¿Qué es?**: Interfaz que extiende de `JpaRepository<SaleOrder, Long>`.
* **Consultas automáticas**: `save`, `findById`, `findAll` y `findByUserId(Long userId)` generadas en memoria por Spring Data JPA sin escribir SQL a mano.

---

### Paquete: `com.jonathan.gamestore.sales.service` (Lógica de Negocio)

#### `SaleOrderService.java`
* **¿Qué es?**: El cerebro de la aplicación donde residen las reglas comerciales y la integración con OpenFeign.
* **Anotación `@Transactional`**: Si se produce un error de stock o un corte de red a mitad de camino, hace **Rollback** automático y la base de datos queda limpia.
* **Métodos principales**:
 1. `createOrder(SaleOrderRequest request)`:
 * Consulta a `CatalogClient` por cada juego en MongoDB.
 * Valida que el juego exista y esté activo (`active == true`).
 * Valida existencias en stock (`stock >= quantity`).
 * Blinda el precio usando el oficial de MongoDB (`game.price()`).
 * Genera una clave digital única (`STEAM-` + UUID).
 * Calcula subtotales y total con `BigDecimal`.
 * Guarda la orden y sus ítems en cascada.
 2. `getAllOrders()`, `getOrderById(id)`, `getOrdersByUserId(userId)`, `updateOrder(id, request)`, `deleteOrder(id)`.

---

### Paquete: `com.jonathan.gamestore.sales.controller` (Capa Web REST)

#### `SaleOrderController.java`
* **¿Qué es?**: El "Mesero" que atiende peticiones HTTP en `/api/orders`.
* **Endpoints**:
 * `POST /api/orders`: Crea una nueva orden de venta validada contra el catálogo.
 * `GET /api/orders`: Lista todas las órdenes.
 * `GET /api/orders/{id}`: Busca una orden por su ID.
 * `GET /api/orders/user/{userId}`: Historial de compras de un usuario.
 * `PUT /api/orders/{id}`: Actualiza una orden.
 * `DELETE /api/orders/{id}`: Elimina una orden.

---

### Paquete: `com.jonathan.gamestore.sales.exception` (Manejo de Errores)

#### `ErrorResponse.java` (Record)
* **¿Qué es?**: Estructura estándar y profesional para reportar errores en formato JSON (`status`, `error`, `message`, `validationErrors`, `timestamp`).

#### `GlobalExceptionHandler.java`
* **¿Qué es?**: Interceptor global con `@RestControllerAdvice`.
* **Excepciones controladas**:
 * `MethodArgumentNotValidException`: Errores de validación de campos con HTTP 400.
 * `IllegalArgumentException`: Reglas de negocio rotas (stock insuficiente, juego inactivo) con HTTP 400.
 * `FeignException.NotFound`: Cuando el juego no existe en `catalog-service` devolviendo HTTP 404 claro.

---

## 5. Catálogo de Endpoints RESTful (`/api/orders`)

| Método | Endpoint | Descripción | Código Éxito | Códigos Error |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/orders` | Crea una nueva orden de venta validando precio y stock en `catalog-service` | `201 Created` | `400 Bad Request`, `404 Not Found` |
| `GET` | `/api/orders` | Lista todas las órdenes registradas en el sistema | `200 OK` | `500 Internal Error` |
| `GET` | `/api/orders/{id}` | Busca una orden por su ID numérico | `200 OK` | `404 Not Found` |
| `GET` | `/api/orders/user/{userId}` | Obtiene todas las compras realizadas por un usuario | `200 OK` | `200 OK (vacío)` |
| `PUT` | `/api/orders/{id}` | Modifica una orden existente recalculando con el catálogo | `200 OK` | `400 Bad Request`, `404 Not Found` |
| `DELETE` | `/api/orders/{id}` | Elimina una orden y sus ítems en cascada | `204 No Content` | `404 Not Found` |

---

## 6. Guía de Pruebas Rápidas con cURL / PowerShell

### Crear una Orden de Compra:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method Post -ContentType "application/json" -Body '{
 "userId": 101,
 "items": [
 {
 "gameId": "650c1f1e9b1d8b2bad000001",
 "gameTitle": "Elden Ring",
 "unitPrice": 59.99,
 "quantity": 1
 }
 ]
}' | ConvertTo-Json -Depth 5
```

### Consultar todas las Órdenes:
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method Get | ConvertTo-Json -Depth 5
```
