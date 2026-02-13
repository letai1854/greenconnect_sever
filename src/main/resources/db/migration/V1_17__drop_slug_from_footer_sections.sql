-- -- Make extra columns nullable in footer tables
-- -- These columns exist in DB but not in entity, so make them nullable

-- -- footer_sections: make slug nullable (or drop if exists)
-- ALTER TABLE footer_sections MODIFY COLUMN slug VARCHAR(255) NULL;

-- -- footer_links: make label nullable (or drop if exists)  
-- ALTER TABLE footer_links MODIFY COLUMN label VARCHAR(255) NULL;
