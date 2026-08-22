CREATE TABLE ride_matches (
    id UUID PRIMARY KEY,
    ride_request_1_id UUID NOT NULL,
    ride_request_2_id UUID NOT NULL,
    match_score DOUBLE PRECISION NOT NULL,
    pickup_distance_meters DOUBLE PRECISION NOT NULL,
    destination_distance_meters DOUBLE PRECISION NOT NULL,
    route_similarity DOUBLE PRECISION NOT NULL,
    departure_time_difference_seconds BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ride_matches_ride_1 FOREIGN KEY (ride_request_1_id) REFERENCES ride_requests (id),
    CONSTRAINT fk_ride_matches_ride_2 FOREIGN KEY (ride_request_2_id) REFERENCES ride_requests (id),
    CONSTRAINT uk_ride_matches_pair UNIQUE (ride_request_1_id, ride_request_2_id),
    CONSTRAINT chk_ride_matches_distinct CHECK (ride_request_1_id <> ride_request_2_id),
    CONSTRAINT chk_ride_matches_score CHECK (match_score BETWEEN 0 AND 1),
    CONSTRAINT chk_ride_matches_similarity CHECK (route_similarity BETWEEN 0 AND 1),
    CONSTRAINT chk_ride_matches_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'))
);

CREATE INDEX idx_ride_matches_ride_1 ON ride_matches (ride_request_1_id);
CREATE INDEX idx_ride_matches_ride_2 ON ride_matches (ride_request_2_id);
CREATE INDEX idx_ride_matches_status ON ride_matches (status);
