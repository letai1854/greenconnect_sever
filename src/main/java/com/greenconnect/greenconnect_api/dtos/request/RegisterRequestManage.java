package com.greenconnect.greenconnect_api.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class RegisterRequestManage {
    
    /**
     * Activation token từ email (UUID)
     * Frontend gửi token từ URL: http://localhost:3000/manage/activate?token=e786fdd5-2663-41bd-afc1-8c64b638cb9f
     * Server sẽ hash token này để so sánh với tokenHash trong DB
     */
    @NotBlank(message = "Token không được để trống")
    String token;
    
    /**
     * Mật khẩu được user nhập (ít nhất 1 chữ hoa, 1 chữ thường, 1 số, tối thiểu 8 ký tự)
     */
    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$", 
             message = "Mật khẩu phải chứa ít nhất 1 chữ thường, 1 chữ hoa và 1 số")
    String password;
    
    /**
     * Xác nhận mật khẩu (phải khớp với password)
     */
    @NotBlank(message = "Nhập lại mật khẩu không được để trống")
    String confirmPassword;
}