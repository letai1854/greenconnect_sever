package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.BannerGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface cho thực thể BannerGroup.
 * <p>
 * Quản lý các nhóm banner, mỗi nhóm chứa một hoặc nhiều banner và có một layout hiển thị chung.
 * </p>
 */
@Repository
public interface BannerGroupRepository extends JpaRepository<BannerGroup, UUID> {

    /**
     * Tìm một BannerGroup bằng thuộc tính groupKey duy nhất của nó.
     * Phương thức này rất quan trọng để kiểm tra sự tồn tại của một nhóm trước khi tạo mới,
     * hoặc để tìm một nhóm và thêm banner mới vào đó.
     *
     * @param groupKey Khóa duy nhất của nhóm banner cần tìm.
     * @return một {@link Optional} chứa {@link BannerGroup} nếu tìm thấy, ngược lại là Optional rỗng.
     */
    Optional<BannerGroup> findByGroupKey(String groupKey);

}