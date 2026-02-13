package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    /**
     * SỬA LỖI: Đổi tên phương thức từ findByUserId thành findByUser_Id.
     * Cú pháp "User_Id" chỉ cho Spring Data JPA biết rằng nó cần truy vấn
     * dựa trên trường 'id' của thuộc tính 'user' trong entity Address.
     */
    List<Address> findByUser_Id(UUID userId);
    
    /**
     * Tìm địa chỉ mặc định của user.
     * <p>Sử dụng userId (read-only field) để query nhanh hơn so với user_id FK.</p>
     * <p>Index: idx_address_user_default (user_id, is_default) được sử dụng.</p>
     * 
     * @param userId UUID của user
     * @return Optional<Address> chứa địa chỉ mặc định nếu có
     */
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}