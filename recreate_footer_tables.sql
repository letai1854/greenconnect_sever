-- -- =====================================================
-- -- DROP và TẠO LẠI footer tables
-- -- Chạy trực tiếp trong MySQL
-- -- =====================================================

-- -- 1. Xóa bảng cũ (footer_links trước vì có FK)
-- DROP TABLE IF EXISTS footer_links;
-- DROP TABLE IF EXISTS footer_sections;

-- -- 2. Tạo lại footer_sections (BINARY(16) cho UUID - giống các bảng khác)
-- CREATE TABLE footer_sections (
--     id BINARY(16) PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     sort_order INT NOT NULL,
--     is_active BOOLEAN NOT NULL DEFAULT true,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     INDEX idx_footer_section_sort (sort_order)
-- );

-- -- 3. Tạo lại footer_links (BINARY(16) cho UUID)
-- CREATE TABLE footer_links (
--     id BINARY(16) PRIMARY KEY,
--     section_id BINARY(16) NOT NULL,
--     icon_key VARCHAR(50) NOT NULL,
--     value TEXT NOT NULL,
--     is_active BOOLEAN NOT NULL DEFAULT true,
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
--     INDEX idx_footer_link_section (section_id),
--     CONSTRAINT fk_footer_link_section FOREIGN KEY (section_id) 
--         REFERENCES footer_sections(id) ON DELETE CASCADE
-- );

-- -- 4. Insert sample data
-- INSERT INTO footer_sections (id, name, sort_order, is_active) VALUES
-- ('550e8400-e29b-41d4-a716-446655440001', 'Công ty', 1, true),
-- ('550e8400-e29b-41d4-a716-446655440002', 'Cần giúp đỡ', 2, true),
-- ('550e8400-e29b-41d4-a716-446655440003', 'Địa chỉ', 3, true),
-- ('550e8400-e29b-41d4-a716-446655440004', 'Tải ứng dụng', 4, true);

-- INSERT INTO footer_links (id, section_id, icon_key, value, is_active) VALUES
-- (UUID(), '550e8400-e29b-41d4-a716-446655440001', 'FACEBOOK', 'https://facebook.com/greenconnect', true),
-- (UUID(), '550e8400-e29b-41d4-a716-446655440001', 'INSTAGRAM', 'https://instagram.com/greenconnect', true),
-- (UUID(), '550e8400-e29b-41d4-a716-446655440002', 'PHONE', '1900 1234', true),
-- (UUID(), '550e8400-e29b-41d4-a716-446655440002', 'EMAIL', 'hotro@greenconnect.vn', true),
-- (UUID(), '550e8400-e29b-41d4-a716-446655440003', 'ADDRESS', '123 Đường ABC, TP.HCM', true),
-- (UUID(), '550e8400-e29b-41d4-a716-446655440004', 'DOWNLOAD', 'https://greenconnect.vn/download', true);

-- -- 5. Verify
-- SELECT * FROM footer_sections ORDER BY sort_order;
-- SELECT * FROM footer_links;
