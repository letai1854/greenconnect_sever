package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Banner;

/**
 * Repository interface cho thực thể Banner.
 * 
 * <p>Quản lý các banner quảng cáo hiển thị trên trang chủ hoặc các trang khác.
 * Bảng này cho phép Admin dễ dàng thay đổi các hình ảnh quảng cáo và các
 * đường link liên kết.</p>
 */
@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    /**
     * Lấy danh sách tất cả các banner đang ở trạng thái hoạt động,
     * sắp xếp theo thứ tự hiển thị đã được định sẵn.
     * <p>Đây là phương thức chính được {@code HomepageLayoutService} hoặc các service
     * khác gọi để lấy ra danh sách banner cần hiển thị trên một vị trí cụ thể
     * của trang web.</p>
     *
     * @return một danh sách (List) các đối tượng {@link Banner}, đã được sắp xếp.
     */
    java.util.List<Banner> findByBannerGroupIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID bannerGroupId);
}