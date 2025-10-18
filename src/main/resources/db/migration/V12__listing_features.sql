-- Flyway V12: listing_features table
-- provides a join table between listings and features

CREATE TABLE IF NOT EXISTS listing_features (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    feature_id uuid NOT NULL REFERENCES features(id) ON DELETE RESTRICT,
    listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    price_cents bigint DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_listing_features_feature UNIQUE (listing_id, feature_id)
);

CREATE INDEX IF NOT EXISTS idx_listing_features_feature_id ON listing_features (feature_id);
