-- V15: add invited_by column to users to track inviter user id
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS invited_by uuid NULL;

-- Maintain referential integrity but keep historical invites even if inviter is deleted
ALTER TABLE users
    ADD CONSTRAINT fk_users_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES users(id)
        ON DELETE SET NULL;

-- Optional index to speed up lookups by inviter (e.g. moderation dashboards)
CREATE INDEX IF NOT EXISTS idx_users_invited_by ON users (invited_by);
