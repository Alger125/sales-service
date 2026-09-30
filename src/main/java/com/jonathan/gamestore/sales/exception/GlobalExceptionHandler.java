package com.jonathan.gamestore.sales.exception;

import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * MANEJADOR GLOBAL DE EXCEPCIONES: GlobalExceptionHandler
 *
 * ¿Qué es y cómo funciona? (Concepto Senior explicado para Juniors)
 *
 * En programación tradicional, llenaríamos cada Controller con bloques `try { ... } catch (...)`.
 * Eso ensuciaría el código y rompería el principio DRY (Don't Repeat Yourself).
 *
 * Spring Boot nos proporciona la anotación `@RestControllerAdvice`:
 * Actúa como un "pararrayos global" que envuelve a TODOS los controladores de la aplicación.
 * Si cualquier controlador lanza una excepción que no fue atrapada, el flujo se desvía
 * automáticamente hacia los métodos de esta clase.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

 /**
 * Atrapa específicamente la excepción 'MethodArgumentNotValidException'.
 * Esta excepción es lanzada automáticamente por Spring cuando una petición con @Valid
 * no cumple alguna de las reglas (ej: un campo @NotNull viene nulo o un @Positive es negativo).
 */
 @ExceptionHandler(MethodArgumentNotValidException.class)
 public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
 Map<String, String> errors = new HashMap<>();

 ex.getBindingResult().getAllErrors().forEach((error) -> {
 String fieldName = ((FieldError) error).getField();
 String errorMessage = error.getDefaultMessage();
 errors.put(fieldName, errorMessage);
 });

 ErrorResponse response = new ErrorResponse(
 HttpStatus.BAD_REQUEST.value(),
 HttpStatus.BAD_REQUEST.getReasonPhrase(),
 "Error en la validacion de los datos enviados",
 errors,
 LocalDateTime.now()
 );

 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
 }

 /**
 * Atrapa excepciones de reglas de negocio (ej: stock insuficiente o juego inexistente).
 * Devuelve HTTP 400 Bad Request con un mensaje claro en español.
 */
 @ExceptionHandler(IllegalArgumentException.class)
 public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
 ErrorResponse response = new ErrorResponse(
 HttpStatus.BAD_REQUEST.value(),
 HttpStatus.BAD_REQUEST.getReasonPhrase(),
 ex.getMessage(),
 null,
 LocalDateTime.now()
 );
 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
 }

 /**
 * Atrapa errores cuando OpenFeign llama a catalog-service y el juego no existe (HTTP 404).
 */
 @ExceptionHandler(FeignException.NotFound.class)
 public ResponseEntity<ErrorResponse> handleFeignNotFound(FeignException.NotFound ex) {
 ErrorResponse response = new ErrorResponse(
 HttpStatus.NOT_FOUND.value(),
 "Game Not Found In Catalog",
 "El videojuego solicitado no fue encontrado en el catalogo de productos.",
 null,
 LocalDateTime.now()
 );
 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
 }
}
