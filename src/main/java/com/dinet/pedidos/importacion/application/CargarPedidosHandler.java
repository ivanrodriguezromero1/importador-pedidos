package com.dinet.pedidos.importacion.application;

import com.dinet.pedidos.importacion.application.dto.ErrorFila;
import com.dinet.pedidos.importacion.application.dto.ResumenCarga;
import com.dinet.pedidos.importacion.domain.model.Pedido;
import com.dinet.pedidos.importacion.domain.port.CatalogosConsulta;
import com.dinet.pedidos.importacion.domain.port.IdempotenciaAlmacen;
import com.dinet.pedidos.importacion.domain.port.PedidosRepositorio;
import com.dinet.pedidos.importacion.domain.service.ValidacionPedidoService;
import com.dinet.pedidos.importacion.shared.csv.CsvPedidosParser;
import com.dinet.pedidos.importacion.shared.crypto.Hashes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CargarPedidosHandler {

    private final CatalogosConsulta catalogos;
    private final PedidosRepositorio pedidosRepo;
    private final IdempotenciaAlmacen idem;
    private final ValidacionPedidoService validacion;

    @Value("${app.batch.size:500}")
    private int batchSize;

    public ResumenCarga ejecutar(byte[] csvBytes, String claveIdempotencia) {
        String hash = Hashes.sha256Hex(csvBytes);

        if (idem.estadoDe(claveIdempotencia, hash).isPresent()) {
            return ResumenCarga.vacio();
        }

        if (!idem.registrarInicio(claveIdempotencia, hash)) {
            return ResumenCarga.vacio();
        }

        try {
            var parsed = CsvPedidosParser.parse(csvBytes);

            var errores = new ArrayList<>(parsed.errores());
            var hoyLima = LocalDate.now(ZoneId.of("America/Lima"));

            List<Pedido> aGuardar = new ArrayList<>();
            for (var fv : parsed.filasValidas()) {
                var p = fv.pedido();
                int linea = fv.linea();


                if (!catalogos.existeCliente(p.clienteId())) {
                    errores.add(new ErrorFila(linea, "CLIENTE_NO_ENCONTRADO")); continue;
                }
                var soporte = catalogos.zonaSoportaRefrigeracion(p.zonaId());
                if (soporte.isEmpty()) {
                    errores.add(new ErrorFila(linea, "ZONA_INVALIDA")); continue;
                }
                if (p.requiereRefrigeracion() && !soporte.get()) {
                    errores.add(new ErrorFila(linea, "CADENA_FRIO_NO_SOPORTADA")); continue;
                }
                if (p.fechaEntrega().isBefore(hoyLima)) {
                    errores.add(new ErrorFila(linea, "FECHA_INVALIDA")); continue;
                }

                var codigos = validacion.validar(p);
                if (!codigos.isEmpty()) {
                    for (String c : codigos) errores.add(new ErrorFila(linea, c));
                    continue;
                }
                aGuardar.add(p);
            }

            int guardados = 0;
            for (int i = 0; i < aGuardar.size(); i += batchSize) {
                int fin = Math.min(i + batchSize, aGuardar.size());
                var sub = aGuardar.subList(i, fin);
                pedidosRepo.upsertPorLote(sub);
                guardados += sub.size();
            }

            int totalProcesados = parsed.filasValidas().size() + parsed.errores().size();
            int conError = errores.size();

            Map<String,Integer> agrupados = new HashMap<>();
            for (ErrorFila e : errores) agrupados.merge(e.motivo(), 1, Integer::sum);

            return new ResumenCarga(totalProcesados, guardados, conError, errores, agrupados);

        } catch (IOException e) {
            var err = new ErrorFila(1, "CSV_ILEGIBLE");
            return new ResumenCarga(0, 0, 1, List.of(err), Map.of("CSV_ILEGIBLE", 1));
        }
    }
}