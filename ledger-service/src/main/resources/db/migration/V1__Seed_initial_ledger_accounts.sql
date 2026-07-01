-- Initial Ledger Accounts Seed Data
-- Hybrid approach: JPA manages schema, Flyway seeds data

INSERT INTO ledger_accounts (
    id,
    owner_name,
    bank_account_no,
    ledger_balance,
    daily_limit,
    daily_used,
    last_used_date,
    kyc_tier,
    created_at,
    updated_at
) VALUES 
-- Account 1: Basic Tier 1 user
(
    'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    'user_01',
    '1000000001',
    50000.00,
    50000.00,
    0.00,
    NULL,
    'TIER_1',
    NOW(),
    NOW()
),
-- Account 2: Tier 1 with moderate balance
(
    'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    'user_02',
    '1000000002',
    150000.00,
    50000.00,
    0.00,
    NULL,
    'TIER_1',
    NOW(),
    NOW()
),
-- Account 3: Tier 2 business user
(
    'c3d4e5f6-a7b8-9012-cdef-123456789012',
    'user_03',
    '1000000003',
    500000.00,
    500000.00,
    0.00,
    NULL,
    'TIER_2',
    NOW(),
    NOW()
),
-- Account 4: Tier 2 high-value customer
(
    'd4e5f6a7-b8c9-0123-defa-234567890123',
    'user_04',
    '1000000004',
    2500000.00,
    500000.00,
    0.00,
    NULL,
    'TIER_2',
    NOW(),
    NOW()
),
-- Account 5: Premium Tier 3 enterprise
(
    'e5f6a7b8-c9d0-1234-efab-345678901234',
    'user_05',
    '1000000005',
    10000000.00,
    5000000.00,
    0.00,
    NULL,
    'TIER_3',
    NOW(),
    NOW()
);
