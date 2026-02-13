-- -- Fix enum column lengths in order_requests and request_media tables
-- -- This prevents "Data truncated for column" errors

-- USE greenconnect;

-- -- Fix order_requests table enum columns
-- ALTER TABLE order_requests 
--     MODIFY COLUMN request_type VARCHAR(50)  NULL,
--     MODIFY COLUMN created_by VARCHAR(50)  NULL,
--     MODIFY COLUMN status VARCHAR(50)  NULL,
--     MODIFY COLUMN refund_status VARCHAR(50) NULL;

-- -- Fix request_media table enum column
-- ALTER TABLE request_media 
--     MODIFY COLUMN media_type VARCHAR(50)  NULL;

-- -- Verify the changes
-- DESCRIBE order_requests;
-- DESCRIBE request_media;
