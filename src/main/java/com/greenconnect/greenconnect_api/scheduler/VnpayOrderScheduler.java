package com.greenconnect.greenconnect_api.scheduler;

import com.greenconnect.greenconnect_api.services.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 🕐 Scheduler tự động hủy đơn hàng VNPay quá hạn
 * 
 * <p><b>Mục đích:</b></p>
 * <ul>
 *   <li>Tự động hủy đơn hàng VNPay chưa thanh toán sau 15 phút</li>
 *   <li>Rollback voucher (nếu có)</li>
 *   <li>Đảm bảo không trừ stock cho đơn hàng không thanh toán</li>
 * </ul>
 * 
 * <p><b>⚠️ LƯU Ý:</b></p>
 * <p>Stock KHÔNG cần hoàn lại vì theo flow mới, VNPay order chưa trừ stock!</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VnpayOrderScheduler {

    private final OrderService orderService;

    /**
     * Runs every 30 minutes to check and cancel expired VNPay orders
     * Cron expression: 0 at second, every 30 minutes, every hour, every day
     */
    @Scheduled(cron = "0 */30 * * * *") // Chạy mỗi 30 phút
    public void scheduledCancelExpiredVnpayOrders() {
        log.info("🕐 Starting scheduled task: Cancel expired VNPay orders");
        
        int cancelledCount = orderService.cancelExpiredVnpayOrders();
        
        log.info("✅ Scheduled task completed: {} orders cancelled", cancelledCount);
    }
}
