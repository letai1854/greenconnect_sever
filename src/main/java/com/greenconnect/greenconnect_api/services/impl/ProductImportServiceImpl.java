package com.greenconnect.greenconnect_api.services.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.greenconnect.greenconnect_api.dtos.internal.ProductImportDTO;
import com.greenconnect.greenconnect_api.dtos.internal.ProductImportDTO.VariantImportDTO;
import com.greenconnect.greenconnect_api.dtos.request.CreateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.ProductImportResultResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductImportResultResponse.ProductImportDetail;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.entities.Category;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.Supplier;
import com.greenconnect.greenconnect_api.repositories.CategoryRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.repositories.SupplierRepository;
import com.greenconnect.greenconnect_api.services.ProductImportService;
import com.greenconnect.greenconnect_api.services.ProductService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation để xử lý import sản phẩm từ Excel
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImportServiceImpl implements ProductImportService {
    
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY - Elasticsearch có thể không khả dụng
    private ElasticsearchSyncService elasticsearchSyncService;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    // Index của các cột trong Excel (0-based)
    private static final int COL_REF_ID = 0;           // A - Mã nhóm
    private static final int COL_NAME = 1;             // B - Tên sản phẩm
    private static final int COL_CATEGORY = 2;         // C - Danh mục
    private static final int COL_SUPPLIER = 3;         // D - Nhà cung cấp
    private static final int COL_DESCRIPTION = 4;      // E - Mô tả
    private static final int COL_SLUG = 5;             // F - Slug
    private static final int COL_IS_ACTIVE = 6;        // G - Hiển thị
    private static final int COL_IS_FEATURED = 7;      // H - Nổi bật
    private static final int COL_MAIN_IMAGE = 8;       // I - Link ảnh chính
    private static final int COL_ADDITIONAL_IMAGES = 9; // J - Link ảnh phụ
    private static final int COL_VARIANT_NAME = 10;    // K - Tên biến thể
    private static final int COL_SKU = 11;             // L - SKU
    private static final int COL_PRICE = 12;           // M - Giá gốc
    private static final int COL_DISCOUNT = 13;        // N - Giảm giá (%)
    private static final int COL_STOCK = 14;           // O - Tồn kho
    private static final int COL_UNIT = 15;            // P - Đơn vị tính
    private static final int COL_IS_DEFAULT = 16;      // Q - Là mặc định
    
    @Override
    // ✅ BỎ @Transactional - Mỗi product tự quản lý transaction riêng
    // Tránh lỗi: "Transaction silently rolled back because it has been marked as rollback-only"
    public ProductImportResultResponse importProductsFromExcel(MultipartFile file) {
        log.info("🚀 Bắt đầu import sản phẩm từ file Excel: {}", file.getOriginalFilename());
        
        // Bước 1: Parse Excel file
        List<ProductImportDTO> products;
        try {
            products = parseExcelFile(file);
            log.info("📊 Đã parse được {} sản phẩm từ Excel", products.size());
        } catch (Exception e) {
            log.error("❌ Lỗi khi parse file Excel: {}", e.getMessage(), e);
            return ProductImportResultResponse.builder()
                    .totalProcessed(0)
                    .successCount(0)
                    .failedCount(0)
                    .details(List.of(ProductImportDetail.builder()
                            .refId("N/A")
                            .productName("N/A")
                            .status("FAILED")
                            .errors(List.of("Lỗi đọc file Excel: " + e.getMessage()))
                            .build()))
                    .build();
        }
        
        // Bước 2: Validate và tạo sản phẩm
        List<ProductImportDetail> details = new ArrayList<>();
        List<UUID> successfulProductIds = new ArrayList<>(); // 🔄 Thu thập ID để sync Recombee batch
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;
        
        for (ProductImportDTO productDTO : products) {
            log.info("🔍 Xử lý sản phẩm: {} (RefId: {})", productDTO.getName(), productDTO.getRefId());
            
            try {
                // Validate format và business rules
                boolean isValid = validateProduct(productDTO);
                
                if (!isValid) {
                    // Có lỗi validation
                    log.warn("⚠️ Sản phẩm {} không hợp lệ: {}", productDTO.getRefId(), productDTO.getValidationErrors());
                    failedCount++;
                    details.add(ProductImportDetail.builder()
                            .refId(productDTO.getRefId())
                            .productName(productDTO.getName())
                            .status("FAILED")
                            .errors(productDTO.getValidationErrors())
                            .build());
                    continue;
                }
                
                // Kiểm tra sản phẩm đã tồn tại (theo tên)
                boolean productExists = productRepository.existsByNameIgnoreCase(productDTO.getName());
                if (productExists) {
                    log.warn("⚠️ Sản phẩm '{}' đã tồn tại trong hệ thống", productDTO.getName());
                    skippedCount++;
                    details.add(ProductImportDetail.builder()
                            .refId(productDTO.getRefId())
                            .productName(productDTO.getName())
                            .status("SKIPPED")
                            .message("Sản phẩm đã tồn tại trong hệ thống")
                            .errors(List.of("Tên sản phẩm '" + productDTO.getName() + "' đã được sử dụng"))
                            .build());
                    continue;
                }
                
                // Kiểm tra SKU trùng lặp với sản phẩm đã có trong database
                List<String> duplicatedSkus = new ArrayList<>();
                for (VariantImportDTO variant : productDTO.getVariants()) {
                    if (variant.getSku() != null && !variant.getSku().isEmpty()) {
                        if (productVariantRepository.existsBySku(variant.getSku())) {
                            duplicatedSkus.add(variant.getSku());
                        }
                    }
                }
                
                if (!duplicatedSkus.isEmpty()) {
                    log.warn("⚠️ SKU trùng lặp cho sản phẩm {}: {}", productDTO.getRefId(), duplicatedSkus);
                    skippedCount++;
                    details.add(ProductImportDetail.builder()
                            .refId(productDTO.getRefId())
                            .productName(productDTO.getName())
                            .status("SKIPPED")
                            .message("SKU đã tồn tại trong hệ thống")
                            .errors(List.of("Các SKU sau đã được sử dụng: " + String.join(", ", duplicatedSkus)))
                            .build());
                    continue;
                }
                
                // Hợp lệ và không trùng -> Tạo sản phẩm
                CreateProductRequest request = convertToCreateRequest(productDTO);
                ProductResponse response = productService.createProduct(request);
                
                log.info("✅ Đã tạo thành công sản phẩm: {} (ID: {})", response.getName(), response.getId());
                
                // 🔄 SYNC VÀO ELASTICSEARCH
                if (elasticsearchSyncService != null) {
                    try {
                        elasticsearchSyncService.syncProduct(response.getId());
                        log.info("✅ Đã sync product {} vào Elasticsearch", response.getId());
                    } catch (Exception esException) {
                        log.warn("⚠️ Lỗi sync Elasticsearch cho product {}: {}", 
                                response.getId(), esException.getMessage());
                        // Không fail toàn bộ import nếu chỉ Elasticsearch lỗi
                    }
                } else {
                    log.debug("ℹ️ Elasticsearch không khả dụng, bỏ qua sync cho product {}", response.getId());
                }
                
                successCount++;
                successfulProductIds.add(response.getId()); // 🔄 Lưu ID để sync Recombee
                details.add(ProductImportDetail.builder()
                        .refId(productDTO.getRefId())
                        .productName(productDTO.getName())
                        .status("SUCCESS")
                        .message("Đã tạo thành công sản phẩm với " + productDTO.getVariants().size() + " biến thể")
                        .build());
                        
            } catch (Exception e) {
                // Bắt mọi exception không mong đợi để tránh crash toàn bộ import
                log.error("❌ Lỗi không mong đợi khi xử lý sản phẩm {} ({}): {}", 
                        productDTO.getRefId(), productDTO.getName(), e.getMessage(), e);
                failedCount++;
                
                // Xác định lỗi chi tiết
                String errorMessage;
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                    errorMessage = "Dữ liệu bị trùng lặp trong database";
                } else if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                    errorMessage = "Vi phạm ràng buộc dữ liệu: " + e.getMessage();
                } else {
                    errorMessage = "Lỗi khi lưu vào database: " + (e.getMessage() != null ? e.getMessage() : "Lỗi không xác định");
                }
                
                details.add(ProductImportDetail.builder()
                        .refId(productDTO.getRefId())
                        .productName(productDTO.getName())
                        .status("FAILED")
                        .errors(List.of(errorMessage))
                        .build());
            }
        }
        
        log.info("📈 Kết quả import: Tổng={}, Thành công={}, Bỏ qua={}, Thất bại={}", 
                products.size(), successCount, skippedCount, failedCount);
        
        // Bước 3: Sync to Recombee (BATCH) - Chỉ sync những sản phẩm tạo thành công
        if (recombeeSyncService != null && !successfulProductIds.isEmpty()) {
            try {
                log.info("🔄 Bắt đầu sync {} sản phẩm vào Recombee (batch mode)...", successfulProductIds.size());
                List<Product> productsToSync = productRepository.findAllById(successfulProductIds);
                
                // ⚠️ QUAN TRỌNG: Force initialize lazy-loaded entities TRƯỚC KHI gọi async
                for (Product product : productsToSync) {
                    if (product.getCategory() != null) {
                        product.getCategory().getName(); // Trigger lazy loading
                    }
                    if (product.getSupplier() != null) {
                        product.getSupplier().getName(); // Trigger lazy loading
                    }
                    if (product.getProductVariants() != null) {
                        product.getProductVariants().size(); // Trigger lazy loading
                    }
                }
                
                recombeeSyncService.syncProductsBatch(productsToSync);
                log.info("✅ Hoàn tất sync Recombee batch cho {} sản phẩm", productsToSync.size());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi khi sync Recombee batch: {}", e.getMessage());
                // Không fail import nếu Recombee lỗi
            }
        } else if (recombeeSyncService == null) {
            log.debug("ℹ️ Recombee không khả dụng, bỏ qua batch sync");
        }
        
        return ProductImportResultResponse.builder()
                .totalProcessed(products.size())
                .successCount(successCount)
                .failedCount(failedCount + skippedCount) // Cộng cả skipped vào failed count
                .details(details)
                .build();
    }
    
    @Override
    public List<ProductImportDTO> parseExcelFile(MultipartFile file) {
        Map<String, ProductImportDTO> productMap = new LinkedHashMap<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0); // Sheet đầu tiên
            
            // ✅ CHỈ BỎ QUA HEADER (dòng 0), đọc tất cả từ dòng 1
            int startRow = findDataStartRow();
            log.info("📄 Bắt đầu đọc dữ liệu từ dòng {} (Header đã bỏ qua)", startRow + 1);
            
            for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isEmptyRow(row)) {
                    continue; // Bỏ qua dòng trống
                }
                
                final int rowIndex = i; // Make effectively final for lambda
                try {
                    processRow(row, i + 1, productMap);
                } catch (Exception e) {
                    log.error("❌ Lỗi khi xử lý dòng {}: {}", i + 1, e.getMessage());
                    // Tạo một sản phẩm lỗi để báo cáo
                    String refId = getCellValueAsString(row.getCell(COL_REF_ID));
                    ProductImportDTO errorProduct = productMap.computeIfAbsent(refId + "_ERROR_" + rowIndex, k -> 
                        ProductImportDTO.builder()
                            .refId(refId.isEmpty() ? "UNKNOWN_" + rowIndex : refId)
                            .name("Lỗi dòng " + (rowIndex + 1))
                            .validationErrors(new ArrayList<>())
                            .build()
                    );
                    errorProduct.getValidationErrors().add("Dòng " + (i + 1) + ": Lỗi đọc dữ liệu - " + e.getMessage());
                }
            }
            
        } catch (IOException e) {
            log.error("❌ Không thể đọc file Excel: {}", e.getMessage());
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage());
        }
        
        return new ArrayList<>(productMap.values());
    }
    
    /**
     * Tìm dòng bắt đầu chứa dữ liệu thực
     * ✅ CHỈ BỎ QUA HEADER (dòng 0)
     * ✅ ĐỌC TẤT CẢ dòng từ dòng 1 trở đi, BẤT CHẤP MÀU SẮC
     * ✅ Chỉ skip khi dòng trống hoặc là ghi chú (bắt đầu bằng ký tự đặc biệt)
     */
    private int findDataStartRow() {
        // ⚡ LUÔN BẮT ĐẦU TỪ DÒNG 1 (sau header)
        // Dòng 0 = Header -> đã tự động bỏ qua trong parseExcelFile
        // Từ dòng 1 trở đi = Dữ liệu thực, đọc tất cả
        return 1;
    }
    
    /**
     * Kiểm tra dòng có trống không
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i <= COL_IS_DEFAULT; i++) {
            Cell cell = row.getCell(i);
            if (cell != null && !getCellValueAsString(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Xử lý một dòng Excel
     * ✅ CHỈ BỎ QUA khi:
     *    1. Mã nhóm trống
     *    2. Mã nhóm bắt đầu bằng ký tự ghi chú (📝, •, #, //, --)
     * ✅ ĐỌC TẤT CẢ dòng khác, bất chấp màu sắc
     */
    private void processRow(Row row, int rowNumber, Map<String, ProductImportDTO> productMap) {
        String refId = getCellValueAsString(row.getCell(COL_REF_ID));
        
        // Bỏ qua dòng trống
        if (refId.isEmpty()) {
            log.debug("⏭️ Dòng {} bỏ qua: Mã nhóm trống", rowNumber);
            return;
        }
        
        // Bỏ qua dòng ghi chú (bắt đầu bằng ký tự đặc biệt)
        if (refId.startsWith("📝") || refId.startsWith("•") || 
            refId.startsWith("#") || refId.startsWith("//") || refId.startsWith("--")) {
            log.debug("⏭️ Dòng {} bỏ qua: Dòng ghi chú ({})", rowNumber, refId);
            return;
        }
        
        // Lấy hoặc tạo ProductImportDTO
        ProductImportDTO product = productMap.computeIfAbsent(refId, k -> {
            // Dòng đầu tiên của nhóm -> Lấy thông tin chung
            ProductImportDTO dto = ProductImportDTO.builder()
                    .refId(refId)
                    .name(getCellValueAsString(row.getCell(COL_NAME)))
                    .categoryName(getCellValueAsString(row.getCell(COL_CATEGORY)))
                    .supplierName(getCellValueAsString(row.getCell(COL_SUPPLIER)))
                    .description(getCellValueAsString(row.getCell(COL_DESCRIPTION)))
                    .slug(getCellValueAsString(row.getCell(COL_SLUG)))
                    .isActive(parseBooleanCell(row.getCell(COL_IS_ACTIVE), true))
                    .isFeatured(parseBooleanCell(row.getCell(COL_IS_FEATURED), false))
                    .mainImageUrl(getCellValueAsString(row.getCell(COL_MAIN_IMAGE)))
                    .additionalImagesUrls(getCellValueAsString(row.getCell(COL_ADDITIONAL_IMAGES)))
                    .variants(new ArrayList<>())
                    .validationErrors(new ArrayList<>())
                    .build();
            
            log.debug("🆕 Tạo sản phẩm mới: {} (RefId: {})", dto.getName(), refId);
            return dto;
        });
        
        // Thêm variant vào sản phẩm
        VariantImportDTO variant = VariantImportDTO.builder()
                .rowNumber(rowNumber)
                .name(getCellValueAsString(row.getCell(COL_VARIANT_NAME)))
                .sku(getCellValueAsString(row.getCell(COL_SKU)))
                .price(parseNumericCell(row.getCell(COL_PRICE)))
                .discountPercentage(parseNumericCell(row.getCell(COL_DISCOUNT)))
                .stockQuantity(parseIntCell(row.getCell(COL_STOCK)))
                .unit(getCellValueAsString(row.getCell(COL_UNIT)))
                .isDefault(parseBooleanCell(row.getCell(COL_IS_DEFAULT), false))
                .build();
        
        product.getVariants().add(variant);
        log.debug("   ➕ Thêm variant: {} (Dòng {})", variant.getName(), rowNumber);
    }
    
    /**
     * Đọc giá trị cell dưới dạng String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Nếu là số nguyên, không hiển thị phần thập phân
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    }
                    return String.valueOf(numericValue);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue()).toUpperCase();
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    return cell.getStringCellValue().trim();
                }
            default:
                return "";
        }
    }
    
    /**
     * Parse cell thành BigDecimal
     */
    private BigDecimal parseNumericCell(Cell cell) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return BigDecimal.ZERO;
                }
                return new BigDecimal(value);
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể parse số từ cell: {}", cell);
        }
        
        return BigDecimal.ZERO;
    }
    
    /**
     * Parse cell thành Integer
     */
    private Integer parseIntCell(Cell cell) {
        if (cell == null) {
            return 0;
        }
        
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return 0;
                }
                return Integer.parseInt(value);
            }
        } catch (Exception e) {
            log.warn("⚠️ Không thể parse số nguyên từ cell: {}", cell);
        }
        
        return 0;
    }
    
    /**
     * Parse cell thành Boolean
     */
    private Boolean parseBooleanCell(Cell cell, boolean defaultValue) {
        if (cell == null) {
            return defaultValue;
        }
        
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim().toUpperCase();
            return "TRUE".equals(value) || "YES".equals(value) || "1".equals(value);
        }
        
        return defaultValue;
    }
    
    @Override
    public boolean validateProduct(ProductImportDTO productDTO) {
        List<String> errors = productDTO.getValidationErrors();
        boolean isValid = true;
        
        // ===== 1. VALIDATE FORMAT =====
        
        // Tên sản phẩm bắt buộc
        if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
            errors.add("Tên sản phẩm không được để trống");
            isValid = false;
        }
        
        // Danh mục bắt buộc
        if (productDTO.getCategoryName() == null || productDTO.getCategoryName().trim().isEmpty()) {
            errors.add("Danh mục không được để trống");
            isValid = false;
        }
        
        // Nhà cung cấp bắt buộc
        if (productDTO.getSupplierName() == null || productDTO.getSupplierName().trim().isEmpty()) {
            errors.add("Nhà cung cấp không được để trống");
            isValid = false;
        }
        
        // Phải có ít nhất 1 variant
        if (productDTO.getVariants().isEmpty()) {
            errors.add("Sản phẩm phải có ít nhất 1 biến thể");
            isValid = false;
        }
        
        // ===== 2. VALIDATE REFERENCE (Tìm Category và Supplier) =====
        
        if (productDTO.getCategoryName() != null && !productDTO.getCategoryName().isEmpty()) {
            Optional<Category> categoryOpt = categoryRepository.findByName(productDTO.getCategoryName());
            if (categoryOpt.isPresent()) {
                productDTO.setCategoryId(categoryOpt.get().getId());
            } else {
                errors.add("Danh mục '" + productDTO.getCategoryName() + "' không tồn tại trong hệ thống");
                isValid = false;
            }
        }
        
        if (productDTO.getSupplierName() != null && !productDTO.getSupplierName().isEmpty()) {
            Optional<Supplier> supplierOpt = supplierRepository.findByName(productDTO.getSupplierName());
            if (supplierOpt.isPresent()) {
                productDTO.setSupplierId(supplierOpt.get().getId());
            } else {
                errors.add("Nhà cung cấp '" + productDTO.getSupplierName() + "' không tồn tại trong hệ thống");
                isValid = false;
            }
        }
        
        // ===== 3. VALIDATE VARIANTS (Business Logic) =====
        
        int defaultCount = 0;
        Set<String> skus = new HashSet<>();
        
        for (VariantImportDTO variant : productDTO.getVariants()) {
            String variantPrefix = "Dòng " + variant.getRowNumber() + " (" + variant.getName() + ")";
            
            // Tên variant bắt buộc
            if (variant.getName() == null || variant.getName().trim().isEmpty()) {
                errors.add(variantPrefix + ": Tên biến thể không được để trống");
                isValid = false;
            }
            
            // Giá phải > 0
            if (variant.getPrice() == null || variant.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                errors.add(variantPrefix + ": Giá phải lớn hơn 0");
                isValid = false;
            }
            
            // Discount phải từ 0-100
            if (variant.getDiscountPercentage() != null) {
                if (variant.getDiscountPercentage().compareTo(BigDecimal.ZERO) < 0 || 
                    variant.getDiscountPercentage().compareTo(new BigDecimal("100")) > 0) {
                    errors.add(variantPrefix + ": Phần trăm giảm giá phải từ 0-100");
                    isValid = false;
                }
            }
            
            // Tồn kho không được âm
            if (variant.getStockQuantity() == null || variant.getStockQuantity() < 0) {
                errors.add(variantPrefix + ": Số lượng tồn kho không được âm");
                isValid = false;
            }
            
            // Đơn vị tính bắt buộc
            if (variant.getUnit() == null || variant.getUnit().trim().isEmpty()) {
                errors.add(variantPrefix + ": Đơn vị tính không được để trống");
                isValid = false;
            }
            
            // Đếm số variant mặc định
            if (variant.getIsDefault() != null && variant.getIsDefault()) {
                defaultCount++;
            }
            
            // Kiểm tra SKU trùng trong cùng sản phẩm
            if (variant.getSku() != null && !variant.getSku().isEmpty()) {
                if (!skus.add(variant.getSku())) {
                    errors.add(variantPrefix + ": SKU '" + variant.getSku() + "' bị trùng trong cùng sản phẩm");
                    isValid = false;
                }
            }
        }
        
        // Phải có đúng 1 variant mặc định
        if (defaultCount == 0) {
            errors.add("Sản phẩm phải có ít nhất 1 biến thể mặc định (cột Q = TRUE)");
            isValid = false;
        } else if (defaultCount > 1) {
            errors.add("Sản phẩm chỉ được có 1 biến thể mặc định (có " + defaultCount + " biến thể được chọn là mặc định)");
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * Chuyển đổi ProductImportDTO thành CreateProductRequest
     */
    private CreateProductRequest convertToCreateRequest(ProductImportDTO dto) {
        // ⭐ LOG: Kiểm tra mainImageUrl từ DTO
        log.info("🖼️ Converting DTO to CreateRequest - mainImageUrl: {}", dto.getMainImageUrl());
        log.info("   additionalImagesUrls: {}", dto.getAdditionalImagesUrls());
        
        // Xử lý images
        List<CreateProductRequest.CreateProductImageRequest> images = new ArrayList<>();
        
        // ✅ BỔ SUNG: Thêm mainImageUrl vào đầu danh sách images nếu tồn tại
        int displayOrder = 0;
        if (dto.getMainImageUrl() != null && !dto.getMainImageUrl().trim().isEmpty()) {
            images.add(CreateProductRequest.CreateProductImageRequest.builder()
                    .mediaType("IMAGE")
                    .mediaUrl(dto.getMainImageUrl().trim())
                    .displayOrder(displayOrder++)
                    .build());
            log.info("   ✅ Added mainImageUrl to images list: {}", dto.getMainImageUrl());
        }
        
        // Thêm ảnh phụ
        if (dto.getAdditionalImagesUrls() != null && !dto.getAdditionalImagesUrls().isEmpty()) {
            String[] urls = dto.getAdditionalImagesUrls().split(",");
            for (String url : urls) {
                url = url.trim();
                if (!url.isEmpty()) {
                    images.add(CreateProductRequest.CreateProductImageRequest.builder()
                            .mediaType("IMAGE")
                            .mediaUrl(url)
                            .displayOrder(displayOrder++)
                            .build());
                }
            }
            log.info("   ✅ Added {} additional images", urls.length);
        }
        
        log.info("   📊 Total images to create: {}", images.size());
        
        // Chuyển đổi variants
        List<CreateProductRequest.CreateProductVariantRequest> variants = dto.getVariants().stream()
                .map(v -> CreateProductRequest.CreateProductVariantRequest.builder()
                        .name(v.getName())
                        .sku(v.getSku())
                        .price(v.getPrice())
                        .discountPercentage(v.getDiscountPercentage())
                        .stockQuantity(v.getStockQuantity())
                        .unit(v.getUnit())
                        .isActive(true)
                        .isDefault(v.getIsDefault())
                        .build())
                .collect(Collectors.toList());
        
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(dto.getCategoryId())
                .supplierId(dto.getSupplierId())
                .name(dto.getName())
                .description(dto.getDescription())
                .slug(dto.getSlug())
                .isActive(dto.getIsActive())
                .isFeatured(dto.getIsFeatured())
                .mainImageUrl(dto.getMainImageUrl()) // ⭐ Truyền mainImageUrl để đánh dấu ảnh chính
                .images(images)
                .variants(variants)
                .build();
        
        log.info("   ✅ CreateProductRequest built - mainImageUrl: {}, images count: {}", 
                request.getMainImageUrl(), request.getImages() != null ? request.getImages().size() : 0);
        
        return request;
    }
}
