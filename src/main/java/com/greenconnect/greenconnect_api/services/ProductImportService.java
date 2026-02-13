package com.greenconnect.greenconnect_api.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.greenconnect.greenconnect_api.dtos.internal.ProductImportDTO;
import com.greenconnect.greenconnect_api.dtos.response.ProductImportResultResponse;

/**
 * Service interface để xử lý import sản phẩm từ file Excel
 */
public interface ProductImportService {
    
    /**
     * Import sản phẩm từ file Excel
     * 
     * @param file File Excel chứa dữ liệu sản phẩm
     * @return Kết quả import (thành công/thất bại cho từng sản phẩm)
     */
    ProductImportResultResponse importProductsFromExcel(MultipartFile file);
    
    /**
     * Parse file Excel thành danh sách ProductImportDTO
     * (Gom nhóm các dòng theo RefId)
     * 
     * @param file File Excel
     * @return Danh sách sản phẩm đã được gom nhóm
     */
    List<ProductImportDTO> parseExcelFile(MultipartFile file);
    
    /**
     * Validate một sản phẩm import (kiểm tra format, reference, business logic)
     * 
     * @param productDTO Sản phẩm cần validate
     * @return true nếu hợp lệ, false nếu có lỗi (lỗi sẽ được thêm vào productDTO.validationErrors)
     */
    boolean validateProduct(ProductImportDTO productDTO);
}
