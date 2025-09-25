CREATE TABLE IF NOT EXISTS signup_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email CITEXT NOT NULL,
    display_name TEXT NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Optional index to speed lookups by email for active tokens
-- Partial index cannot reference non-immutable functions (now() is STABLE),
-- so we only filter by consumed_at and include expires_at in the key for range filtering.
CREATE INDEX IF NOT EXISTS idx_signup_tokens_active_email ON signup_tokens (email, expires_at)
    WHERE consumed_at IS NULL;

-- Ensure only one active token per email (enforced via application logic; DB partial unique is tricky)
