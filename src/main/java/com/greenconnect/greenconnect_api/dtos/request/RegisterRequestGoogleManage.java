package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequestGoogleManage {
    
    /**
     * Activation token từ email (UUID)
     * Frontend gửi token từ URL: http://localhost:3000/manage/activate?token=e786fdd5-2663-41bd-afc1-8c64b638cb9f
     * Server sẽ hash token này để so sánh với tokenHash trong DB
     */
    @NotBlank(message = "Token không được để trống")
    String token;
    
    /**
     * Tên người dùng từ Firebase profile
     */
    @Size(min = 2, max = 255, message = "Tên người dùng phải từ 2-255 ký tự")
    String fullName; 
    
    /**
     * Avatar URL từ Firebase
     */
    String photoURL; 
    
    /**
     * Firebase User ID hoặc Google User ID (dùng để identify user)
     */
    @NotBlank(message = "Provider ID không được để trống") 
    String providerId;

}
