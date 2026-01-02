-- ============================================================================
-- Pricing Intelligence Lab - Baseline Daily Sales Table
-- Version: V6
-- ============================================================================
--
-- Adds baseline_daily_sales table to store historical sales data.
-- This data is used by the simulation engine to project how units sold
-- would change under different pricing scenarios.
--
-- ============================================================================

CREATE TABLE baseline_daily_sales (
    id UUID PRIMARY KEY,
    store_id UUID NOT NULL REFERENCES stores(id),
    sku_id UUID NOT NULL REFERENCES skus(id),
    sales_date DATE NOT NULL,
    units_sold DECIMAL(12, 2) NOT NULL,
    revenue DECIMAL(12, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_at TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT uk_baseline_store_sku_date UNIQUE (store_id, sku_id, sales_date)
);

-- Indexes for common query patterns
CREATE INDEX idx_baseline_store ON baseline_daily_sales(store_id);
CREATE INDEX idx_baseline_sku ON baseline_daily_sales(sku_id);
CREATE INDEX idx_baseline_date ON baseline_daily_sales(sales_date);
CREATE INDEX idx_baseline_store_sku ON baseline_daily_sales(store_id, sku_id);

COMMENT ON TABLE baseline_daily_sales IS 'Historical daily sales data for simulation baseline calculations';
COMMENT ON COLUMN baseline_daily_sales.units_sold IS 'Number of units sold on this date';
COMMENT ON COLUMN baseline_daily_sales.revenue IS 'Revenue generated (optional, can be derived from price x units)';
