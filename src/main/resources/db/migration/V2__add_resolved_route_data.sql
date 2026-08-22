ALTER TABLE ride_requests ADD COLUMN pickup_place_id VARCHAR(255);
ALTER TABLE ride_requests ADD COLUMN destination_place_id VARCHAR(255);
ALTER TABLE ride_requests ADD COLUMN route_distance_meters BIGINT;
ALTER TABLE ride_requests ADD COLUMN route_duration_seconds BIGINT;
ALTER TABLE ride_requests ADD COLUMN route_encoded_polyline VARCHAR(20000);
