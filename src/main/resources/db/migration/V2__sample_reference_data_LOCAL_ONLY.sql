-- ============================================================================
-- Pricing Intelligence Lab - Sample Reference Data
-- Version: V2
-- ============================================================================
--
-- WARNING: LOCAL/DEMO USE ONLY
--
-- This migration inserts sample reference data for local development and demos.
-- It MUST NOT be executed in production environments.
--
-- For production deployments:
--   1. Delete or skip this migration file
--   2. Use proper data import processes from master data systems
--   3. Or configure Flyway to ignore this file via flyway.ignoreMigrationPatterns
--
-- ============================================================================

-- ============================================================================
-- SAMPLE STORES
-- ============================================================================

INSERT INTO stores (id, store_code, store_name, region, format, address, is_active, created_at, created_by) VALUES
    ('11111111-1111-1111-1111-111111111111', 'BKK001', 'Rama 9 Hypermarket', 'Bangkok', 'Hypermarket', '999 Rama 9 Road, Huaykwang, Bangkok 10310', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111112', 'BKK002', 'Silom Supermarket', 'Bangkok', 'Supermarket', '123 Silom Road, Bangrak, Bangkok 10500', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111113', 'BKK003', 'Chatuchak Express', 'Bangkok', 'Express', '456 Kamphaeng Phet Road, Chatuchak, Bangkok 10900', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111114', 'CNX001', 'Chiang Mai Central', 'North', 'Hypermarket', '789 Super Highway Road, Muang, Chiang Mai 50000', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111115', 'PKT001', 'Phuket Beach Mall', 'South', 'Supermarket', '321 Patong Beach Road, Kathu, Phuket 83150', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111116', 'KKN001', 'Khon Kaen Plaza', 'Northeast', 'Hypermarket', '654 Mittraphap Road, Muang, Khon Kaen 40000', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111117', 'BKK004', 'Bangna Mega Store', 'Bangkok', 'Hypermarket', '888 Bangna-Trad Road, Bangna, Bangkok 10260', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('11111111-1111-1111-1111-111111111118', 'BKK005', 'Thonglor Express', 'Bangkok', 'Express', '55 Sukhumvit 55, Watthana, Bangkok 10110', true, CURRENT_TIMESTAMP, 'SYSTEM');

-- ============================================================================
-- SAMPLE SKUS
-- ============================================================================

INSERT INTO skus (id, sku_code, sku_name, category, subcategory, brand, uom, is_active, created_at, created_by) VALUES
    -- Beverages
    ('22222222-2222-2222-2222-222222222221', '8851234560001', 'Coca-Cola Original 1.5L', 'Beverages', 'Carbonated Drinks', 'Coca-Cola', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222222', '8851234560002', 'Pepsi Max 1.5L', 'Beverages', 'Carbonated Drinks', 'Pepsi', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222223', '8851234560003', 'Singha Water 6L', 'Beverages', 'Water', 'Singha', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222224', '8851234560004', 'Lactasoy Soy Milk Original 1L', 'Beverages', 'Non-Dairy', 'Lactasoy', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222225', '8851234560005', 'Red Bull Energy Drink 250ml', 'Beverages', 'Energy Drinks', 'Red Bull', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),

    -- Snacks
    ('22222222-2222-2222-2222-222222222226', '8851234560006', 'Lay''s Classic Potato Chips 75g', 'Snacks', 'Chips', 'Lay''s', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222227', '8851234560007', 'Pringles Original 110g', 'Snacks', 'Chips', 'Pringles', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222228', '8851234560008', 'Oreo Original 137g', 'Snacks', 'Biscuits', 'Oreo', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),

    -- Dairy
    ('22222222-2222-2222-2222-222222222229', '8851234560009', 'Meiji Fresh Milk 2L', 'Dairy', 'Fresh Milk', 'Meiji', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222230', '8851234560010', 'Dutch Mill Yogurt Strawberry 135ml', 'Dairy', 'Yogurt', 'Dutch Mill', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),

    -- Instant Noodles
    ('22222222-2222-2222-2222-222222222231', '8851234560011', 'Mama Tom Yum Shrimp 55g', 'Instant Food', 'Instant Noodles', 'Mama', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM'),
    ('22222222-2222-2222-2222-222222222232', '8851234560012', 'Mama Pork Flavor 55g', 'Instant Food', 'Instant Noodles', 'Mama', 'EA', true, CURRENT_TIMESTAMP, 'SYSTEM');

-- ============================================================================
-- SAMPLE BASE PRICES
-- ============================================================================

-- Prices for BKK001 (Rama 9 Hypermarket)
INSERT INTO base_prices (id, sku_id, store_id, price, effective_date, created_at, created_by) VALUES
    ('33333333-3333-3333-3333-333333333301', '22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 45.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333302', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 42.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333303', '22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 35.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333304', '22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111111', 39.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333305', '22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111111', 25.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333306', '22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111111', 35.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333307', '22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111111', 6.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM');

-- Prices for BKK002 (Silom Supermarket)
INSERT INTO base_prices (id, sku_id, store_id, price, effective_date, created_at, created_by) VALUES
    ('33333333-3333-3333-3333-333333333311', '22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111112', 47.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333312', '22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112', 44.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('33333333-3333-3333-3333-333333333313', '22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111112', 37.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM');

-- ============================================================================
-- SAMPLE SKU COSTS
-- ============================================================================

INSERT INTO sku_costs (id, sku_id, cost, effective_date, created_at, created_by) VALUES
    ('44444444-4444-4444-4444-444444444401', '22222222-2222-2222-2222-222222222221', 32.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444402', '22222222-2222-2222-2222-222222222222', 30.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444403', '22222222-2222-2222-2222-222222222223', 22.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444404', '22222222-2222-2222-2222-222222222224', 28.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444405', '22222222-2222-2222-2222-222222222225', 18.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444406', '22222222-2222-2222-2222-222222222226', 22.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444407', '22222222-2222-2222-2222-222222222227', 35.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444408', '22222222-2222-2222-2222-222222222228', 25.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444409', '22222222-2222-2222-2222-222222222229', 52.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444410', '22222222-2222-2222-2222-222222222230', 8.00, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444411', '22222222-2222-2222-2222-222222222231', 3.50, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM'),
    ('44444444-4444-4444-4444-444444444412', '22222222-2222-2222-2222-222222222232', 3.50, '2024-01-01', CURRENT_TIMESTAMP, 'SYSTEM');
