-- Flyway V11: listing_media table
-- provides a join table between listings and media_assets tables introducing new media_asset_category

INSERT INTO media_asset_categories (code, description) VALUES
    ('LISTING_PHOTO', 'Foto relativa ad un annuncio')
ON CONFLICT (code) DO NOTHING;

CREATE TABLE IF NOT EXISTS listing_media (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    media_id uuid NOT NULL REFERENCES media_assets(id) ON DELETE RESTRICT,
    listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    sort_order int NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_listing_media_sort UNIQUE (listing_id, sort_order),
    CONSTRAINT uq_listing_media_media UNIQUE (listing_id, media_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_media_media_id ON listing_media (media_id);
