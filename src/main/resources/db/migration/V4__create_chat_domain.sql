ALTER TABLE ride_matches ADD COLUMN ride_request_1_accepted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE ride_matches ADD COLUMN ride_request_2_accepted_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE chats (
    id UUID PRIMARY KEY,
    ride_match_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chats_ride_match FOREIGN KEY (ride_match_id) REFERENCES ride_matches (id),
    CONSTRAINT uk_chats_ride_match UNIQUE (ride_match_id)
);

CREATE TABLE chat_participants (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_chat_participants_chat FOREIGN KEY (chat_id) REFERENCES chats (id),
    CONSTRAINT fk_chat_participants_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT uk_chat_participants_chat_user UNIQUE (chat_id, user_id)
);

CREATE TABLE messages (
    id UUID PRIMARY KEY,
    chat_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    message VARCHAR(4000) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_messages_chat FOREIGN KEY (chat_id) REFERENCES chats (id),
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES app_users (id),
    CONSTRAINT chk_messages_not_blank CHECK (LENGTH(TRIM(message)) > 0)
);

CREATE INDEX idx_chat_participants_user ON chat_participants (user_id);
CREATE INDEX idx_messages_chat_sent ON messages (chat_id, sent_at);
CREATE INDEX idx_messages_sender ON messages (sender_id);
