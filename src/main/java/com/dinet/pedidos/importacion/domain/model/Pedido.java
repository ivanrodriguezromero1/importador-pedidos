package com.dinet.pedidos.importacion.domain.model;

import java.time.LocalDate;

public record Pedido(
        String numeroPedido,
        String clienteId,
        LocalDate fechaEntrega,
        Estado estado,
        String zonaId,
        boolean requiereRefrigeracion
) {}
