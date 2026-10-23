CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    preferred_language VARCHAR(10) DEFAULT 'en',
    account_status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_type VARCHAR(20) DEFAULT 'SAVINGS',
    balance NUMERIC(15, 2) NOT NULL DEFAULT 500000.00,
    currency VARCHAR(5) DEFAULT 'INR',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    transaction_type VARCHAR(20) NOT NULL,
    amount NUMERIC(15, 2) NOT NULL,
    tax_amount NUMERIC(15, 2) DEFAULT 0.00,
    counterparty_username VARCHAR(50),
    description TEXT,
    status VARCHAR(20) DEFAULT 'COMPLETED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    principal NUMERIC(15, 2) NOT NULL,
    accrued_interest NUMERIC(15, 2) DEFAULT 0.00,
    interest_rate NUMERIC(5, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    issued_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS market_assets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol VARCHAR(30) UNIQUE NOT NULL,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(30) NOT NULL,
    price NUMERIC(15, 2) NOT NULL,
    volatility NUMERIC(5, 4) NOT NULL,
    trading_schedule VARCHAR(20) DEFAULT '24_7',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS holdings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES market_assets(id) ON DELETE CASCADE,
    quantity NUMERIC(18, 6) NOT NULL DEFAULT 0.000000,
    average_purchase_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_asset UNIQUE (user_id, asset_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS government_ledger (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    transaction_id UUID NOT NULL,
    tax_amount NUMERIC(15, 2) NOT NULL,
    source_username VARCHAR(50) NOT NULL,
    recipient_username VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- SEED ALL 30 STOCKS, 20 COMMODITIES, 5 ETFS, REAL ESTATE & CRYPTO
INSERT INTO market_assets (symbol, name, category, price, volatility, trading_schedule) VALUES
-- 30 STOCKS
('AAPL', 'Apple Inc.', 'STOCKS', 19450.00, 0.0120, '24_7'),
('MSFT', 'Microsoft Corporation', 'STOCKS', 36800.00, 0.0110, '24_7'),
('NVDA', 'NVIDIA Corporation', 'STOCKS', 11450.00, 0.0250, '24_7'),
('AMZN', 'Amazon.com Inc.', 'STOCKS', 16900.00, 0.0140, '24_7'),
('GOOGL', 'Alphabet Inc. Class A', 'STOCKS', 15100.00, 0.0130, '24_7'),
('META', 'Meta Platforms Inc.', 'STOCKS', 49800.00, 0.0190, '24_7'),
('TSLA', 'Tesla Inc.', 'STOCKS', 18600.00, 0.0280, '24_7'),
('BRK.B', 'Berkshire Hathaway Inc.', 'STOCKS', 39950.00, 0.0070, '24_7'),
('LLY', 'Eli Lilly and Company', 'STOCKS', 78900.00, 0.0130, '24_7'),
('JPM', 'JPMorgan Chase & Co.', 'STOCKS', 18750.00, 0.0090, '24_7'),
('V', 'Visa Inc.', 'STOCKS', 24200.00, 0.0080, '24_7'),
('MA', 'Mastercard Incorporated', 'STOCKS', 41350.00, 0.0080, '24_7'),
('XOM', 'Exxon Mobil Corporation', 'STOCKS', 10150.00, 0.0110, '24_7'),
('JNJ', 'Johnson & Johnson', 'STOCKS', 13450.00, 0.0060, '24_7'),
('PG', 'Procter & Gamble Co.', 'STOCKS', 14300.00, 0.0060, '24_7'),
('HD', 'Home Depot Inc.', 'STOCKS', 32600.00, 0.0100, '24_7'),
('CVX', 'Chevron Corporation', 'STOCKS', 13200.00, 0.0110, '24_7'),
('WMT', 'Walmart Inc.', 'STOCKS', 6850.00, 0.0070, '24_7'),
('COST', 'Costco Wholesale Corp.', 'STOCKS', 74800.00, 0.0090, '24_7'),
('NFLX', 'Netflix Inc.', 'STOCKS', 59200.00, 0.0180, '24_7'),
('ADBE', 'Adobe Inc.', 'STOCKS', 43500.00, 0.0150, '24_7'),
('CRM', 'Salesforce Inc.', 'STOCKS', 24900.00, 0.0140, '24_7'),
('AMD', 'Advanced Micro Devices', 'STOCKS', 13950.00, 0.0240, '24_7'),
('INTC', 'Intel Corporation', 'STOCKS', 1980.00, 0.0220, '24_7'),
('QCOM', 'Qualcomm Inc.', 'STOCKS', 14650.00, 0.0160, '24_7'),
('HDFCBANK', 'HDFC Bank Limited', 'STOCKS', 1685.00, 0.0080, '24_7'),
('RELIANCE', 'Reliance Industries Ltd.', 'STOCKS', 2935.00, 0.0090, '24_7'),
('ICICIBANK', 'ICICI Bank Limited', 'STOCKS', 1255.00, 0.0090, '24_7'),
('TCS', 'Tata Consultancy Services', 'STOCKS', 4165.00, 0.0070, '24_7'),
('INFY', 'Infosys Limited', 'STOCKS', 1895.00, 0.0110, '24_7'),

-- 20 COMMODITIES
('GOLD', 'Gold (10 Grams Bullion)', 'COMMODITIES', 78500.00, 0.0040, '24_7'),
('SILVER', 'Silver (1 Kilogram Bar)', 'COMMODITIES', 94200.00, 0.0090, '24_7'),
('PLATINUM', 'Platinum (10 Grams)', 'COMMODITIES', 28400.00, 0.0070, '24_7'),
('PALLADIUM', 'Palladium (10 Grams)', 'COMMODITIES', 29800.00, 0.0120, '24_7'),
('CRUDE_WTI', 'Crude Oil (WTI Barrel)', 'COMMODITIES', 6250.00, 0.0180, '24_7'),
('BRENT_CRUDE', 'Brent Crude Oil (Barrel)', 'COMMODITIES', 6680.00, 0.0170, '24_7'),
('NAT_GAS', 'Natural Gas (MMBtu)', 'COMMODITIES', 225.00, 0.0280, '24_7'),
('HEATING_OIL', 'Heating Oil (Gallon)', 'COMMODITIES', 215.00, 0.0150, '24_7'),
('COPPER', 'Copper Grade A (1 kg)', 'COMMODITIES', 825.00, 0.0090, '24_7'),
('ALUMINUM', 'Aluminum (1 kg)', 'COMMODITIES', 235.00, 0.0080, '24_7'),
('ZINC', 'Zinc (1 kg)', 'COMMODITIES', 270.00, 0.0090, '24_7'),
('NICKEL', 'Nickel (1 kg)', 'COMMODITIES', 1420.00, 0.0150, '24_7'),
('LEAD', 'Lead (1 kg)', 'COMMODITIES', 185.00, 0.0070, '24_7'),
('IRON_ORE', 'Iron Ore (1 Metric Ton)', 'COMMODITIES', 8950.00, 0.0110, '24_7'),
('WHEAT', 'Milling Wheat (Quintal)', 'COMMODITIES', 2450.00, 0.0080, '24_7'),
('CORN', 'Yellow Corn (Quintal)', 'COMMODITIES', 2100.00, 0.0080, '24_7'),
('SOYBEANS', 'Soybeans (Quintal)', 'COMMODITIES', 4650.00, 0.0090, '24_7'),
('COFFEE', 'Arabica Coffee (100 kg)', 'COMMODITIES', 32000.00, 0.0120, '24_7'),
('SUGAR', 'Refined Sugar (Quintal)', 'COMMODITIES', 3850.00, 0.0060, '24_7'),
('COTTON', 'Raw Cotton (Bale)', 'COMMODITIES', 58000.00, 0.0100, '24_7'),

-- 5 ETFS
('VOO', 'Vanguard S&P 500 ETF', 'ETFS', 43800.00, 0.0050, '24_7'),
('QQQ', 'Invesco QQQ Trust (Nasdaq 100)', 'ETFS', 42100.00, 0.0080, '24_7'),
('NIFTYBEES', 'Nippon India Nifty 50 BeES', 'ETFS', 268.00, 0.0040, '24_7'),
('GLD', 'SPDR Gold Shares ETF', 'ETFS', 20500.00, 0.0030, '24_7'),
('VTI', 'Vanguard Total Stock Market ETF', 'ETFS', 22950.00, 0.0050, '24_7'),

-- FRACTIONAL REAL ESTATE
('DXB_PALM', 'Dubai Palm Luxury Villa (0.01% Share)', 'REALESTATE', 250000.00, 0.0020, '24_7'),
('NYC_PENT', 'Manhattan Penthouse (0.01% Share)', 'REALESTATE', 380000.00, 0.0020, '24_7'),
('BOM_SEAFACE', 'Bandra Sea-Facing Tower (0.01% Share)', 'REALESTATE', 450000.00, 0.0020, '24_7'),

-- CRYPTO
('BTC', 'Bitcoin Sovereign Virtual', 'CRYPTO', 5820000.00, 0.0250, '24_7'),
('ETH', 'Ethereum Virtual', 'CRYPTO', 292000.00, 0.0300, '24_7'),
('SOL', 'Solana Virtual', 'CRYPTO', 14800.00, 0.0350, '24_7')
ON CONFLICT (symbol) DO NOTHING;
