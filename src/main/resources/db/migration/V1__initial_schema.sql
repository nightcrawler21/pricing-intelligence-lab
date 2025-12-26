-- ============================================================================
-- Pricing Intelligence Lab - Initial Schema
-- Version: V1
-- Description: Creates all initial tables for the pricing experiment system
-- ============================================================================

-- ============================================================================
-- REFERENCE DATA TABLES
-- ============================================================================

-- Stores table - represents physical retail locations
CREATE TABLE stores (
    id              UUID PRIMARY KEY,
    store_code      VARCHAR(50) NOT NULL UNIQUE,
    store_name      VARCHAR(200) NOT NULL,
    region          VARCHAR(100),
    format          VARCHAR(50),
    address         VARCHAR(500),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_store_code ON stores(store_code);
CREATE INDEX idx_store_region ON stores(region);
CREATE INDEX idx_store_active ON stores(is_active);

-- SKUs table - represents products
CREATE TABLE skus (
    id              UUID PRIMARY KEY,
    sku_code        VARCHAR(50) NOT NULL UNIQUE,
    sku_name        VARCHAR(300) NOT NULL,
    category        VARCHAR(100),
    subcategory     VARCHAR(100),
    brand           VARCHAR(100),
    uom             VARCHAR(20),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_sku_code ON skus(sku_code);
CREATE INDEX idx_sku_category ON skus(category);
CREATE INDEX idx_sku_active ON skus(is_active);

-- Base prices table - regular prices for SKUs at stores
CREATE TABLE base_prices (
    id              UUID PRIMARY KEY,
    sku_id          UUID NOT NULL REFERENCES skus(id),
    store_id        UUID NOT NULL REFERENCES stores(id),
    price           DECIMAL(12,2) NOT NULL,
    effective_date  DATE NOT NULL,
    end_date        DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_base_price_sku ON base_prices(sku_id);
CREATE INDEX idx_base_price_store ON base_prices(store_id);
CREATE INDEX idx_base_price_effective ON base_prices(effective_date);

-- SKU costs table - cost data for margin calculations
CREATE TABLE sku_costs (
    id              UUID PRIMARY KEY,
    sku_id          UUID NOT NULL REFERENCES skus(id),
    cost            DECIMAL(12,2) NOT NULL,
    effective_date  DATE NOT NULL,
    end_date        DATE,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100)
);

CREATE INDEX idx_sku_cost_sku ON sku_costs(sku_id);
CREATE INDEX idx_sku_cost_effective ON sku_costs(effective_date);

-- ============================================================================
-- EXPERIMENT TABLES
-- ============================================================================

-- Experiments table - main experiment definition
CREATE TABLE experiments (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(200) NOT NULL,
    description             VARCHAR(2000),
    status                  VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    start_date              DATE NOT NULL,
    end_date                DATE NOT NULL,
    business_justification  VARCHAR(2000),
    hypothesis              VARCHAR(1000),
    approved_by             VARCHAR(100),
    rejection_reason        VARCHAR(1000),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100),
    CONSTRAINT chk_experiment_status CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'RUNNING', 'COMPLETED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_experiment_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_experiment_status ON experiments(status);
CREATE INDEX idx_experiment_start_date ON experiments(start_date);
CREATE INDEX idx_experiment_created_by ON experiments(created_by);

-- Experiment scopes table - defines which store-SKU combinations are in the experiment
CREATE TABLE experiment_scopes (
    id              UUID PRIMARY KEY,
    experiment_id   UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    store_id        UUID NOT NULL REFERENCES stores(id),
    sku_id          UUID NOT NULL REFERENCES skus(id),
    is_test_group   BOOLEAN NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100),
    CONSTRAINT uq_scope_experiment_store_sku UNIQUE (experiment_id, store_id, sku_id)
);

CREATE INDEX idx_scope_experiment ON experiment_scopes(experiment_id);
CREATE INDEX idx_scope_store ON experiment_scopes(store_id);
CREATE INDEX idx_scope_sku ON experiment_scopes(sku_id);

-- Experiment levers table - defines pricing changes to apply
CREATE TABLE experiment_levers (
    id              UUID PRIMARY KEY,
    experiment_id   UUID NOT NULL REFERENCES experiments(id) ON DELETE CASCADE,
    sku_id          UUID NOT NULL REFERENCES skus(id),
    lever_type      VARCHAR(30) NOT NULL,
    lever_value     DECIMAL(12,4) NOT NULL,
    description     VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(100),
    updated_at      TIMESTAMP,
    updated_by      VARCHAR(100),
    CONSTRAINT chk_lever_type CHECK (lever_type IN ('PERCENTAGE_CHANGE', 'ABSOLUTE_CHANGE', 'TARGET_PRICE', 'COMPETITOR_MATCH'))
);

CREATE INDEX idx_lever_experiment ON experiment_levers(experiment_id);
CREATE INDEX idx_lever_sku ON experiment_levers(sku_id);

-- Experiment guardrails table - defines constraints for the experiment
CREATE TABLE experiment_guardrails (
    experiment_id           UUID PRIMARY KEY REFERENCES experiments(id) ON DELETE CASCADE,
    max_discount_pct        DECIMAL(5,2),
    max_markup_pct          DECIMAL(5,2),
    min_margin_pct          DECIMAL(5,2),
    max_revenue_impact_pct  DECIMAL(5,2),
    prevent_below_cost      BOOLEAN NOT NULL DEFAULT TRUE,
    enforce_price_points    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(100),
    updated_at              TIMESTAMP,
    updated_by              VARCHAR(100)
);

-- ============================================================================
-- SIMULATION TABLES
-- ============================================================================

-- Simulation runs table - tracks simulation executions
CREATE TABLE simulation_runs (
    id                          UUID PRIMARY KEY,
    experiment_id               UUID NOT NULL REFERENCES experiments(id),
    status                      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at                  TIMESTAMP,
    completed_at                TIMESTAMP,
    error_message               VARCHAR(2000),
    projected_revenue_test      DECIMAL(15,2),
    projected_revenue_control   DECIMAL(15,2),
    projected_revenue_lift_pct  DECIMAL(8,4),
    projected_units_test        DECIMAL(15,2),
    projected_units_control     DECIMAL(15,2),
    projected_margin_test       DECIMAL(15,2),
    projected_margin_control    DECIMAL(15,2),
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by                  VARCHAR(100),
    updated_at                  TIMESTAMP,
    updated_by                  VARCHAR(100),
    CONSTRAINT chk_sim_status CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_sim_run_experiment ON simulation_runs(experiment_id);
CREATE INDEX idx_sim_run_status ON simulation_runs(status);
CREATE INDEX idx_sim_run_started ON simulation_runs(started_at);

-- Simulation results daily table - granular simulation results
CREATE TABLE simulation_results_daily (
    id                  UUID PRIMARY KEY,
    simulation_run_id   UUID NOT NULL REFERENCES simulation_runs(id) ON DELETE CASCADE,
    simulation_date     DATE NOT NULL,
    store_id            UUID NOT NULL REFERENCES stores(id),
    sku_id              UUID NOT NULL REFERENCES skus(id),
    is_test_group       BOOLEAN NOT NULL,
    base_price          DECIMAL(12,2) NOT NULL,
    simulated_price     DECIMAL(12,2) NOT NULL,
    projected_units     DECIMAL(12,2) NOT NULL,
    projected_revenue   DECIMAL(15,2) NOT NULL,
    projected_margin    DECIMAL(15,2) NOT NULL,
    baseline_units      DECIMAL(12,2),
    baseline_revenue    DECIMAL(15,2)
);

CREATE INDEX idx_sim_result_run ON simulation_results_daily(simulation_run_id);
CREATE INDEX idx_sim_result_date ON simulation_results_daily(simulation_date);
CREATE INDEX idx_sim_result_store ON simulation_results_daily(store_id);
CREATE INDEX idx_sim_result_sku ON simulation_results_daily(sku_id);

-- ============================================================================
-- AUDIT TABLES
-- ============================================================================

-- Audit logs table - immutable audit trail
CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       UUID NOT NULL,
    action          VARCHAR(50) NOT NULL,
    performed_by    VARCHAR(100) NOT NULL,
    performed_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    old_value       TEXT,
    new_value       TEXT,
    notes           VARCHAR(1000),
    ip_address      VARCHAR(45)
);

CREATE INDEX idx_audit_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_entity_id ON audit_logs(entity_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_user ON audit_logs(performed_by);
CREATE INDEX idx_audit_timestamp ON audit_logs(performed_at);

-- ============================================================================
-- COMMENTS
-- ============================================================================

COMMENT ON TABLE stores IS 'Physical retail store locations for pricing experiments';
COMMENT ON TABLE skus IS 'Stock keeping units (products) that can have price experiments';
COMMENT ON TABLE base_prices IS 'Regular selling prices for SKUs at specific stores';
COMMENT ON TABLE sku_costs IS 'Cost of goods sold for margin calculations';
COMMENT ON TABLE experiments IS 'Pricing experiments with defined scope, levers, and guardrails';
COMMENT ON TABLE experiment_scopes IS 'Store-SKU combinations included in an experiment';
COMMENT ON TABLE experiment_levers IS 'Pricing changes to apply in an experiment';
COMMENT ON TABLE experiment_guardrails IS 'Constraints preventing harmful pricing actions';
COMMENT ON TABLE simulation_runs IS 'Execution records for experiment simulations';
COMMENT ON TABLE simulation_results_daily IS 'Daily granularity projected outcomes';
COMMENT ON TABLE audit_logs IS 'Immutable audit trail for all significant actions';
