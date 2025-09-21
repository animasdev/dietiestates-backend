-- Flyway V3: users schema (aligned to docs/persistenza_dati.md)
-- Pre-req: roles table exists (see V2__lookups.sql)

-- Enable citext for case-insensitive email uniqueness
CREATE EXTENSION IF NOT EXISTS citext;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name text NOT NULL,
    email citext NOT NULL UNIQUE,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    password_hash text NULL,
    password_algo text NULL,
    is_first_access boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);
