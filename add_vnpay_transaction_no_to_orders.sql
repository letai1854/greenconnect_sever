-- Thêm cột vnpay_transaction_no vào bảng orders
-- Mục đích: Lưu mã giao dịch VNPay (vnp_TransactionNo) để dùng cho API refund

ALTER TABLE orders 
ADD COLUMN vnpay_transaction_no VARCHAR(100) COMMENT 'Mã giao dịch VNPay (vnp_TransactionNo) - dùng cho refund API';

-- Index cho tìm kiếm theo vnpay_transaction_no (nếu cần)
CREATE INDEX idx_orders_vnpay_transaction_no ON orders(vnpay_transaction_no);
