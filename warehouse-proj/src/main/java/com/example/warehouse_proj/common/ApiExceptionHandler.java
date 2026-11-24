package com.example.warehouse_proj.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gives API errors a {@code message} the client can show. Spring leaves it out by default, so a
 * rule like "Truck still has shipments" or "firstName must contain letters only" would only reach
 * the user as "Request failed with status code 409/400".
 *
 * <p>Only messages we wrote ourselves are sent (the ResponseStatusException reason and the
 * validation messages), never internal exception text.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> responseStatus(ResponseStatusException e) {
        return respond(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return respond(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Invalid request" : message);
    }

    /** Bad JSON, an unknown enum value such as status "WRONG", or an unparseable date. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> unreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "The request body could not be read: check the values and their formats");
    }

    private static ResponseEntity<Map<String, Object>> respond(HttpStatusCode code, String message) {
        HttpStatus known = HttpStatus.resolve(code.value());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", code.value());
        body.put("error", known != null ? known.getReasonPhrase() : "Error");
        body.put("message", message);
        return ResponseEntity.status(code).body(body);
    }
}
