package com.dinet.pedidos.importacion.domain.port;

import java.util.Optional;

public interface IdempotenciaAlmacen {
    enum Estado { COMPLETED }

    boolean registrarInicio(String claveIdempotencia, String archivoHash);

    Optional<Estado> estadoDe(String claveIdempotencia, String archivoHash);

    default void marcarCompletado(String claveIdempotencia, String archivoHash, String resultadoJson) {}
}
