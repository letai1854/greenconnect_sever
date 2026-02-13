package com.greenconnect.greenconnect_api.entities;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otp_codes", indexes = {
    @jakarta.persistence.Index(name = "idx_otp_user_id", columnList = "user_id"),
    @jakarta.persistence.Index(name = "idx_otp_expiry_date", columnList = "expiry_date"),
    @jakarta.persistence.Index(name = "idx_otp_user_expiry", columnList = "user_id, expiry_date"),
    // ✅ NEW: Cleanup expired OTPs
    @jakarta.persistence.Index(name = "idx_otp_expiry_created", columnList = "expiry_date, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(name = "otp_code_hash", nullable = false, length = 255)
    private String otpCodeHash;
    
    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;
    
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}