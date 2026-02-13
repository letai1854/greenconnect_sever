-- Script để cho phép các cột địa chỉ có thể NULL
-- Chạy script này trên MySQL để fix lỗi "Column cannot be null"

USE greenconnect;

-- Cho phép tất cả các cột mã địa chỉ có thể NULL
ALTER TABLE addresses MODIFY COLUMN province_code_34 VARCHAR(10) NULL;
ALTER TABLE addresses MODIFY COLUMN province_name_34 VARCHAR(100) NULL;
ALTER TABLE addresses MODIFY COLUMN province_code_63 VARCHAR(10) NULL;
ALTER TABLE addresses MODIFY COLUMN province_name_63 VARCHAR(100) NULL;

ALTER TABLE addresses MODIFY COLUMN district_code_63 VARCHAR(10) NULL;
ALTER TABLE addresses MODIFY COLUMN district_name_63 VARCHAR(100) NULL;

ALTER TABLE addresses MODIFY COLUMN ward_code_34 VARCHAR(10) NULL;
ALTER TABLE addresses MODIFY COLUMN ward_name_34 VARCHAR(100) NULL;
ALTER TABLE addresses MODIFY COLUMN ward_code_63 VARCHAR(10) NULL;
ALTER TABLE addresses MODIFY COLUMN ward_name_63 VARCHAR(100) NULL;

-- Cho phép các trường optional khác cũng NULL
ALTER TABLE addresses MODIFY COLUMN note VARCHAR(500) NULL;
ALTER TABLE addresses MODIFY COLUMN street_address VARCHAR(255) NULL;

-- Verify changes
DESCRIBE addresses;
