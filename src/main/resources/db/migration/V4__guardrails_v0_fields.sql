-- Story 2.4: Guardrails Management - Add v0 guardrail fields
-- v0 guardrails use price floor/ceiling and max change percent instead of
-- the more complex margin-based constraints

-- Add v0 guardrail fields to experiment_guardrails table
ALTER TABLE experiment_guardrails
    ADD COLUMN price_floor DECIMAL(12,2);

ALTER TABLE experiment_guardrails
    ADD COLUMN price_ceiling DECIMAL(12,2);

ALTER TABLE experiment_guardrails
    ADD COLUMN max_change_percent DECIMAL(5,2);

-- Note: Existing fields (max_discount_pct, max_markup_pct, min_margin_pct, etc.)
-- are kept for backwards compatibility and potential future use.
-- v0 only uses price_floor, price_ceiling, and max_change_percent.
