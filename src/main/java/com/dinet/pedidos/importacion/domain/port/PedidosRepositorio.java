package com.dinet.pedidos.importacion.domain.port;

import com.dinet.pedidos.importacion.domain.model.Pedido;
import java.util.List;

public interface PedidosRepositorio {
    void upsertPorLote(List<Pedido> pedidos);
}
