package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.HomepageLayout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HomepageLayoutRepository extends JpaRepository<HomepageLayout, UUID> {
    /**
     * Lấy tất cả các layout đang hoạt động, sắp xếp theo thứ tự hiển thị.
     * Đây là truy vấn cốt lõi để xây dựng cấu trúc trang chủ.
     */
    // The entity does not have a 'displayOrder' field itself; campaigns within a layout have an order.
    // Return all active homepage layouts; ordering can be applied by the caller if needed.
    List<HomepageLayout> findByIsActiveTrue();
}