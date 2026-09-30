# Sales Service (sales-service)

> **Microservicio de Gestion de Ventas, Ordenes, Facturacion y Generacion de Claves Digitales.**  
> Desarrollado con **Java 17**, **Spring Boot 4**, **Spring Data JPA**, **H2 Database**, **Spring Cloud Netflix Eureka** y **Spring Cloud OpenFeign**.

---

## 1. Introduccion y Justificacion Arquitectonica

Una de las preguntas fundamentales en arquitectura de software empresarial es:  
**¿Por que ventas es un microservicio autonomo e independiente?**

### Principio de Base de Datos Propia (Database-per-Service Pattern)
* En un monolito tradicional, un fallo en la base de datos detiene toda la empresa.
* `sales-service` **posee su propia base de datos relacional (H2 / SQL)**. Ningun otro microservicio tiene permitido conectarse directamente por JDBC a las tablas de ventas.
* **¿Por que SQL para Ventas?** Las ventas involucran transacciones monetarias y exigen cumplimiento estricto de propiedades **ACID** (Atomicidad, Consistencia, Aislamiento y Durabilidad).
* Si `catalog-service` se encuentra bajo mantenimiento o sufre una caida, `sales-service` continua operativo, manteniendo aislada su base de datos.

### Escalabilidad Horizontal Independiente
En temporadas de alta demanda (como promociones o ventas especiales), el volumen de transacciones de compra puede multiplicarse exponencialmente mientras que el catalogo de productos permanece estatico. La independencia permite escalar multiples instancias de `sales-service` sin desperdiciar memoria en componentes que no experimentan carga.

---

## 2. Mapeo Exhaustivo de Comunicacion Inter-Microservicios

Esta seccion documenta exactamente **donde, como y a traves de que archivos y componentes tecnicos `sales-service` mapea y consume los demas microservicios del ecosistema**.

### Diagrama de Enrutamiento y Resolucion de Nombres

```
                                  +-----------------------------+
                                  |        EUREKA SERVER        |
                                  |   (Directorio Central)      |
                                  |         Puerto 8761         |
                                  +--------------+--------------+
                                                 |
                          +----------------------+----------------------+
                          | 1. Heartbeat / Registro                     | 1. Heartbeat / Registro
                          |    "SALES-SERVICE en 8081"                  |    "CATALOG-SERVICE en 8082"
                          v                                             v
            +---------------------------+                 +---------------------------+
            |       SALES-SERVICE       |                 |      CATALOG-SERVICE      |
            |        Puerto 8081        |                 |        Puerto 8082        |
            |     Base de Datos H2      |                 |    Base de Datos Mongo    |
            |   (Transacciones SQL)     |                 |     (Catalogo NoSQL)      |
            +-------------+-------------+                 +-------------+-------------+
                          |                                             ^
                          | 2. Peticion HTTP declarativa OpenFeign      |
                          |    GET http://catalog-service/api/games/{id}|
                          +---------------------------------------------+
```

---

### Mapeo 1: Descubrimiento y Registro con `eureka-server` (Puerto 8761)

* **Archivo de configuracion**: `src/main/resources/application.properties`
  ```properties
  spring.application.name=sales-service
  server.port=8081
  eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
  ```
* **Punto de activacion en codigo Java**: `SalesServiceApplication.java`
  ```java
  @SpringBootApplication
  @EnableDiscoveryClient  // Activa el registro dinamico con Eureka Server
  @EnableFeignClients     // Activa el escaneo y construccion de clientes Feign
  public class SalesServiceApplication { ... }
  ```
* **Mecanismo de operacion**:
  1. Al arrancar en el puerto `8081`, el cliente de Eureka emite un registro HTTP hacia `http://localhost:8761/eureka/apps/SALES-SERVICE`.
  2. Emite latidos de salud (*heartbeats*) cada 30 segundos para confirmar disponibilidad.
  3. Descarga la cache local con la tabla de localizacion de todos los demas microservicios activos.

---

### Mapeo 2: Comunicacion Declarativa Sincrona con `catalog-service` (Puerto 8082)

`sales-service` **NO** tiene codificadas direcciones IP fijas ni puertos como `localhost:8082`. La resolucion es completamente dinamica a traves del nombre de servicio registrado en Eureka.

#### A. Interfaz del Cliente Feign: `CatalogClient.java`
* **Ubicacion**: `src/main/java/com/jonathan/gamestore/sales/client/CatalogClient.java`
* **Codigo de Mapeo**:
  ```java
  @FeignClient(name = "catalog-service")
  public interface CatalogClient {

      @GetMapping("/api/games/{id}")
      GameResponse getGameById(@PathVariable("id") String id);
  }
  ```
* **Explicacion tecnica del mapeo**:
  * `@FeignClient(name = "catalog-service")`: Le indica a Spring Cloud que resuelva el host `catalog-service` consultando el directorio de Eureka. Si existen 3 instancias de catalogo, Spring Cloud LoadBalancer reparte el trafico entre ellas.
  * `@GetMapping("/api/games/{id}")`: Mapea directamente contra el endpoint expuesto por `GameController` en `catalog-service`.
  * `@PathVariable("id") String id`: Envia el ObjectId de 24 caracteres de MongoDB.

#### B. Molde de Deserializacion Remota: `GameResponse.java`
* **Ubicacion**: `src/main/java/com/jonathan/gamestore/sales/dto/GameResponse.java`
* **Funcion**:
  Dado que `sales-service` no tiene acceso a las clases de `catalog-service`, este Java `record` replica el contrato JSON devuelto por MongoDB:
  ```java
  public record GameResponse(
          String id,
          String title,
          String description,
          String genre,
          BigDecimal price,     // Precio oficial validado
          Integer stock,        // Existencias reales en almacen
          List<String> platforms,
          Boolean active        // Estado de publicacion
  ) {}
  ```

#### C. Inyeccion y Orquestacion en la Capa de Negocio: `SaleOrderService.java`
* **Ubicacion**: `src/main/java/com/jonathan/gamestore/sales/service/SaleOrderService.java`
* **Flujo de validacion y blindaje de precios**:
  ```java
  // Inyeccion automatica del cliente Feign
  private final CatalogClient catalogClient;

  @Transactional
  public SaleOrder createOrder(SaleOrderRequest request) {
      for (OrderItemRequest itemReq : request.items()) {
          // LLAMADA REMOTA AL MICROSERVICIO DE CATALOGO:
          GameResponse game = catalogClient.getGameById(itemReq.gameId());

          // 1. Validacion de existencia y estado activo
          if (game == null || !Boolean.TRUE.equals(game.active())) {
              throw new IllegalArgumentException("Videojuego inactivo o inexistente");
          }

          // 2. Validacion de existencias en tiempo real
          if (game.stock() == null || game.stock() < itemReq.quantity()) {
              throw new IllegalArgumentException("Stock insuficiente para: " + game.title());
          }

          // 3. BLINDAJE DE PRECIO (Anti-Fraude):
          // Se toma el precio oficial de MongoDB (game.price()), ignorando datos alterados del cliente
          BigDecimal officialPrice = game.price();
          ...
      }
  }
  ```

---

## 3. Arquitectura Interna por Capas (Clean Architecture)

El flujo interno de procesamiento en `sales-service` desacopla la recepcion, la logica y la persistencia:

```
[Peticion HTTP entrante desde Cliente / Postman / Frontend]
                 | 1. POST /api/orders
                 v
+-------------------------------------------------------------+
| 1. CAPA CONTROLADOR: SaleOrderController                    |
|    - Recibe el payload JSON                                 |
|    - Ejecuta validaciones estructurales (@Valid)            |
|    - Delega el procesamiento a la capa de servicio          |
+-----------------------------+-------------------------------+
                               | 2. createOrder(request)
                               v
+-------------------------------------------------------------+
| 2. CAPA SERVICIO: SaleOrderService                          |
|    - Invoca CatalogClient (OpenFeign)                       |
|    - Valida existencia, estado activo y stock               |
|    - Calcula el importe oficial con BigDecimal              |
|    - Genera las claves digitales (STEAM-UUID)               |
|    - Garantiza atomicidad transaccional (@Transactional)    |
+-----------------------------+-------------------------------+
                               | 3. orderRepository.save(order)
                               v
+-------------------------------------------------------------+
| 3. CAPA REPOSITORIO: SaleOrderRepository                    |
|    - Interfaz Spring Data JPA                               |
|    - Genera sentencias SQL automaticas                      |
+-----------------------------+-------------------------------+
                               | 4. Persistencia relacional
                               v
+-------------------------------------------------------------+
| 4. BASE DE DATOS: H2 SQL (Tablas sale_orders y order_items) |
+-------------------------------------------------------------+
```

---

## 4. Anatomia Detallada de Clases y Componentes

### Paquete: `com.jonathan.gamestore.sales`

#### `SalesServiceApplication.java`
* Punto de entrada de Spring Boot.
* Habilita `@SpringBootApplication`, `@EnableDiscoveryClient` y `@EnableFeignClients`.

---

### Paquete: `com.jonathan.gamestore.sales.client`

#### `CatalogClient.java`
* Interfaz declarativa de OpenFeign que implementa la comunicacion HTTP hacia `catalog-service`.

---

### Paquete: `com.jonathan.gamestore.sales.config`

#### `H2Config.java`
* Registra el servlet `JakartaWebServlet` para habilitar el acceso a la consola de base de datos H2 en `http://localhost:8081/h2-console`.

---

### Paquete: `com.jonathan.gamestore.sales.model` (Entidades de Dominio)

#### `OrderStatus.java` (Enum)
* Catalogo formal de estados de orden: `PENDING`, `COMPLETED`, `CANCELLED`.

#### `SaleOrder.java` (Entidad Raiz)
* Mapea la tabla `sale_orders`.
* Atributos: `id` (autoincremental), `userId`, `totalAmount`, `status`, `createdAt`.
* Relacion `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`: Garantiza que los items se guarden o eliminen conjuntamente con la orden.

#### `OrderItem.java` (Entidad Detalle)
* Mapea la tabla `order_items`.
* Atributos: `gameId` (de tipo `String` compatible con ObjectIds de MongoDB), `gameTitle`, `unitPrice`, `quantity`, `digitalKey`.
* Relacion `@ManyToOne`: Referencia a la orden padre mediante clave foranea `order_id`.

---

### Paquete: `com.jonathan.gamestore.sales.dto`

#### `SaleOrderRequest.java`
* Record que recibe los datos de compra (`userId`, `List<OrderItemRequest> items`). Omite deliberadamente `totalAmount` por seguridad.

#### `OrderItemRequest.java`
* Record con los articulos solicitados (`gameId`, `quantity`, `unitPrice`).

#### `GameResponse.java`
* Record para almacenar la respuesta devuelta por `catalog-service`.

---

### Paquete: `com.jonathan.gamestore.sales.repository`

#### `SaleOrderRepository.java`
* Interfaz `JpaRepository<SaleOrder, Long>`. Expone consultas derivadas como `findByUserId(Long userId)`.

---

### Paquete: `com.jonathan.gamestore.sales.service`

#### `SaleOrderService.java`
* Orquestador central de la logica de negocio, calculos monetarios e invocaciones a OpenFeign.

---

### Paquete: `com.jonathan.gamestore.sales.controller`

#### `SaleOrderController.java`
* Controlador REST expuesto en `/api/orders`.

---

### Paquete: `com.jonathan.gamestore.sales.exception`

#### `ErrorResponse.java`
* Record que estandariza la estructura de error HTTP devuelta al cliente (`status`, `error`, `message`, `validationErrors`, `timestamp`).

#### `GlobalExceptionHandler.java`
* Interceptor `@RestControllerAdvice` que captura `MethodArgumentNotValidException`, `IllegalArgumentException` y fallos 404 de OpenFeign.

---

## 5. Catalogo Completo de Endpoints RESTful (`/api/orders`)

| Operacion | Metodo HTTP | Ruta Endpoint | Descripcion del Recurso | Codigo Exito | Codigos Falla |
| :--- | :---: | :--- | :--- | :---: | :---: |
| **CREATE** | `POST` | `/api/orders` | Registra una orden validando precio y stock en `catalog-service` | `201 Created` | `400 Bad Request`, `404 Not Found` |
| **READ (All)** | `GET` | `/api/orders` | Lista todas las ordenes emitidas en el sistema | `200 OK` | `500 Internal Error` |
| **READ (ById)**| `GET` | `/api/orders/{id}` | Recupera el detalle de una orden por su identificador numerico | `200 OK` | `404 Not Found` |
| **READ (User)**| `GET` | `/api/orders/user/{userId}` | Filtra las ordenes correspondientes a un usuario | `200 OK` | - |
| **UPDATE** | `PUT` | `/api/orders/{id}` | Actualiza una orden recalculando montos contra el catalogo | `200 OK` | `404 Not Found`, `400 Bad Request` |
| **DELETE** | `DELETE` | `/api/orders/{id}` | Elimina una orden y sus detalles asociados en cascada | `204 No Content` | `404 Not Found` |

---

## 6. Guia Exhaustiva de Pruebas (PowerShell y cURL)

### 1. Registrar una Orden de Compra (Validada via OpenFeign)
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method Post -ContentType "application/json" -Body '{
  "userId": 101,
  "items": [
    {
      "gameId": "650c1f1e9b1d8b2bad000001",
      "gameTitle": "Elden Ring",
      "unitPrice": 59.99,
      "quantity": 2
    }
  ]
}' | ConvertTo-Json -Depth 5
```
*Respuesta exitosa:* Retorna la orden con estado `PENDING`, el `totalAmount` calculado oficialmente y las claves digitales `STEAM-XXXX` autogeneradas.

---

### 2. Consultar todas las ordenes
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method Get | ConvertTo-Json -Depth 5
```

---

### 3. Consultar una orden especifica por ID
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders/1" -Method Get | ConvertTo-Json -Depth 5
```

---

### 4. Consultar ordenes por ID de Usuario
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders/user/101" -Method Get | ConvertTo-Json -Depth 5
```

---

### 5. Eliminar una orden existente
```powershell
Invoke-WebRequest -Uri "http://localhost:8081/api/orders/1" -Method Delete
```

---

## 7. Instrucciones de Ejecucion Optimizada (8 GB RAM Setup)

```powershell
cd C:\Users\ErickJimz\IdeaProjects\sales-service
.\mvnw.cmd clean package -DskipTests
java -Xmx300m -jar .\target\sales-service-0.0.1-SNAPSHOT.jar
```
* **Puerto configurado:** `8081`
* **Consola H2 Database:** `http://localhost:8081/h2-console`
* **JDBC URL:** `jdbc:h2:mem:salesdb`


---

## 8. Flujo de Interaccion Integral del Ecosistema (End-to-End)

Esta seccion describe la secuencia operativa completa que conecta a **`eureka-server`**, **`catalog-service`** y **`sales-service`** en un escenario real de compra de videojuegos.

### Diagrama de Secuencia de la Interaccion Completa

```
[Usuario / Postman]     [sales-service:8081]      [eureka-server:8761]      [catalog-service:8082]     [MongoDB:27017]
         |                       |                         |                          |                       |
         |=== 1. Registro inicial en el ecosistema =========================================================|
         |                       |                         |<--- Registra CATALOG ----|                       |
         |                       |<--- Registra SALES -----|                          |                       |
         |                       |                         |                          |                       |
         |=== 2. Alta de Videojuego en Catalogo ============================================================|
         |-- POST /api/games -------------------------------------------------------->|                       |
         |   (Elden Ring, $59.99, stock: 10)               |                          |-- save(Game) -------->|
         |                                                 |                          |<-- id: "674a123f..." -|
         |<-- HTTP 201 Created (id: "674a123f...") -----------------------------------|                       |
         |                                                 |                          |                       |
         |=== 3. Intento de Compra con Validacion Sincrona (OpenFeign) =====================================|
         |-- POST /api/orders ---------------------------->|                          |                       |
         |   (gameId: "674a123f...", qty: 2)               |                          |                       |
         |                       |-- 3.1 Resolucion ------>|                          |                       |
         |                       |    "¿Donde esta CATALOG?"                          |                       |
         |                       |<-- Retorna 8082 --------|                          |                       |
         |                       |                                                    |                       |
         |                       |-- 3.2 GET /api/games/674a123f... (OpenFeign) ----->|                       |
         |                       |                                                    |-- findById() -------->|
         |                       |                                                    |<-- Game Document -----|
         |                       |<-- HTTP 200 OK (Price: $59.99, Stock: 10) ---------|                       |
         |                       |                                                    |                       |
         |                       |-- 3.3 Reglas de Negocio en Servidor:               |                       |
         |                       |   a) Verifica: stock (10) >= cantidad (2) -> OK    |                       |
         |                       |   b) Blindaje: Aplica $59.99 (ignora cliente)      |                       |
         |                       |   c) Genera CD-Key: "STEAM-A8F2-4B1C..."           |                       |
         |                       |   d) Persiste en H2 SQL (Transaccion ACID)         |                       |
         |<-- HTTP 201 Created --|                                                    |                       |
         |   (Total: $119.98, CD-Keys generadas)                                      |                       |
```

### Guia de Reproduccion Paso a Paso de la Interaccion

#### Paso 1: Inicializar Eureka Server
En una terminal:
```powershell
cd C:\Users\ErickJimz\IdeaProjects\eureka-server
.\mvnw.cmd spring-boot:run
```
*Verificar:* Abrir el navegador en `http://localhost:8761` (Dashboard de Eureka activo).

#### Paso 2: Inicializar Catalog Service
En una segunda terminal:
```powershell
cd C:\Users\ErickJimz\IdeaProjects\catalog-service
.\mvnw.cmd spring-boot:run
```
*Verificar:* En `http://localhost:8761` aparecera registrado el nodo **`CATALOG-SERVICE`** en estado `UP`.

#### Paso 3: Inicializar Sales Service
En una tercera terminal:
```powershell
cd C:\Users\ErickJimz\IdeaProjects\sales-service
.\mvnw.cmd spring-boot:run
```
*Verificar:* En `http://localhost:8761` aparecera registrado el nodo **`SALES-SERVICE`** en estado `UP`.

#### Paso 4: Crear el Videojuego en el Catalogo (MongoDB)
```powershell
$gameResponse = Invoke-RestMethod -Uri "http://localhost:8082/api/games" -Method Post -ContentType "application/json" -Body '{
  "title": "Elden Ring",
  "description": "Edicion Estandar",
  "genre": "RPG",
  "price": 59.99,
  "stock": 10,
  "platforms": ["PC", "PS5"]
}'
$gameId = $gameResponse.id
Write-Host "Juego registrado con ID NoSQL: $gameId"
```

#### Paso 5: Emitir la Orden de Compra en Ventas (Consumiendo Catalogo via OpenFeign)
```powershell
Invoke-RestMethod -Uri "http://localhost:8081/api/orders" -Method Post -ContentType "application/json" -Body @"
{
  "userId": 101,
  "items": [
    {
      "gameId": "$gameId",
      "gameTitle": "Elden Ring",
      "unitPrice": 59.99,
      "quantity": 2
    }
  ]
}
"@ | ConvertTo-Json -Depth 5
```

*Resultado observable:*
* `sales-service` consulta de forma invisible a `catalog-service` a traves de Eureka.
* Valida existencias y precio en MongoDB.
* Genera las claves digitales seguras para el usuario.
* Retorna la orden con el monto oficial calculado ($119.98).
