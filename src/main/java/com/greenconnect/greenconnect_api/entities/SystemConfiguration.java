package com.greenconnect.greenconnect_api.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "system_configurations") // Tên bảng trong Database
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Cột này để phân biệt (VD: "CHATBOT_PROMPT")
    @Column(name = "config_key", unique = true, nullable = false)
    private String configKey;

    // Cột này chứa nội dung dài (Prompt)
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    // Constant cho config key mặc định
    public static final String CHATBOT_PROMPT_KEY = "CHATBOT_PROMPT";
}
