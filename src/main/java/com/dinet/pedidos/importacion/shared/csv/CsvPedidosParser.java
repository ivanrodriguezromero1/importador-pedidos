package com.dinet.pedidos.importacion.shared.csv;

import com.dinet.pedidos.importacion.application.dto.ErrorFila;
import com.dinet.pedidos.importacion.domain.model.Estado;
import com.dinet.pedidos.importacion.domain.model.Pedido;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class CsvPedidosParser {

    private static final String[] HEADER = {
            "numero_pedido","cliente_id","fecha_entrega","estado","zona_id","requiere_refrigeracion"
    };

    public static Result parse(byte[] csvBytes) throws IOException {
        try (var reader = new InputStreamReader(new ByteArrayInputStream(csvBytes), StandardCharsets.UTF_8);
             var parser = CSVParser.parse(reader, CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .setIgnoreEmptyLines(true)
                     .build())) {

            var headerMap = parser.getHeaderMap();
            if (headerMap == null || headerMap.size() != HEADER.length) {
                return Result.cabeceraInvalida("CABECERA_INVALIDA");
            }
            for (String h : HEADER) {
                if (!headerMap.containsKey(h)) {
                    return Result.cabeceraInvalida("CABECERA_INVALIDA: falta '" + h + "'");
                }
            }

            List<FilaValida> filas = new ArrayList<>();
            List<ErrorFila> errores = new ArrayList<>();

            for (CSVRecord r : parser) {
                int linea = (int) r.getRecordNumber() + 1; // cabecera = línea 1

                try {
                    var numero   = obligatorio(r, "numero_pedido");
                    var cliente  = obligatorio(r, "cliente_id");
                    var fechaStr = obligatorio(r, "fecha_entrega");
                    var estadoStr= obligatorio(r, "estado");
                    var zona     = obligatorio(r, "zona_id");
                    var reqRefriStr = obligatorio(r, "requiere_refrigeracion");

                    LocalDate fecha;
                    try { fecha = LocalDate.parse(fechaStr); }
                    catch (DateTimeParseException ex) { errores.add(new ErrorFila(linea, "FECHA_INVALIDA_FORMATO")); continue; }

                    Estado estado;
                    try { estado = Estado.valueOf(estadoStr); }
                    catch (IllegalArgumentException ex) { errores.add(new ErrorFila(linea, "ESTADO_INVALIDO")); continue; }

                    boolean requiereRefri;
                    if ("true".equalsIgnoreCase(reqRefriStr) || "false".equalsIgnoreCase(reqRefriStr)) {
                        requiereRefri = Boolean.parseBoolean(reqRefriStr);
                    } else {
                        errores.add(new ErrorFila(linea, "BOOLEANO_INVALIDO_requiere_refrigeracion"));
                        continue;
                    }

                    if (!numero.matches("^[A-Za-z0-9-]+$")) {
                        errores.add(new ErrorFila(linea, "NUMERO_PEDIDO_INVALIDO"));
                        continue;
                    }

                    filas.add(new FilaValida(linea, new Pedido(numero, cliente, fecha, estado, zona, requiereRefri)));

                } catch (CampoObligatorio e) {
                    errores.add(new ErrorFila(linea, "CAMPO_OBLIGATORIO_FALTA_" + e.nombre()));
                } catch (Exception e) {
                    errores.add(new ErrorFila(linea, "ERROR_DESCONOCIDO"));
                }
            }
            return new Result(filas, errores, null);
        }
    }

    private static String obligatorio(CSVRecord r, String nombre) {
        var v = r.get(nombre);
        if (v == null) throw new CampoObligatorio(nombre);
        v = v.trim();
        if (v.isEmpty()) throw new CampoObligatorio(nombre);
        return v;
    }

    private static final class CampoObligatorio extends RuntimeException {
        private final String nombre;
        CampoObligatorio(String nombre) { super(nombre); this.nombre = nombre; }
        public String nombre() { return nombre; }
    }

    public record FilaValida(int linea, Pedido pedido) {}
    public record Result(List<FilaValida> filasValidas, List<ErrorFila> errores, String cabeceraError) {
        static Result cabeceraInvalida(String motivo) {
            return new Result(List.of(), List.of(new ErrorFila(1, motivo)), motivo);
        }
    }
}