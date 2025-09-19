package com.dinet.pedidos.importacion.adapters.out.jpa;

import com.dinet.pedidos.importacion.domain.model.Pedido;
import com.dinet.pedidos.importacion.domain.port.PedidosRepositorio;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class PedidosRepositorioJdbc implements PedidosRepositorio {

    private final JdbcTemplate jdbc;

    @Override
    public void upsertPorLote(List<Pedido> pedidos) {
        if (pedidos == null || pedidos.isEmpty()) return;

        String sql = """
      insert into pedidos(numero_pedido, cliente_id, zona_id, fecha_entrega, estado, requiere_refrigeracion)
      values (?, ?, ?, ?, ?, ?)
      on conflict (numero_pedido) do update
         set cliente_id = excluded.cliente_id,
             zona_id = excluded.zona_id,
             fecha_entrega = excluded.fecha_entrega,
             estado = excluded.estado,
             requiere_refrigeracion = excluded.requiere_refrigeracion,
             actualizado_en = now()
      """;

        jdbc.batchUpdate(sql, pedidos, pedidos.size(), (ps, p) -> {
            ps.setString(1, p.numeroPedido());
            ps.setString(2, p.clienteId());
            ps.setString(3, p.zonaId());
            ps.setObject(4, p.fechaEntrega());
            ps.setString(5, p.estado().name());
            ps.setBoolean(6, p.requiereRefrigeracion());
        });
    }
}
