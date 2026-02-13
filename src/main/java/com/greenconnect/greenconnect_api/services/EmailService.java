package com.greenconnect.greenconnect_api.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    /**
     * Gửi email OTP đến người dùng
     */
    public void sendOtpEmail(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("greenconnect11@gmail.com");
        message.setTo(toEmail);
        message.setSubject("GreenConnect - Mã xác thực đặt lại mật khẩu");
        
        String emailContent = String.format(
            "Chào bạn,\n\n" +
            "Bạn đã yêu cầu đặt lại mật khẩu cho tài khoản GreenConnect của mình.\n\n" +
            "Mã xác thực OTP của bạn là: %s\n\n" +
            "Mã này có hiệu lực trong 3 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n" +
            "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
            "Trân trọng,\n" +
            "Đội ngũ GreenConnect",
            otp
        );
        
        message.setText(emailContent);
        
        try {
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    /**
     * Gửi email dạng plain text (tiện ích chung để gửi các loại email khác nhau).
     */
    public void sendPlainEmail(String toEmail, String subject, String body) {
        log.info("📧 [EmailService] START - Gửi email tới: {}", toEmail);
        log.info("📧 [EmailService] Subject: {}", subject);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("greenconnect11@gmail.com");
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            log.info("📧 [EmailService] mailSender object: {}", mailSender != null ? "NOT NULL" : "NULL");
            log.info("📧 [EmailService] Calling mailSender.send()...");
            mailSender.send(message);
            log.info("✅ [EmailService] SUCCESS - Email sent to: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ [EmailService] FAILED - Exception: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gửi email: " + e.getMessage());
        }
    }

    /**
     * Gửi email dạng plain text bất đồng bộ (không block luồng thực thi chính)
     */
    @Async
    public void sendPlainEmailAsync(String toEmail, String subject, String body) {
        sendPlainEmail(toEmail, subject, body);
    }
}