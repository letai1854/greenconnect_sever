package com.greenconnect.greenconnect_api.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.OtpCode;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.repositories.OtpCodeRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;

@Service
public class OtpService {
    
    @Autowired
    private OtpCodeRepository otpCodeRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 3;
    private static final int OTP_COOLDOWN_SECONDS = 60; // Cooldown 1 phút giữa các lần gửi
    
    /**
     * Tạo và gửi OTP cho email người dùng
     */
    @Transactional
    public void generateAndSendOtp(String email) {
        // Tìm user theo email
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        // Kiểm tra xem có OTP còn hiệu lực không để tránh spam
        Optional<OtpCode> latestOtp = otpCodeRepository.findLatestByUserEmail(email);
        if (latestOtp.isPresent()) {
            OtpCode existingOtp = latestOtp.get();
            if (existingOtp.getExpiryDate().isAfter(LocalDateTime.now())) {
                // Còn hiệu lực
                long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), existingOtp.getExpiryDate()).toMinutes();
                throw new RuntimeException("Bạn đã có mã OTP còn hiệu lực. Vui lòng kiểm tra email hoặc đợi " + remainingMinutes + " phút để yêu cầu mã mới.");
            }
            // Kiểm tra cooldown (1 phút từ lần gửi cuối)
            LocalDateTime lastSent = existingOtp.getCreatedAt();
            if (lastSent.plusSeconds(OTP_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
                long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), lastSent.plusSeconds(OTP_COOLDOWN_SECONDS)).toSeconds();
                throw new RuntimeException("Vui lòng đợi " + remainingSeconds + " giây trước khi gửi lại mã OTP.");
            }
        }
        
        // Xóa tất cả OTP cũ của user (đã hết hạn)
        otpCodeRepository.deleteByUser(user);
        otpCodeRepository.flush();
        
        // Tạo OTP 6 chữ số
        String otp = generateOtp();
        
        // Hash OTP để lưu vào database
        String otpHash = passwordEncoder.encode(otp);
        
        // Tạo thời gian hết hạn (3 phút)
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        
        // Lưu OTP vào database
        OtpCode otpCode = OtpCode.builder()
            .user(user)
            .otpCodeHash(otpHash)
            .expiryDate(expiryDate)
            .build();
        
        otpCodeRepository.save(otpCode);
        
        // Gửi email
        emailService.sendOtpEmail(email, otp);
    }
    
    /**
     * Xác thực OTP và đặt lại mật khẩu
     */
    @Transactional
    public void verifyOtpAndResetPassword(String email, String otp, String newPassword) {
        // Tìm user theo email
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        // Tìm OTP mới nhất của user
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findLatestByUserEmail(email);
        
        if (otpCodeOpt.isEmpty()) {
            throw new RuntimeException("Không tìm thấy mã OTP. Vui lòng yêu cầu mã mới.");
        }
        
        OtpCode otpCode = otpCodeOpt.get();
        
        // Kiểm tra OTP đã hết hạn chưa
        if (otpCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }
        
        // Xác thực OTP
        if (!passwordEncoder.matches(otp, otpCode.getOtpCodeHash())) {
            throw new RuntimeException("Mã OTP không chính xác.");
        }
        
        // Đặt lại mật khẩu
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);
        
        // Xóa tất cả OTP của user sau khi đổi mật khẩu thành công để tránh tái sử dụng
        otpCodeRepository.deleteByUser(user);
        otpCodeRepository.flush();
    }
    
    /**
     * Cleanup OTP hết hạn - có thể chạy định kỳ
     */
    @Transactional
    public void cleanupExpiredOtps() {
        otpCodeRepository.deleteExpiredOtpCodes(LocalDateTime.now());
    }
    
    /**
     * Kiểm tra trạng thái OTP của user
     */
    public String checkOtpStatus(String email) {
        Optional<OtpCode> latestOtp = otpCodeRepository.findLatestByUserEmail(email);
        if (latestOtp.isEmpty()) {
            return "Không có mã OTP nào";
        }
        
        OtpCode otp = latestOtp.get();
        if (otp.getExpiryDate().isAfter(LocalDateTime.now())) {
            long remainingMinutes = java.time.Duration.between(LocalDateTime.now(), otp.getExpiryDate()).toMinutes();
            return "Có mã OTP còn hiệu lực, hết hạn sau " + remainingMinutes + " phút";
        } else {
            return "Mã OTP đã hết hạn";
        }
    }
    
    /**
     * Kiểm tra xem user có đăng ký bằng Google không
     * <p>Trả về true nếu user đăng ký bằng GOOGLE, false nếu LOCAL</p>
     */
    public boolean isGoogleProvider(String email) {
        User user = userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        return user.getProvider() == com.greenconnect.greenconnect_api.enums.Provider.GOOGLE;
    }
    
    /**
     * Xác thực OTP (không reset password, chỉ kiểm tra)
     * <p>Trả về true nếu OTP hợp lệ (đúng và chưa hết hạn), false nếu không hợp lệ</p>
     */
    public boolean verifyOtp(String email, String otp) {
        // Kiểm tra email có tồn tại không
        userRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new RuntimeException("Email không tồn tại trong hệ thống"));
        
        // Tìm OTP mới nhất của user
        Optional<OtpCode> otpCodeOpt = otpCodeRepository.findLatestByUserEmail(email);
        
        if (otpCodeOpt.isEmpty()) {
            return false; // Không có OTP nào
        }
        
        OtpCode otpCode = otpCodeOpt.get();
        
        // Kiểm tra OTP đã hết hạn chưa
        if (otpCode.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false; // OTP đã hết hạn
        }
        
        // Xác thực OTP
        return passwordEncoder.matches(otp, otpCode.getOtpCodeHash());
    }
    
    /**
     * Tạo OTP 6 chữ số ngẫu nhiên
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        StringBuilder otp = new StringBuilder();
        
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        
        return otp.toString();
    }
}