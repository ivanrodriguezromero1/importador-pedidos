CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS clientes (
  id             VARCHAR(64) PRIMARY KEY,
  activo         BOOLEAN     NOT NULL DEFAULT TRUE,
  creado_en      TIMESTAMP   NOT NULL DEFAULT now(),
  actualizado_en TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS zonas (
  id                       VARCHAR(64) PRIMARY KEY,
  soporte_refrigeracion    BOOLEAN     NOT NULL DEFAULT FALSE,
  creado_en                TIMESTAMP   NOT NULL DEFAULT now(),
  actualizado_en           TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS pedidos (
  id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
  numero_pedido           VARCHAR(64) NOT NULL,
  cliente_id              VARCHAR(64) NOT NULL,
  zona_id                 VARCHAR(64) NOT NULL,
  fecha_entrega           DATE        NOT NULL,
  estado                  VARCHAR(16) NOT NULL CHECK (estado IN ('PENDIENTE','CONFIRMADO','ENTREGADO')),
  requiere_refrigeracion  BOOLEAN     NOT NULL,
  creado_en               TIMESTAMP   NOT NULL DEFAULT now(),
  actualizado_en          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_pedidos_numero_pedido ON pedidos (numero_pedido);
CREATE INDEX IF NOT EXISTS idx_pedidos_estado_fecha ON pedidos (estado, fecha_entrega);
CREATE INDEX IF NOT EXISTS idx_pedidos_cliente_id  ON pedidos (cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_zona_id     ON pedidos (zona_id);

CREATE TABLE IF NOT EXISTS cargas_idempotencia (
  id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  clave_idempotencia VARCHAR(128) NOT NULL,
  archivo_hash       VARCHAR(128) NOT NULL,
  creado_en          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_cargas_clave_hash
  ON cargas_idempotencia (clave_idempotencia, archivo_hash);

CREATE OR REPLACE FUNCTION set_actualizado_en() RETURNS TRIGGER AS $$
BEGIN
  NEW.actualizado_en := now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_pedidos_touch') THEN
    CREATE TRIGGER trg_pedidos_touch
      BEFORE UPDATE ON pedidos
      FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_clientes_touch') THEN
    CREATE TRIGGER trg_clientes_touch
      BEFORE UPDATE ON clientes
      FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_zonas_touch') THEN
    CREATE TRIGGER trg_zonas_touch
      BEFORE UPDATE ON zonas
      FOR EACH ROW EXECUTE FUNCTION set_actualizado_en();
  END IF;
END $$;
