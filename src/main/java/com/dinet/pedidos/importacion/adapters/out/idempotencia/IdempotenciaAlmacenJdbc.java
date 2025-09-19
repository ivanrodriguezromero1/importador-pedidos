package com.dinet.pedidos.importacion.adapters.out.idempotencia;

import com.dinet.pedidos.importacion.domain.port.IdempotenciaAlmacen;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class IdempotenciaAlmacenJdbc implements IdempotenciaAlmacen {

    private final JdbcTemplate jdbc;

    @Override
    public boolean registrarInicio(String claveIdempotencia, String archivoHash) {
        try {
            jdbc.update("insert into cargas_idempotencia(clave_idempotencia, archivo_hash) values (?,?)",
                    claveIdempotencia, archivoHash);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    @Override
    public Optional<Estado> estadoDe(String claveIdempotencia, String archivoHash) {
        Boolean exists = jdbc.queryForObject(
                "select exists (select 1 from cargas_idempotencia where clave_idempotencia=? and archivo_hash=?)",
                Boolean.class, claveIdempotencia, archivoHash);
        return Boolean.TRUE.equals(exists) ? Optional.of(Estado.COMPLETED) : Optional.empty();
    }
}
