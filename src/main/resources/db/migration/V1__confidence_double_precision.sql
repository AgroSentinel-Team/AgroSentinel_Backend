DO $$
BEGIN
    IF to_regclass('predictive_records') IS NOT NULL THEN
        ALTER TABLE predictive_records
            ALTER COLUMN confidence TYPE DOUBLE PRECISION
            USING confidence::DOUBLE PRECISION;
    END IF;
END;
$$;