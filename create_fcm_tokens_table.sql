-- =====================================================
-- FCM Token Management Table
-- =====================================================
-- Purpose: Store Firebase Cloud Messaging tokens for push notifications
-- Features: Unique token constraint, device type classification, user relationship

CREATE TABLE IF NOT EXISTS fcm_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token VARCHAR(500) NOT NULL UNIQUE,
    device_type VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_fcm_token_user 
        FOREIGN KEY (user_id) 
        REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Check constraint for device_type
    CONSTRAINT chk_device_type 
        CHECK (device_type IN ('ANDROID', 'IOS', 'WEB'))
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_fcm_token ON fcm_tokens(token);
CREATE INDEX IF NOT EXISTS idx_fcm_user_id ON fcm_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_fcm_user_active ON fcm_tokens(user_id, is_active);

-- Add comments for documentation
COMMENT ON TABLE fcm_tokens IS 'Firebase Cloud Messaging tokens for push notifications';
COMMENT ON COLUMN fcm_tokens.id IS 'Primary key (UUID)';
COMMENT ON COLUMN fcm_tokens.user_id IS 'Foreign key to users table';
COMMENT ON COLUMN fcm_tokens.token IS 'FCM token (unique identifier for device)';
COMMENT ON COLUMN fcm_tokens.device_type IS 'Device type: ANDROID, IOS, or WEB';
COMMENT ON COLUMN fcm_tokens.is_active IS 'Whether the token is currently active';
COMMENT ON COLUMN fcm_tokens.last_updated IS 'Last time the token was used/updated';

-- Example data (optional - for testing)
-- INSERT INTO fcm_tokens (id, user_id, token, device_type, is_active, last_updated)
-- VALUES (
--     gen_random_uuid(),
--     'user_uuid_here',
--     'fcm_token_example_xyz123',
--     'ANDROID',
--     TRUE,
--     CURRENT_TIMESTAMP
-- );
