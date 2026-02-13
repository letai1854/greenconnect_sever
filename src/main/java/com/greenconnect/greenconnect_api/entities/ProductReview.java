package com.greenconnect.greenconnect_api.entities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType; // ⭐ THAY ĐỔI: Dùng ArrayList để khởi tạo
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_reviews", indexes = {
    // Index để lấy review theo sản phẩm, sắp xếp theo thời gian
    @Index(name = "idx_review_product_time", columnList = "product_id, review_time DESC"),
    
    // Index để kiểm tra duy nhất: Mỗi chi tiết đơn hàng chỉ được review 1 lần
    @Index(name = "idx_review_order_detail_unique", columnList = "order_detail_id", unique = true),
    
    // Index để Admin/Moderator lọc các review cần duyệt
    @Index(name = "idx_review_approved_status", columnList = "is_approved, review_time"),
    
    // ✅ NEW: Lấy review với rating cao nhất cho mỗi product
    @Index(name = "idx_review_product_rating_time", columnList = "product_id, rating DESC, review_time DESC"),
    
    // ✅ NEW: Lọc review có ảnh theo product
    @Index(name = "idx_review_product_approved_rating", columnList = "product_id, is_approved, rating DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    // Liên kết tới sản phẩm được đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    // Liên kết tới người dùng đã viết đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    // ⭐ CỐT LÕI: Liên kết tới chi tiết đơn hàng để xác minh "Đã mua hàng"
    // Quan hệ OneToOne đảm bảo mỗi mục trong đơn hàng chỉ được review 1 lần.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_detail_id", unique = true, nullable = false)
    private OrderDetail orderDetail;
    
    // Số sao đánh giá (ví dụ: 1 đến 5 sao)
    @Column(name = "rating", nullable = false)
    private Short rating;
    
    // Nội dung bình luận của khách hàng (có thể null)
    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;
    
    // Cờ để quản lý việc hiển thị (nếu bạn có chức năng kiểm duyệt)
    @Column(name = "is_approved", nullable = false)
    @Builder.Default
    private Boolean isApproved = true;
    
    @CreationTimestamp
    @Column(name = "review_time")
    private LocalDateTime reviewTime;
    
    @UpdateTimestamp
    @Column(name = "last_edited_time")
    private LocalDateTime lastEditedTime;
    
    // ----- ⭐ PHẦN BỔ SUNG: SHOP PHẢN HỒI ĐÁNH GIÁ -----
    
    // Nội dung phản hồi từ Shop
    @Column(name = "shop_reply", columnDefinition = "TEXT")
    private String shopReply;
    
    // Thời gian Shop phản hồi
    @Column(name = "shop_reply_at")
    private LocalDateTime shopReplyAt;
    
    // Người phản hồi (Admin/Nhân viên)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_replier_id")
    private User shopReplier;
    
    // ----- KẾT THÚC PHẦN BỔ SUNG -----
    
    // Liên kết tới danh sách media (ảnh/video) của review này
    @OneToMany(mappedBy = "productReview", cascade = CascadeType.ALL, orphanRemoval = true) // ⭐ orphanRemoval = true để tự động xóa media khi review bị xóa
    @Builder.Default
    private List<ReviewMedia> reviewMedia = new ArrayList<>();
}