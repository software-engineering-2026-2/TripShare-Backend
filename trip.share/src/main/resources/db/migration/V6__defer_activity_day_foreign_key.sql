-- Trips cascade to days and itineraries. Check only after both cascades have completed.
ALTER TABLE itinerary_activities
    ALTER CONSTRAINT fk_itinerary_activities_day DEFERRABLE INITIALLY DEFERRED;
