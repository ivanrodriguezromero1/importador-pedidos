# Importador de pedidos

Servicio en **Spring Boot 3** (Java 17) que carga pedidos desde un **CSV**, valida reglas de negocio y persiste en **PostgreSQL** usando inserciones por lote. Incluye **idempotencia** por corrida, protección vía **JWT**, **OpenAPI** y logs **JSON** con `X-Correlation-Id`.

## Cómo ejecutar

1. **Base de datos (PostgreSQL)**  
   ```bash
   docker compose up -d
   ```

2. **Aplicación**  
   ```bash
   mvn clean spring-boot:run
   ```
   o bien:
   ```bash
   mvn clean package
   java -jar target/importador-pedidos-0.0.1-SNAPSHOT.jar
   ```

> Si usas Windows con Docker en WSL y no conecta a `localhost`, usa la IP de Windows en la URL JDBC del `application.yml`.

## Arquitectura

Se aplicó un estilo **hexagonal** (ports & adapters). El **dominio** contiene modelos, puertos e invariantes. La capa de **aplicación** orquesta el caso de uso de importación. Los **adaptadores** exponen el endpoint HTTP y realizan la persistencia con JPA. La **configuración** agrupa seguridad (resource server JWT), OpenAPI y logging.

- `domain`: entidades/VOs (`Pedido`, `Estado`), puertos y servicios de dominio (`ValidacionPedidoService`, `CalculoPrioridadService`).
- `application`: caso de uso `CargarPedidosHandler` (orquestación, batch, idempotencia).
- `adapters`: entrada REST (controller) y salida JPA (repositorio JDBC/JPA).
- `config`: beans de seguridad, OpenAPI y logback.
- `shared`: utilidades (parser CSV, hashes, etc.).

**Dependencias dirigidas hacia el dominio**; el dominio no depende de frameworks.

## Configuración

`src/main/resources/application.yml` (resumen):
- `spring.datasource.url=jdbc:postgresql://localhost:5432/pedidos`
- `spring.datasource.username=postgres`
- `spring.datasource.password=postgres`
- `spring.jpa.open-in-view=false`
- Flyway habilitado (`db/migration`)
- `app.batch.size=500` (rango esperado 500–1000)
- `app.security.hmac-secret=<secreto-HS256>`
- Logback con encoder JSON y propagación de `X-Correlation-Id`

## API

- **Swagger UI**: `http://localhost:8080/swagger-ui`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Endpoint principal

**POST** `/pedidos/cargar`  (multipart/form-data)
- **Headers**:  
  - `Authorization: Bearer <jwt>`  
  - `Idempotency-Key: <uuid>`  
  - `X-Correlation-Id: <opcional>`
- **Partes**:  
  - `file`: CSV UTF-8

**Respuesta (resumen):**
```json
{
  "totalProcesados": 1000,
  "guardados": 1000,
  "conError": 0,
  "erroresPorFila": [],
  "erroresAgrupados": {}
}
```

**Modelo de error estándar:**
```json
{
  "code": "UNAUTHORIZED",
  "message": "JWT requerido o inválido",
  "details": [],
  "correlationId": "pedido-123"
}
```

## Formato CSV

- Codificación: UTF-8, delimitador `,`
- Cabecera:  
  `numeroPedido,clienteId,fechaEntrega,estado,zonaEntrega,requiereRefrigeracion`
- Valores válidos:  
  - `estado`: `PENDIENTE|CONFIRMADO|ENTREGADO`  
  - `requiereRefrigeracion`: `true|false`

## Validaciones de negocio

Por fila:
- `numeroPedido`: alfanumérico y **único**.
- `clienteId`: debe existir en `clientes`.
- `fechaEntrega`: **no pasada** (zona `America/Lima`).
- `zonaEntrega`: debe existir en `zonas`.
- Si `requiereRefrigeracion=true`, la zona debe soportarla.

Se guardan **solo** las filas válidas.

## Batch

Inserciones en lotes usando `app.batch.size` (por defecto 500). Volumen esperado por carga: **hasta 1000** filas. Se reducen lecturas repetidas de catálogos.

## Idempotencia

- Requiere header `Idempotency-Key`.
- Se calcula `SHA-256` del archivo.
- Tabla `cargas_idempotencia` mantiene `(idempotency_key, archivo_hash)`.
- Si ya existe la combinación, **no se repiten efectos** (respuesta neutra).

## Seguridad

Resource Server **JWT** (HS256 con secreto en configuración). Rutas públicas: **Swagger** y **OpenAPI**. Resto autenticado. Para pruebas locales, genera un JWT con el secreto configurado (`app.security.hmac-secret`).

## Logs y correlación

Logs en **JSON**. Si llega `X-Correlation-Id`, se devuelve en la respuesta y se añade al **MDC** para que aparezca en todas las líneas de log.

## Datos y migraciones (Flyway)

Se crean las tablas: `clientes`, `zonas`, `pedidos`, `cargas_idempotencia`.  
Índices en `numero_pedido` (UK) y `(estado, fecha_entrega)`.

## Tests y cobertura

Pruebas unitarias en dominio (JUnit 5, AssertJ). **JaCoCo** verifica cobertura por paquete (umbral configurable en `pom.xml`).

```bash
mvn clean verify
# Reporte: target/site/jacoco/index.html
```

## Supuestos y límites

- Catálogos (`clientes`, `zonas`) precargados (o por seed).
- JWT simplificado para entorno local (HS256).
- Sin reintentos automáticos ante fallos transitorios de BD.

## Comandos útiles

Semillas rápidas (WSL):
```bash
docker exec -it pg_importador_pedidos psql -U postgres -d pedidos -c "insert into clientes(id, activo) values ('CLI-123', true), ('CLI-999', true) on conflict do nothing;"
docker exec -it pg_importador_pedidos psql -U postgres -d pedidos -c "insert into zonas(id, soporte_refrigeracion) values ('ZONA1', true), ('ZONA5', false) on conflict do nothing;"
```

Ejemplo de carga (PowerShell, una línea):
```powershell
curl.exe -i -X POST "http://localhost:8080/pedidos/cargar" -H "Authorization: Bearer <TU_JWT>" -H "X-Correlation-Id: pedido-123" -H "Idempotency-Key: 55555555-5555-5555-5555-555555555555" -F "file=@"D:utalepo\samples\pedidos_1000.csv";type=text/csv"
```
