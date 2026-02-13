-- -- ================================================================
-- -- Drop Old Address Columns - URGENT FIX
-- -- Description: Remove old address fields that are causing errors
-- -- Date: 2025-11-13
-- -- ================================================================

-- -- IMPORTANT: Make sure you have backed up any important address data
-- -- before running this script!

-- -- Drop old address columns
-- ALTER TABLE addresses DROP COLUMN IF EXISTS ward;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS district;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS city;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS district_new;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS city_new;

-- -- Make note nullable if not already
-- ALTER TABLE addresses MODIFY COLUMN note VARCHAR(255) NULL;

-- -- Verify the changes
-- SELECT 
--     COLUMN_NAME, 
--     DATA_TYPE, 
--     IS_NULLABLE, 
--     COLUMN_DEFAULT
-- FROM 
--     INFORMATION_SCHEMA.COLUMNS
-- WHERE 
--     TABLE_SCHEMA = DATABASE()
--     AND TABLE_NAME = 'addresses'
-- ORDER BY 
--     ORDINAL_POSITION;
