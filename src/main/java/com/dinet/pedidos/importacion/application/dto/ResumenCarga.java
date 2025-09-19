package com.dinet.pedidos.importacion.application.dto;

import java.util.List;
import java.util.Map;

public record ResumenCarga(
        int totalProcesados,
        int guardados,
        int conError,
        List<ErrorFila> erroresPorFila,
        Map<String, Integer> erroresAgrupados
) {
    public static ResumenCarga vacio() {
        return new ResumenCarga(0,0,0, List.of(), Map.of());
    }
}
