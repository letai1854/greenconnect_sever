package com.greenconnect.greenconnect_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.SupplierCertification;
import com.greenconnect.greenconnect_api.entities.SupplierCertificationId;

/**
 * Repository interface cho thực thể SupplierCertification.
 * 
 * <p>Quản lý mối quan hệ nhiều-nhiều giữa Nhà cung cấp (Supplier) và Chứng nhận (Certification).
 * Do sử dụng khóa chính kết hợp ({@link SupplierCertificationId}), repository này kế thừa
 * {@code JpaRepository<SupplierCertification, SupplierCertificationId>}.</p>
 */
@Repository
public interface SupplierCertificationRepository extends JpaRepository<SupplierCertification, SupplierCertificationId> {

    /**
     * Lấy danh sách tất cả các chứng nhận mà một nhà cung cấp sở hữu.
     * <p>Đây là phương thức chính, được sử dụng trong trang chi tiết nhà cung cấp để hiển thị
     * các chứng nhận uy tín như VietGAP, GlobalG.A.P....</p>
     *
    //  * @param supplierId ID của nhà cung cấp.
    //  * @return một danh sách (List) các đối tượng {@link SupplierCertification}.
    //  */
    // List<SupplierCertification> findByIdSupplierId(UUID supplierId);

    // /**
    //  * Lấy danh sách tất cả các nhà cung cấp sở hữu một chứng nhận cụ thể.
    //  * <p>Hữu ích cho chức năng lọc sản phẩm/nhà cung cấp. Ví dụ: "Hiển thị tất cả
    //  * các nhà cung cấp có chứng nhận VietGAP".</p>
    //  *
    //  * @param certificationId ID của chứng nhận.
    //  * @return một danh sách (List) các đối tượng {@link SupplierCertification}.
    //  */
    // List<SupplierCertification> findByIdCertificationId(UUID certificationId);

    // /**
    //  * Xóa tất cả các liên kết chứng nhận của một nhà cung cấp.
    //  * <p>Rất quan trọng khi xóa một nhà cung cấp. Service sẽ gọi hàm này trước để xóa
    //  * các mối quan hệ trong bảng này, đảm bảo tính toàn vẹn dữ liệu.</p>
    //  *
    //  * @param supplierId ID của nhà cung cấp cần xóa các liên kết chứng nhận.
    //  */
    // @Transactional
    // @Modifying
    // void deleteByIdSupplierId(UUID supplierId);

    // /**
    //  * Xóa tất cả các liên kết chứng nhận khi một loại chứng nhận bị xóa.
    //  * <p>Tương tự, khi Admin xóa một loại chứng nhận (ví dụ: chứng nhận X đã lỗi thời),
    //  * tất cả các liên kết của nó với các nhà cung cấp cũng cần được xóa.</p>
    //  *
    //  * @param certificationId ID của chứng nhận cần xóa các liên kết.
    //  */
    // @Transactional
    // @Modifying
    // void deleteByIdCertificationId(UUID certificationId);
}