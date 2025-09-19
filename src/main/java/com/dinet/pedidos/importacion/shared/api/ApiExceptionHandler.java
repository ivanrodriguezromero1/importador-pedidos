package com.dinet.pedidos.importacion.shared.api;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.*;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    private String cid() { return MDC.get("correlationId"); }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> missingHeader(MissingRequestHeaderException ex) {
        var body = new ErrorResponse("BAD_REQUEST", ex.getMessage(), List.of("Header requerido"), cid());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooBig(MaxUploadSizeExceededException ex) {
        var body = new ErrorResponse("PAYLOAD_TOO_LARGE", "Archivo demasiado grande", List.of(), cid());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception ex) {
        log.error("Error no controlado", ex);
        var body = new ErrorResponse("INTERNAL_ERROR", "Error interno", List.of(), cid());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

