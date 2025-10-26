-- Flyway V11: add additional listing conditions fields

ALTER TABLE listings
    ADD COLUMN IF NOT EXISTS contract_description text NULL,
    ADD COLUMN IF NOT EXISTS security_deposit_cents bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS furnished boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS condo_fee_cents bigint NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pets_allowed boolean NOT NULL DEFAULT false;
