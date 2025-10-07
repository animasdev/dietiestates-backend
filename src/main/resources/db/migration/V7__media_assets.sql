-- Flyway V7: media asset categories and media assets tables
-- Provides initial support for logos/foto profilo with local storage abstraction.

CREATE TABLE IF NOT EXISTS media_asset_categories (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code text NOT NULL UNIQUE,
    description text NOT NULL
);

CREATE TABLE IF NOT EXISTS media_assets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id uuid NOT NULL REFERENCES media_asset_categories(id) ON DELETE RESTRICT,
    storage_path text NOT NULL,
    public_url text NOT NULL,
    mime_type text NOT NULL,
    width_px int NULL,
    height_px int NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_media_assets_category ON media_assets (category_id);

INSERT INTO media_asset_categories (code, description) VALUES
    ('AGENCY_LOGO', 'Logo agenzia immobiliare'),
    ('AGENT_AVATAR', 'Foto profilo agente immobiliare')
ON CONFLICT (code) DO NOTHING;
