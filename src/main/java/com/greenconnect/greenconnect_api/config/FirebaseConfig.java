package com.greenconnect.greenconnect_api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Configuration
 * <p>Khởi tạo Firebase App khi Spring Boot startup để có thể gửi push notification</p>
 * <p>File service account JSON: src/main/resources/serviceAccountKey.json</p>
 */
@Configuration
@Slf4j
public class FirebaseConfig {

    /**
     * Khởi tạo Firebase App với service account credentials
     * <p>Bean này sẽ được tự động tạo khi Spring Boot khởi động</p>
     * 
     * @return FirebaseApp instance
     * @throws IOException nếu không đọc được file serviceAccountKey.json
     */
    @Bean
    public FirebaseApp firebaseApp() {
        log.info("🔥 [FIREBASE CONFIG] Bắt đầu khởi tạo Firebase App...");
        
        // Kiểm tra xem Firebase App đã được khởi tạo chưa
        // Tránh lỗi duplicate initialization khi Spring restart
        if (FirebaseApp.getApps().isEmpty()) {
            log.info("🔥 [FIREBASE CONFIG] Chưa có Firebase App nào, đang khởi tạo mới...");
            
            try {
                // Đọc file service account JSON từ thư mục resources
                ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                
                // Kiểm tra file có tồn tại không
                if (!resource.exists()) {
                    log.warn("⚠️ [FIREBASE CONFIG] File serviceAccountKey.json không tồn tại!");
                    log.warn("⚠️ [FIREBASE CONFIG] Push notification sẽ KHÔNG hoạt động!");
                    log.warn("⚠️ [FIREBASE CONFIG] Vui lòng thêm file serviceAccountKey.json vào src/main/resources/");
                    return null; // Trả về null, không crash app
                }
                
                InputStream inputStream = resource.getInputStream();
                
                log.info("🔥 [FIREBASE CONFIG] Đã đọc file serviceAccountKey.json thành công");

                // Tạo Firebase Options với credentials
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(inputStream))
                        .build();
                
                // Khởi tạo Firebase App
                FirebaseApp app = FirebaseApp.initializeApp(options);
                
                log.info("✅ [FIREBASE CONFIG] Firebase App đã được khởi tạo thành công!");
                log.info("✅ [FIREBASE CONFIG] App name: {}", app.getName());
                
                return app;
                
            } catch (IOException e) {
                log.error("❌ [FIREBASE CONFIG] LỖI khi đọc file serviceAccountKey.json: {}", e.getMessage());
                log.error("❌ [FIREBASE CONFIG] Vui lòng kiểm tra file có tồn tại tại: src/main/resources/serviceAccountKey.json");
                log.warn("⚠️ [FIREBASE CONFIG] App sẽ chạy KHÔNG có Firebase (push notification disabled)");
                return null; // Trả về null thay vì throw exception
            }
            
        } else {
            log.info("✅ [FIREBASE CONFIG] Firebase App đã tồn tại, sử dụng instance hiện tại");
            return FirebaseApp.getInstance();
        }
    }
}