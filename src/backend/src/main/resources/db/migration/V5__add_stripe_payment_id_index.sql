CREATE INDEX IF NOT EXISTS idx_users_stripe_payment_id ON users (stripe_payment_id) WHERE stripe_payment_id IS NOT NULL;
