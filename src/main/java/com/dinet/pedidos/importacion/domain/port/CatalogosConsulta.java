package com.dinet.pedidos.importacion.domain.port;

import java.util.Optional;

public interface CatalogosConsulta {
    boolean existeCliente(String clienteId);
    Optional<Boolean> zonaSoportaRefrigeracion(String zonaId);
}