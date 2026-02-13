package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.SupplierMedia;

/**
 * Repository interface cho thực thể SupplierMedia.
 * 
 * <p>Quản lý các đối tượng media (hình ảnh, video) liên quan đến một nhà cung cấp.
 * Cung cấp các phương thức để truy vấn danh sách media cho một nhà cung cấp cụ thể
 * và thực hiện các thao tác xóa hiệu quả.</p>
 */
@Repository
public interface SupplierMediaRepository extends JpaRepository<SupplierMedia, UUID> {

    /**
     * Lấy tất cả các đối tượng media thuộc về một nhà cung cấp cụ thể.
     * <p>Đây là phương thức chính, được sử dụng khi hiển thị trang chi tiết nhà cung cấp
     * để lấy ra toàn bộ thư viện ảnh/video giới thiệu của họ.</p>
     *
    //  * @param supplierId ID của nhà cung cấp.
    //  * @return một danh sách (List) các đối tượng {@link SupplierMedia}.
    //  */
    // List<SupplierMedia> findBySupplierId(UUID supplierId);

    // /**
    //  * Lấy tất cả các đối tượng media của một nhà cung cấp và theo một loại media cụ thể.
    //  * <p>Hữu ích khi bạn muốn tách biệt việc hiển thị hình ảnh và video trên giao diện.</p>
    //  *
    //  * @param supplierId ID của nhà cung cấp.
    //  * @param mediaType  Loại media cần lọc (IMAGE hoặc VIDEO).
    //  * @return một danh sách (List) các đối tượng {@link SupplierMedia} phù hợp.
    //  */
    // List<SupplierMedia> findBySupplierIdAndMediaType(UUID supplierId, MediaType mediaType);

    // /**
    //  * Đếm số lượng media của một nhà cung cấp.
    //  *
    //  * @param supplierId ID của nhà cung cấp.
    //  * @return số lượng media hiện có của nhà cung cấp đó.
    //  */
    // long countBySupplierId(UUID supplierId);

    // /**
    //  * Xóa tất cả các media liên quan đến một nhà cung cấp.
    //  * <p>Phương thức này rất quan trọng và hiệu quả. Khi Admin quyết định xóa một nhà cung cấp,
    //  * trước tiên, service sẽ gọi hàm này để xóa hết tất cả các media liên quan,
    //  * sau đó mới xóa bản thân nhà cung cấp để tránh lỗi khóa ngoại.</p>
    //  *
    //  * @param supplierId ID của nhà cung cấp cần xóa media.
    //  */
    // @Transactional
    // @Modifying
    // void deleteBySupplierId(UUID supplierId);
}