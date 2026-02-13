-- -- ==========================================
-- -- Footer Management System - PostgreSQL Migration
-- -- ==========================================

-- -- Create footer_sections table
-- CREATE TABLE IF NOT EXISTS footer_sections (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     name VARCHAR(255) NOT NULL,
--     sort_order INTEGER NOT NULL,
--     is_active BOOLEAN NOT NULL DEFAULT true,
--     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--     updated_at TIMESTAMP NOT NULL DEFAULT NOW()
-- );

-- -- Create footer_links table
-- CREATE TABLE IF NOT EXISTS footer_links (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     section_id UUID NOT NULL,
--     icon_key VARCHAR(50) NOT NULL,
--     value TEXT NOT NULL,
--     is_active BOOLEAN NOT NULL DEFAULT true,
--     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
--     CONSTRAINT fk_section FOREIGN KEY (section_id) 
--         REFERENCES footer_sections(id) ON DELETE CASCADE
-- );

-- -- Create indexes for performance
-- CREATE INDEX IF NOT EXISTS idx_footer_sections_sort_order ON footer_sections(sort_order);
-- CREATE INDEX IF NOT EXISTS idx_footer_links_section_id ON footer_links(section_id);

-- -- ==========================================
-- -- Trigger Function for updated_at
-- -- ==========================================
-- CREATE OR REPLACE FUNCTION update_updated_at_column()
-- RETURNS TRIGGER AS $$
-- BEGIN
--     NEW.updated_at = NOW();
--     RETURN NEW;
-- END;
-- $$ LANGUAGE plpgsql;

-- -- Apply triggers
-- DROP TRIGGER IF EXISTS update_footer_sections_updated_at ON footer_sections;
-- CREATE TRIGGER update_footer_sections_updated_at
--     BEFORE UPDATE ON footer_sections
--     FOR EACH ROW
--     EXECUTE FUNCTION update_updated_at_column();

-- DROP TRIGGER IF EXISTS update_footer_links_updated_at ON footer_links;
-- CREATE TRIGGER update_footer_links_updated_at
--     BEFORE UPDATE ON footer_links
--     FOR EACH ROW
--     EXECUTE FUNCTION update_updated_at_column();

-- -- ==========================================
-- -- Insert Sample Data
-- -- ==========================================

-- -- Clear existing data (optional - comment out in production)
-- -- DELETE FROM footer_links;
-- -- DELETE FROM footer_sections;

-- -- Insert sections
-- INSERT INTO footer_sections (id, name, sort_order, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440001', 'Công ty', 1, true),
-- ('550e8400-e29b-41d4-a716-446655440002', 'Cần giúp đỡ', 2, true),
-- ('550e8400-e29b-41d4-a716-446655440003', 'Địa chỉ', 3, true),
-- ('550e8400-e29b-41d4-a716-446655440004', 'Tải ứng dụng', 4, true)
-- ON CONFLICT (id) DO NOTHING;

-- -- Insert links for section 1 (Công ty - Social media)
-- INSERT INTO footer_links (section_id, icon_key, value, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440001', 'FACEBOOK', 'https://facebook.com/greenconnect', true),
-- ('550e8400-e29b-41d4-a716-446655440001', 'INSTAGRAM', 'https://instagram.com/greenconnect', true),
-- ('550e8400-e29b-41d4-a716-446655440001', 'TWITTER', 'https://twitter.com/greenconnect', true)
-- ON CONFLICT DO NOTHING;

-- -- Insert links for section 2 (Cần giúp đỡ - Support)
-- INSERT INTO footer_links (section_id, icon_key, value, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440002', 'PHONE', '1900 1234', true),
-- ('550e8400-e29b-41d4-a716-446655440002', 'EMAIL', 'hotro@greenconnect.vn', true)
-- ON CONFLICT DO NOTHING;

-- -- Insert links for section 3 (Địa chỉ - Address)
-- INSERT INTO footer_links (section_id, icon_key, value, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440003', 'ADDRESS', '123 Đường ABC, Phường XYZ, TP. Hồ Chí Minh', true)
-- ON CONFLICT DO NOTHING;

-- -- Insert links for section 4 (Tải ứng dụng - Download)
-- INSERT INTO footer_links (section_id, icon_key, value, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440004', 'DOWNLOAD', 'https://greenconnect.vn/download', true)
-- ON CONFLICT DO NOTHING;

-- -- ==========================================
-- -- Verification Queries
-- -- ==========================================

-- -- Count sections and links
-- SELECT 'Sections' as type, COUNT(*) as count FROM footer_sections
-- UNION ALL
-- SELECT 'Links' as type, COUNT(*) as count FROM footer_links;

-- -- Display full footer structure
-- SELECT 
--     fs.id as section_id,
--     fs.name as section_name,
--     fs.sort_order,
--     fl.id as link_id,
--     fl.icon_key,
--     fl.value
-- FROM footer_sections fs
-- LEFT JOIN footer_links fl ON fs.id = fl.section_id
-- ORDER BY fs.sort_order, fl.created_at;

-- -- ==========================================
-- -- Cleanup Script (Run only if needed)
-- -- ==========================================

-- -- DROP TABLE IF EXISTS footer_links CASCADE;
-- -- DROP TABLE IF EXISTS footer_sections CASCADE;
-- -- DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
