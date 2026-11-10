ALTER TABLE trips
    ALTER COLUMN starts_on DROP NOT NULL,
    ALTER COLUMN ends_on DROP NOT NULL,
    ALTER COLUMN duration_days DROP EXPRESSION,
    ALTER COLUMN duration_days SET NOT NULL,
    DROP CONSTRAINT ck_trips_period,
    ADD CONSTRAINT ck_trips_period CHECK (
        duration_days > 0 AND (
            (starts_on IS NULL AND ends_on IS NULL) OR
            (starts_on IS NOT NULL AND ends_on IS NOT NULL AND ends_on >= starts_on AND duration_days = (ends_on - starts_on + 1))
        )
    );

CREATE OR REPLACE FUNCTION validate_trip_period() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM trip_days WHERE trip_id = NEW.id AND day_number > NEW.duration_days) THEN
        RAISE EXCEPTION 'Reallocate activities and remove days outside the new period before shortening the trip'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER trg_trips_period ON trips;
CREATE TRIGGER trg_trips_period BEFORE UPDATE OF starts_on, ends_on, duration_days ON trips
    FOR EACH ROW EXECUTE FUNCTION validate_trip_period();
