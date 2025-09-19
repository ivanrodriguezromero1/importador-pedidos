package com.dinet.pedidos.importacion.domain.service;

import com.dinet.pedidos.importacion.domain.model.Pedido;
import com.dinet.pedidos.importacion.domain.port.CatalogosConsulta;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidacionPedidoService {

    private final CatalogosConsulta catalogos;
    private final Clock clock;

    public List<String> validar(Pedido p) {
        List<String> errores = new ArrayList<>();

        if (!catalogos.existeCliente(p.clienteId())) {
            errores.add("CLIENTE_NO_ENCONTRADO");
        }

        var soporteOpt = catalogos.zonaSoportaRefrigeracion(p.zonaId());
        if (soporteOpt.isEmpty()) {
            errores.add("ZONA_INVALIDA");
        } else if (p.requiereRefrigeracion() && !soporteOpt.get()) {
            errores.add("CADENA_FRIO_NO_SOPORTADA");
        }

        LocalDate hoy = LocalDate.now(clock);
        if (p.fechaEntrega().isBefore(hoy)) {
            errores.add("FECHA_INVALIDA");
        }

        return errores;
    }
}
