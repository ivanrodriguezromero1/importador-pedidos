package com.dinet.pedidos.importacion.adapters.in.web;

import com.dinet.pedidos.importacion.application.CargarPedidosHandler;
import com.dinet.pedidos.importacion.application.dto.ResumenCarga;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Pedidos")
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class CargarPedidosController {

    private final CargarPedidosHandler handler;

    @Operation(
            summary = "Cargar pedidos desde CSV",
            security = { @SecurityRequirement(name = "bearerAuth") }
    )
    @PostMapping(value = "/cargar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResumenDto> cargar(
            @Parameter(name = "Idempotency-Key", in = ParameterIn.HEADER, required = true)
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @RequestPart("file") MultipartFile archivo
    ) throws IOException {

        ResumenCarga r = handler.ejecutar(archivo.getBytes(), claveIdempotencia);
        ResumenDto respuesta = new ResumenDto(
                r.totalProcesados(), r.guardados(), r.conError(),
                r.erroresPorFila(), r.erroresAgrupados()
        );
        return ResponseEntity.ok(respuesta);
    }

    public record ResumenDto(int totalProcesados, int guardados, int conError,
                             List<?> erroresPorFila,
                             Map<String, Integer> erroresAgrupados) {}
}