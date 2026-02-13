-- V1__drop_old_address_columns.sql
-- Drop old address columns that conflict with new Vietnam standard fields

-- ALTER TABLE addresses DROP COLUMN IF EXISTS ward;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS district;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS city;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS district_new;
-- ALTER TABLE addresses DROP COLUMN IF EXISTS city_new;

-- -- Make note nullable
-- ALTER TABLE addresses MODIFY COLUMN note VARCHAR(255) NULL;
