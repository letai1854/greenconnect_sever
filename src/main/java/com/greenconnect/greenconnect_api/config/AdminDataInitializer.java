package com.greenconnect.greenconnect_api.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.entities.UserRole;
import com.greenconnect.greenconnect_api.enums.Provider;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.enums.UserStatus;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.repositories.UserRoleRepository;
import com.greenconnect.greenconnect_api.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Khởi tạo dữ liệu admin mặc định khi server startup.
 * <p>Tự động tạo tài khoản admin nếu chưa có trong hệ thống.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 Khởi tạo dữ liệu admin...");
        
        try {
            createDefaultAdminIfNotExists();
            log.info("✅ Hoàn tất khởi tạo dữ liệu admin");
        } catch (Exception e) {
            log.error("❌ Lỗi khi khởi tạo dữ liệu admin: {}", e.getMessage(), e);
        }
    }

    /**
     * Tạo tài khoản admin mặc định nếu chưa có.
     */
    private void createDefaultAdminIfNotExists() {
        // Kiểm tra xem đã có admin nào chưa
        boolean hasAdmin = userRoleRepository.existsByRole(Role.ADMIN);
        
        if (hasAdmin) {
            log.info("✅ Đã có tài khoản admin trong hệ thống");
            return;
        }

        log.info("📝 Không tìm thấy admin, đang tạo tài khoản admin mặc định...");

        // Tạo admin user
        User adminUser = User.builder()
                .email("greenconnect11@gmail.com")
                .fullName("GreenConnect Administrator")
                .phoneNumber("0900000000")
                .passwordHash(PasswordUtils.hashPassword("Admin123"))
                .provider(Provider.LOCAL)
                .status(UserStatus.ACTIVE)
                .loyaltyPoints(BigDecimal.ZERO)
                .avatarUrl("https://ui-avatars.com/api/?name=Admin&background=4f46e5&color=fff")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Lưu user trước
        User savedAdmin = userRepository.save(adminUser);
        log.info("👤 Đã tạo admin user với ID: {}", savedAdmin.getId());

        // Assign ADMIN role
        UserRole adminRole = UserRole.builder()
                .user(savedAdmin)
                .role(Role.ADMIN)
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();
        
        userRoleRepository.save(adminRole);
        log.info("🔑 Đã gán role ADMIN cho user: {}", savedAdmin.getEmail());

        // Log thông tin đăng nhập
        log.info("🎉 Tài khoản admin đã được tạo thành công!");
        log.info("📧 Email: admin@greenconnect.com");
        log.info("🔐 Password: admin123");
        log.info("⚠️  QUAN TRỌNG: Hãy đổi password sau lần đăng nhập đầu tiên!");
    }
}