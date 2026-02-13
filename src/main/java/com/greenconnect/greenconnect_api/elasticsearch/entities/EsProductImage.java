package com.greenconnect.greenconnect_api.elasticsearch.entities;

// // === ELASTICSEARCH ANNOTATIONS (ĐÃ COMMENT OUT) ===
// import org.springframework.data.elasticsearch.annotations.Field;
// import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * EsProductImage - DTO (trước đây là Elasticsearch nested object)
 * 
 * ✅ GIỮ NGUYÊN STRUCTURE ĐỂ BACKWARD COMPATIBILITY
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter 
public class EsProductImage {
    
    private String id;
    
    private String productId;
    
    private String mediaType;
    
    private String mediaUrl;
    
    private Integer displayOrder;
    
    private Boolean isMain;
}