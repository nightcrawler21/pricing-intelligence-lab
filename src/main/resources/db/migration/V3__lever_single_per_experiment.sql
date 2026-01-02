-- Story 2.3: Lever Management - Single lever per experiment constraint
-- This migration enforces the v0 simplification: exactly one lever per experiment

-- Add unique constraint on experiment_id to enforce single lever per experiment
-- First, delete any duplicate levers (keep the first one by ID) if they exist
DELETE FROM experiment_levers
WHERE id NOT IN (
    SELECT MIN(id) FROM experiment_levers GROUP BY experiment_id
);

-- Add unique constraint (v0: only one lever allowed per experiment)
ALTER TABLE experiment_levers
    ADD CONSTRAINT uq_lever_experiment UNIQUE (experiment_id);

-- Drop the old CHECK constraint and add updated one with PRICE_DISCOUNT
ALTER TABLE experiment_levers
    DROP CONSTRAINT IF EXISTS chk_lever_type;

ALTER TABLE experiment_levers
    ADD CONSTRAINT chk_lever_type CHECK (
        lever_type IN ('PRICE_DISCOUNT', 'PERCENTAGE_CHANGE', 'ABSOLUTE_CHANGE', 'TARGET_PRICE', 'COMPETITOR_MATCH')
    );
