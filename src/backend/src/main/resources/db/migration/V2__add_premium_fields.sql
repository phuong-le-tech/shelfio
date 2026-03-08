-- V2: Add Stripe payment fields for premium upgrade
ALTER TABLE users ADD COLUMN stripe_customer_id VARCHAR(255);
ALTER TABLE users ADD COLUMN stripe_payment_id VARCHAR(255);
