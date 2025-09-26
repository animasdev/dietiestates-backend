-- Reworked V5: add role_id FK instead of role_code text
-- 1) Add column (nullable first for safety)
ALTER TABLE signup_tokens
    ADD COLUMN IF NOT EXISTS role_id uuid;

-- 2) Backfill existing rows to USER role if any row exists without role_id
UPDATE signup_tokens st
SET role_id = r.id
FROM roles r
WHERE st.role_id IS NULL AND r.code = 'USER';

-- 3) Enforce NOT NULL
ALTER TABLE signup_tokens
    ALTER COLUMN role_id SET NOT NULL;

-- 4) Add FK constraint (no IF NOT EXISTS; Flyway runs once)
ALTER TABLE signup_tokens
    ADD CONSTRAINT fk_signup_tokens_role
        FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT;

-- 5) Index for lookups
CREATE INDEX IF NOT EXISTS idx_signup_tokens_role_id ON signup_tokens (role_id);
