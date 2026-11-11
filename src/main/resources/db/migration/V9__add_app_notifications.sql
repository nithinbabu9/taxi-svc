CREATE TABLE app_notifications (
    id UUID PRIMARY KEY,
    recipient_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(160) NOT NULL,
    body VARCHAR(500) NOT NULL,
    chat_id UUID,
    ride_match_id UUID,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_app_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES app_users (id) ON DELETE CASCADE,
    CONSTRAINT fk_app_notifications_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE SET NULL,
    CONSTRAINT fk_app_notifications_ride_match FOREIGN KEY (ride_match_id) REFERENCES ride_matches (id) ON DELETE SET NULL
);

CREATE INDEX idx_app_notifications_recipient_created ON app_notifications (recipient_id, created_at DESC);
CREATE INDEX idx_app_notifications_recipient_unread ON app_notifications (recipient_id, read_at);
