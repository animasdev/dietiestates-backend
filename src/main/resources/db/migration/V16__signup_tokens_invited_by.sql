-- V16: add invited_by to signup_tokens to persist inviter through onboarding
ALTER TABLE signup_tokens
    ADD COLUMN IF NOT EXISTS invited_by uuid NULL;

ALTER TABLE signup_tokens
    ADD CONSTRAINT fk_signup_tokens_invited_by
        FOREIGN KEY (invited_by)
        REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_signup_tokens_invited_by ON signup_tokens (invited_by);
