-- SME Rau củ P0: stock_batch, stock_shrinkage, extend stock_alert
-- Idempotent — safe to re-run

CREATE TABLE IF NOT EXISTS stock_batch (
    id                  VARCHAR(36)  PRIMARY KEY,
    batch_code          VARCHAR(100) NOT NULL,
    product_id          VARCHAR(36)  NOT NULL,
    warehouse_id        VARCHAR(36)  NOT NULL,
    supplier_id         VARCHAR(36),
    grn_id              VARCHAR(36),
    grn_item_id         VARCHAR(36),
    warehouse_location_id VARCHAR(36),
    received_date       DATE,
    expiry_date         DATE,
    qty_on_hand         DOUBLE PRECISION DEFAULT 0,
    status              VARCHAR(20)  DEFAULT 'ACTIVE',
    is_deleted          BOOLEAN      DEFAULT false,
    created_date        TIMESTAMP,
    created_by          VARCHAR(50),
    updated_date        TIMESTAMP,
    updated_by          VARCHAR(50),
    deleted_at          TIMESTAMP,
    deleted_by          VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_batch_code
    ON stock_batch (batch_code) WHERE COALESCE(is_deleted, false) = false;

CREATE INDEX IF NOT EXISTS idx_stock_batch_fefo
    ON stock_batch (product_id, warehouse_id, expiry_date)
    WHERE COALESCE(is_deleted, false) = false AND qty_on_hand > 0;

CREATE TABLE IF NOT EXISTS stock_shrinkage (
    id              VARCHAR(36) PRIMARY KEY,
    shrinkage_code  VARCHAR(50) NOT NULL,
    warehouse_id    VARCHAR(36) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    note            TEXT,
    confirmed_at    TIMESTAMP,
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_shrinkage_code
    ON stock_shrinkage (shrinkage_code) WHERE COALESCE(is_deleted, false) = false;

CREATE TABLE IF NOT EXISTS stock_shrinkage_line (
    id              VARCHAR(36) PRIMARY KEY,
    shrinkage_id    VARCHAR(36) NOT NULL,
    batch_id        VARCHAR(36) NOT NULL,
    product_id      VARCHAR(36) NOT NULL,
    reason          VARCHAR(20) NOT NULL,
    qty             DOUBLE PRECISION NOT NULL,
    note            TEXT,
    is_deleted      BOOLEAN DEFAULT false,
    created_date    TIMESTAMP,
    created_by      VARCHAR(50),
    updated_date    TIMESTAMP,
    updated_by      VARCHAR(50),
    deleted_at      TIMESTAMP,
    deleted_by      VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_shrinkage_line_shrinkage
    ON stock_shrinkage_line (shrinkage_id) WHERE COALESCE(is_deleted, false) = false;

ALTER TABLE stock_alert ADD COLUMN IF NOT EXISTS alert_type VARCHAR(30) DEFAULT 'LOW_STOCK';
ALTER TABLE stock_alert ADD COLUMN IF NOT EXISTS batch_id VARCHAR(36);
ALTER TABLE stock_alert ADD COLUMN IF NOT EXISTS expiry_date DATE;
ALTER TABLE stock_alert ADD COLUMN IF NOT EXISTS days_to_expiry INTEGER;

CREATE INDEX IF NOT EXISTS idx_stock_alert_type_status
    ON stock_alert (alert_type, status) WHERE COALESCE(is_deleted, false) = false;

UPDATE stock_alert SET alert_type = 'LOW_STOCK' WHERE alert_type IS NULL;
