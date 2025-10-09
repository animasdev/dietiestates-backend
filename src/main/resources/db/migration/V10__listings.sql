-- Flyway V10: listings table

CREATE TABLE IF NOT EXISTS listings (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    agency_id uuid NOT NULL REFERENCES agencies(id),
    owner_agent_id uuid NOT NULL REFERENCES agents(id),
    listing_type_id uuid NOT NULL REFERENCES listing_types(id),
    status_id uuid NOT NULL REFERENCES listing_statuses(id),
    title text NOT NULL,
    description text NOT NULL,
    price_cents bigint NOT NULL CHECK (price_cents >= 0),
    currency TEXT NOT NULL DEFAULT 'EUR',
    size_sqm numeric(10,2) NULL,
    rooms int NULL,
    floor int NULL,
    energy_class text NULL,
    address_line text NOT NULL,
    city text NOT NULL,
    postal_code text NULL,
    geo geography(Point,4326) NOT NULL,
    pending_delete_until timestamptz NULL,
    deleted_at timestamptz NULL,
    published_at timestamptz NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listings_geo ON listings USING GIST (geo);
CREATE INDEX IF NOT EXISTS idx_listings_city ON listings (city);
CREATE INDEX IF NOT EXISTS idx_listings_price ON listings (price_cents);
CREATE INDEX IF NOT EXISTS idx_listings_type ON listings (listing_type_id);
CREATE INDEX IF NOT EXISTS idx_listings_status ON listings (status_id);
CREATE INDEX IF NOT EXISTS idx_listings_status_pending ON listings (status_id, pending_delete_until);
CREATE INDEX IF NOT EXISTS idx_listings_agency_owner ON listings (agency_id, owner_agent_id);
