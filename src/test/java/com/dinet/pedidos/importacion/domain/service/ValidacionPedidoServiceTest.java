package com.dinet.pedidos.importacion.domain.service;

import com.dinet.pedidos.importacion.domain.model.Estado;
import com.dinet.pedidos.importacion.domain.model.Pedido;
import com.dinet.pedidos.importacion.domain.port.CatalogosConsulta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class ValidacionPedidoServiceTest {

    CatalogosConsulta catalogos;
    Clock clock;
    ValidacionPedidoService svc;

    @BeforeEach
    void setUp() {
        catalogos = Mockito.mock(CatalogosConsulta.class);
        // “hoy” fijo: 2025-09-19 en America/Lima
        clock = Clock.fixed(Instant.parse("2025-09-19T05:00:00Z"), ZoneId.of("America/Lima"));
        svc = new ValidacionPedidoService(catalogos, clock);
    }

    private Pedido pedidoOk() {
        return new Pedido("P001", "CLI-123",
                java.time.LocalDate.of(2025, 12, 10), Estado.PENDIENTE, "ZONA1", true);
    }

    @Test
    void pasa_si_todo_ok() {
        var p = pedidoOk();
        when(catalogos.existeCliente("CLI-123")).thenReturn(true);
        when(catalogos.zonaSoportaRefrigeracion("ZONA1")).thenReturn(Optional.of(true));

        List<String> errs = svc.validar(p);
        assertThat(errs).isEmpty();
    }

    @Test
    void error_si_cliente_no_existe() {
        var p = pedidoOk();
        when(catalogos.existeCliente("CLI-123")).thenReturn(false);
        when(catalogos.zonaSoportaRefrigeracion("ZONA1")).thenReturn(Optional.of(true));

        assertThat(svc.validar(p)).containsExactly("CLIENTE_NO_ENCONTRADO");
    }

    @Test
    void error_si_zona_invalida() {
        var p = pedidoOk();
        when(catalogos.existeCliente("CLI-123")).thenReturn(true);
        when(catalogos.zonaSoportaRefrigeracion("ZONA1")).thenReturn(Optional.empty());

        assertThat(svc.validar(p)).containsExactly("ZONA_INVALIDA");
    }

    @Test
    void error_si_cadena_frio_no_soportada() {
        var p = pedidoOk(); // requiereRefrigeracion = true
        when(catalogos.existeCliente("CLI-123")).thenReturn(true);
        when(catalogos.zonaSoportaRefrigeracion("ZONA1")).thenReturn(Optional.of(false));

        assertThat(svc.validar(p)).containsExactly("CADENA_FRIO_NO_SOPORTADA");
    }

    @Test
    void error_si_fecha_pasada() {
        var p = new Pedido("P001","CLI-123",
                java.time.LocalDate.of(2024, 12, 10), Estado.PENDIENTE, "ZONA1", true);
        when(catalogos.existeCliente("CLI-123")).thenReturn(true);
        when(catalogos.zonaSoportaRefrigeracion("ZONA1")).thenReturn(Optional.of(true));

        assertThat(svc.validar(p)).containsExactly("FECHA_INVALIDA");
    }
}
