-- -- Make 'city' column nullable in addresses table
-- -- Reason: This column is not being used in the current address system

-- ALTER TABLE addresses MODIFY COLUMN city VARCHAR(100) NULL;

-- -- Optional: Also make cityNew nullable if it exists
-- ALTER TABLE addresses MODIFY COLUMN cityNew VARCHAR(100) NULL;
