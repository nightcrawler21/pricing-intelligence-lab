-- ============================================================================
-- Pricing Intelligence Lab - Simulation Runner v0 Fields
-- Version: V5
-- Description: Adds fields required for v0 simulation runner
-- ============================================================================

-- Add total_days_simulated to track simulation duration
ALTER TABLE simulation_runs ADD COLUMN IF NOT EXISTS total_days_simulated INTEGER;

-- Add projected_cost to daily results for margin calculation
ALTER TABLE simulation_results_daily ADD COLUMN IF NOT EXISTS projected_cost DECIMAL(15,2);

-- Add unit_cost to daily results for audit trail
ALTER TABLE simulation_results_daily ADD COLUMN IF NOT EXISTS unit_cost DECIMAL(12,2);
