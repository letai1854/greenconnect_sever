-- Add order_code column to order_requests table
-- This column will store the order code reference similar to orders table

ALTER TABLE order_requests 
ADD COLUMN order_code VARCHAR(100) NULL AFTER request_id;

-- Add index for order_code to improve query performance
CREATE INDEX idx_order_request_order_code ON order_requests(order_code);

-- Optional: Update existing records to copy order_code from orders table
-- Uncomment the following line if you want to populate existing records
-- UPDATE order_requests or_req 
-- INNER JOIN orders o ON or_req.order_id = o.id 
-- SET or_req.order_code = o.order_code 
-- WHERE or_req.order_code IS NULL AND or_req.order_id IS NOT NULL;
