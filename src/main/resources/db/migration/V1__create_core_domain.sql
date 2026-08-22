CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone_number VARCHAR(30),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_app_users_email UNIQUE (email)
);

CREATE TABLE ride_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    pickup_address VARCHAR(500) NOT NULL,
    pickup_latitude NUMERIC(9, 6),
    pickup_longitude NUMERIC(9, 6),
    destination_address VARCHAR(500) NOT NULL,
    destination_latitude NUMERIC(9, 6),
    destination_longitude NUMERIC(9, 6),
    departure_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ride_requests_user FOREIGN KEY (user_id) REFERENCES app_users (id),
    CONSTRAINT chk_ride_requests_status CHECK (status IN ('OPEN', 'MATCHED', 'CANCELLED', 'COMPLETED'))
);

CREATE INDEX idx_ride_requests_user_id ON ride_requests (user_id);
CREATE INDEX idx_ride_requests_status_departure ON ride_requests (status, departure_time);
