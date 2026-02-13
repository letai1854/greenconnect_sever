package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Firebase Registration Request DTO
 * <p>Chứa thông tin thiết yếu để đăng ký/đăng nhập qua Firebase (Google)</p>
 * <p>Các thông tin khác sẽ được set mặc định trong code:</p>
 * <ul>
 *   <li>provider = GOOGLE (luôn)</li>
 *   <li>status = ACTIVE (mặc định)</li>
 *   <li>role = CUSTOMER (mặc định)</li>
 *   <li>userCode = auto-generated (sequential 100000+)</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterFirebaseRequest {
    
    /**
     * Email từ Firebase
     */
    @Email(message = "Email không đúng định dạng")
    @NotBlank(message = "Email không được để trống")
    String email; 
    
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
