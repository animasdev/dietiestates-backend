-- Flyway V14: moderation actions log for listings

CREATE TABLE IF NOT EXISTS listing_moderation_actions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    listing_id uuid NOT NULL REFERENCES listings(id) ON DELETE CASCADE,
    performed_by_user_id uuid NOT NULL REFERENCES users(id),
    performed_by_role text NOT NULL,
    action_type_id uuid NOT NULL REFERENCES moderation_action_types(id),
    reason text NULL CHECK (reason IS NULL OR char_length(reason) <= 500),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_listing_moderation_listing_id ON listing_moderation_actions(listing_id);
CREATE INDEX IF NOT EXISTS idx_listing_moderation_action_type ON listing_moderation_actions(action_type_id);
CREATE INDEX IF NOT EXISTS idx_listing_moderation_created_at ON listing_moderation_actions(created_at DESC);
