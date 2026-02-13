-- -- Drop 'city' column from addresses table
-- -- Reason: Not used in new address system (using province_code_63/province_code_34 instead)

-- ALTER TABLE addresses DROP COLUMN IF EXISTS city;

-- -- Optional: Drop cityNew column if it exists and is not used
-- ALTER TABLE addresses DROP COLUMN IF EXISTS cityNew;
