-- Real Google Routes polylines can exceed the original 20,000-character limit.
-- TEXT preserves the complete encoded route used by the matching algorithm.
ALTER TABLE ride_requests
    ALTER COLUMN route_encoded_polyline TYPE TEXT;
