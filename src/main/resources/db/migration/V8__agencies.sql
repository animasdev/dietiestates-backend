-- Flyway V8: agencies table

CREATE TABLE IF NOT EXISTS agencies (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    description text NOT NULL,
    user_id uuid NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    logo_media_id uuid NULL REFERENCES media_assets(id) ON DELETE SET NULL,
    approved_by uuid NULL REFERENCES users(id) ON DELETE SET NULL,
    approved_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_agencies_user_id ON agencies (user_id);
