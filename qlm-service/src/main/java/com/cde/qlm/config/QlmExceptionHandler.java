package com.cde.qlm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import java.time.OffsetDateTime;
import java.util.*;

@RestControllerAdvice @Slf4j
public class QlmExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<Map<String, Object>> notFound(NoSuchElementException ex) {
        return err(HttpStatus.NOT_FOUND, ex.getMessage());
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, Object>> badReq(IllegalArgumentException ex) {
        return err(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> generic(Exception ex) {
        log.error("Unhandled: {}", ex.getMessage(), ex);
        return err(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }
    private ResponseEntity<Map<String, Object>> err(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("status", s.value(), "message", msg,
                "timestamp", OffsetDateTime.now().toString()));
    }
}
