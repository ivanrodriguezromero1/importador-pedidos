package com.dinet.pedidos.importacion.adapters.out.catalogo;

import com.dinet.pedidos.importacion.domain.port.CatalogosConsulta;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CatalogosConsultaJdbc implements CatalogosConsulta {

    private final JdbcTemplate jdbc;

    @Override
    public boolean existeCliente(String clienteId) {
        Boolean exists = jdbc.queryForObject(
                "select exists (select 1 from clientes where id = ? and activo = true)",
                Boolean.class, clienteId
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<Boolean> zonaSoportaRefrigeracion(String zonaId) {
        List<Boolean> list = jdbc.query(
                "select soporte_refrigeracion from zonas where id = ?",
                (rs, i) -> rs.getBoolean(1),
                zonaId
        );
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
