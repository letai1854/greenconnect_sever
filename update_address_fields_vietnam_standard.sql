-- -- ================================================================
-- -- SQL Migration: Update Address Fields to Vietnam Standard
-- -- Description: Replace old address fields with Vietnam 63-province + 34-province system
-- -- Date: 2025
-- -- ================================================================

-- -- Step 1: Add new columns for 63-province system (all nullable)
-- -- These represent the current Vietnam administrative division standard
-- ALTER TABLE addresses ADD COLUMN province_code_63 VARCHAR(10) NULL;
-- ALTER TABLE addresses ADD COLUMN province_name_63 VARCHAR(100) NULL;
-- ALTER TABLE addresses ADD COLUMN district_code_63 VARCHAR(10) NULL;
-- ALTER TABLE addresses ADD COLUMN district_name_63 VARCHAR(100) NULL;
-- ALTER TABLE addresses ADD COLUMN ward_code_63 VARCHAR(10) NULL;
-- ALTER TABLE addresses ADD COLUMN ward_name_63 VARCHAR(100) NULL;

-- -- Step 2: Add new columns for 34-province system (all nullable)
-- ALTER TABLE addresses ADD COLUMN province_code_34 VARCHAR(10) NULL;
-- ALTER TABLE addresses ADD COLUMN province_name_34 VARCHAR(100) NULL;
-- ALTER TABLE addresses ADD COLUMN ward_code_34 VARCHAR(10) NULL;
-- ALTER TABLE addresses ADD COLUMN ward_name_34 VARCHAR(100) NULL;

-- -- Step 3: Migrate existing data (if needed)
-- -- WARNING: This is a placeholder - you need to implement proper data migration logic
-- -- based on your existing data in ward, district, city, district_new, city_new columns
-- -- You may need to use a Vietnam address API or mapping table to convert old data to codes

-- -- Example migration (adjust based on your actual data):
-- -- UPDATE addresses SET 
-- --   province_code_63 = 'CODE_FROM_MAPPING',
-- --   province_name_63 = city,
-- --   district_code_63 = 'CODE_FROM_MAPPING',
-- --   district_name_63 = district,
-- --   ward_code_63 = 'CODE_FROM_MAPPING',
-- --   ward_name_63 = ward
-- -- WHERE ward IS NOT NULL;

-- -- Step 4: (SKIPPED - All fields are nullable)
-- -- No need to make fields NOT NULL since all address fields are now optional

-- -- Step 5: Make note column nullable (if not already)
-- ALTER TABLE addresses MODIFY COLUMN note VARCHAR(255) NULL;

-- -- Step 6: Drop old columns (CAUTION: Only after successful data migration!)
-- -- ALTER TABLE addresses DROP COLUMN ward;
-- -- ALTER TABLE addresses DROP COLUMN district;
-- -- ALTER TABLE addresses DROP COLUMN city;
-- -- ALTER TABLE addresses DROP COLUMN district_new;
-- -- ALTER TABLE addresses DROP COLUMN city_new;

-- -- Step 7: Add indexes for better query performance (optional)
-- -- CREATE INDEX idx_addresses_province_63 ON addresses(province_code_63);
-- -- CREATE INDEX idx_addresses_district_63 ON addresses(district_code_63);
-- -- CREATE INDEX idx_addresses_ward_63 ON addresses(ward_code_63);

-- -- ================================================================
-- -- MIGRATION GUIDE:
-- -- ================================================================
-- -- 1. Backup your database before running this migration
-- -- 2. Run Steps 1-2 first (add new columns)
-- -- 3. Implement and run Step 3 (data migration logic)
-- -- 4. Verify all addresses have proper 63-province data
-- -- 5. Run Step 4 (make columns NOT NULL)
-- -- 6. Test your application with new fields
-- -- 7. Run Step 6 (drop old columns) only when 100% sure
-- -- 8. Optionally run Step 7 (add indexes)
-- -- ================================================================

-- -- ================================================================
-- -- ROLLBACK PLAN (if needed):
-- -- ================================================================
-- -- If you need to rollback before dropping old columns:
-- -- ALTER TABLE addresses DROP COLUMN province_code_63;
-- -- ALTER TABLE addresses DROP COLUMN province_name_63;
-- -- ALTER TABLE addresses DROP COLUMN district_code_63;
-- -- ALTER TABLE addresses DROP COLUMN district_name_63;
-- -- ALTER TABLE addresses DROP COLUMN ward_code_63;
-- -- ALTER TABLE addresses DROP COLUMN ward_name_63;
-- -- ALTER TABLE addresses DROP COLUMN province_code_34;
-- -- ALTER TABLE addresses DROP COLUMN province_name_34;
-- -- ALTER TABLE addresses DROP COLUMN ward_code_34;
-- -- ALTER TABLE addresses DROP COLUMN ward_name_34;
-- -- ================================================================
