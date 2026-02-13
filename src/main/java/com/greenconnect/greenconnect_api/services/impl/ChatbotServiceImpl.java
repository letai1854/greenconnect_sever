package com.greenconnect.greenconnect_api.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greenconnect.greenconnect_api.dtos.ProductAiDto;
import com.greenconnect.greenconnect_api.dtos.request.ChatRequest;
import com.greenconnect.greenconnect_api.dtos.response.ChatHistoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatResponse;
import com.greenconnect.greenconnect_api.dtos.response.ChatSessionListResponse;
import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSearchService;
import com.greenconnect.greenconnect_api.entities.ChatMessage;
import com.greenconnect.greenconnect_api.entities.ChatSession;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.ChatMessageRepository;
import com.greenconnect.greenconnect_api.repositories.ChatSessionRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.services.ChatbotService;
import com.greenconnect.greenconnect_api.services.FooterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    private final ProductRepository productRepository;
    private final ElasticsearchSearchService elasticsearchSearchService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final com.greenconnect.greenconnect_api.services.ProductService productService;
    private final FooterService footerService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Override
    public ChatResponse processChat(ChatRequest request, UUID userId) {
        log.info("🤖 Processing chat request: {} (user: {})", request.getUserQuery(), userId);

        // 🟢 1. TÌM HOẶC TẠO SESSION (E-COMMERCE CHATBOT: 1 USER = 1 SESSION)
        // Backend tự động tìm session active của user
        List<ChatSession> activeSessions = chatSessionRepository
            .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        
        ChatSession session;
        if (!activeSessions.isEmpty()) {
            // Đã có session active → dùng luôn
            session = activeSessions.get(0);
            log.info("📂 Found existing active session for user: {}", session.getId());
        } else {
            // Chưa có session → tạo mới
            session = ChatSession.builder()
                .userId(userId)
                .title("Chat " + new java.text.SimpleDateFormat("dd/MM HH:mm").format(new Date()))
                .isActive(true)
                .build();
            session = chatSessionRepository.save(session);
            log.info("✨ Created new session for user: {}", session.getId());
        }

        // 🟢 2. LƯU TIN NHẮN USER VÀO DB
        ChatMessage userMessage = ChatMessage.builder()
            .session(session)
            .sender(ChatMessage.MessageSender.USER)
            .content(request.getUserQuery())
            .referenceProductIds(null) // User message không có product reference
            .build();
        chatMessageRepository.save(userMessage);
        log.info("💬 Saved user message to DB");

        // 🟢 3. LẤY 10 TIN NHẮN GẦN NHẤT (để làm context cho Gemini)
        List<ChatMessage> recentMessages = chatMessageRepository
            .findBySession_IdOrderByCreatedAtDesc(
                session.getId(), 
                PageRequest.of(0, 10) // Lấy 10 tin mới nhất
            );
        Collections.reverse(recentMessages); // Đảo ngược để đúng thứ tự thời gian (cũ → mới)
        log.info("📜 Loaded {} recent messages from DB", recentMessages.size());

        // 🟢 4. LẤY DANH SÁCH PRODUCT IDs TỪ LỊCH SỬ
        Set<UUID> includeProductIds = extractProductIdsFromHistory(recentMessages);
        log.info("🔗 Extracted {} product IDs from history", includeProductIds.size());

        // 🔥 5. PHÁT HIỆN INTENT (special queries về metadata)
        String queryLower = request.getUserQuery().toLowerCase();
        boolean isSpecialIntent = detectSpecialIntent(queryLower);
        boolean isRecipeIntent = detectRecipeIntent(queryLower);
        
        List<UUID> allProductIds = new ArrayList<>(includeProductIds);
        
        if (isRecipeIntent) {
            // 🍳 RECIPE INTENT (nấu ăn, công thức): Cần nguyên liệu đa dạng từ nhiều category
            log.info("🍳 Recipe intent detected → Diverse ingredients strategy");
            
            // 1. Search ES với query gốc (tìm nguyên liệu chính - 10 sản phẩm)
            Page<EsProduct> mainResults = elasticsearchSearchService.searchProducts(
                request.getUserQuery(), 0, 10
            );
            List<UUID> mainProductIds = mainResults.getContent().stream()
                .map(esProduct -> {
                    try {
                        return UUID.fromString(esProduct.getId());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            allProductIds.addAll(mainProductIds);
            log.info("✅ Added {} main ingredients from ES", mainProductIds.size());
            
            // 2. Extract và search các nguyên liệu phụ từ query
            List<String> ingredientKeywords = extractIngredientKeywords(queryLower);
            for (String ingredient : ingredientKeywords) {
                Page<EsProduct> ingredientResults = elasticsearchSearchService.searchProducts(
                    ingredient, 0, 3
                );
                List<UUID> ingredientIds = ingredientResults.getContent().stream()
                    .map(esProduct -> {
                        try {
                            return UUID.fromString(esProduct.getId());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                allProductIds.addAll(ingredientIds);
                log.info("✅ Added {} products for ingredient '{}'", ingredientIds.size(), ingredient);
            }
            
            // 3. Bổ sung sản phẩm đa dạng từ các category phổ biến (rau, gia vị, thịt...)
            List<UUID> diverseIds = getDiverseIngredientsForRecipe(queryLower, 15);
            allProductIds.addAll(diverseIds);
            log.info("✅ Added {} diverse ingredients", diverseIds.size());
            
        } else if (isSpecialIntent) {
            // 🎯 SPECIAL INTENT (mới/hot/sale/nổi bật): Ưu tiên ProductService 70%, ES 30%
            log.info("🎯 Special intent detected → ProductService 70%, ES 30%");
            
            // 70% từ ProductService (21 sản phẩm theo intent)
            List<UUID> intentBasedIds = getFallbackProducts(request.getUserQuery(), 21);
            allProductIds.addAll(intentBasedIds);
            log.info("✅ Added {} intent-based products (70%)", intentBasedIds.size());
            
            // 30% từ Elasticsearch (9 sản phẩm text matching)
            Page<EsProduct> searchResults = elasticsearchSearchService.searchProducts(
                request.getUserQuery(), 0, 9
            );
            List<UUID> esProductIds = searchResults.getContent().stream()
                .map(esProduct -> {
                    try {
                        return UUID.fromString(esProduct.getId());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID from ES: {}", esProduct.getId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            allProductIds.addAll(esProductIds);
            log.info("✅ Added {} ES products (30%)", esProductIds.size());
            
        } else {
            // 🔍 GENERAL QUERY (tên sản phẩm, category): Ưu tiên ES 70%, intent 30%
            log.info("🔍 General query → ES 70%, ProductService 30%");
            
            // 70% từ Elasticsearch (21 sản phẩm text matching)
            Page<EsProduct> searchResults = elasticsearchSearchService.searchProducts(
                request.getUserQuery(), 0, 21
            );
            List<UUID> esProductIds = searchResults.getContent().stream()
                .map(esProduct -> {
                    try {
                        return UUID.fromString(esProduct.getId());
                    } catch (IllegalArgumentException e) {
                        log.warn("Invalid UUID from ES: {}", esProduct.getId());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            allProductIds.addAll(esProductIds);
            log.info("✅ Added {} ES products (70%)", esProductIds.size());
            
            // 30% từ ProductService (9 sản phẩm theo context)
            List<UUID> intentBasedIds = getFallbackProducts(request.getUserQuery(), 9);
            allProductIds.addAll(intentBasedIds);
            log.info("✅ Added {} intent-based products (30%)", intentBasedIds.size());
            
            // Nếu ES trả ít kết quả → bổ sung thêm
            if (esProductIds.size() < 10) {
                log.info("⚠️ ES returned only {} results, adding more fallback", esProductIds.size());
                int needed = 30 - allProductIds.size();
                if (needed > 0) {
                    List<UUID> additionalIds = getFallbackProducts(request.getUserQuery(), needed);
                    allProductIds.addAll(additionalIds);
                    log.info("✅ Added {} additional products", additionalIds.size());
                }
            }
        }
        
        // 9. Distinct để tránh trùng
        allProductIds = allProductIds.stream().distinct().collect(Collectors.toList());
        log.info("🔀 After processing: {} unique products", allProductIds.size());
        
        // 🔥 TỐI ƯU: Giới hạn tối đa 30 sản phẩm (design limit)
        if (allProductIds.size() > 30) {
            allProductIds = allProductIds.subList(0, 30);
            log.info("⚠️ Truncated to 30 products (design limit)");
        }
        
        // 10. Fetch thông tin đầy đủ từ MySQL
        List<Product> products = productRepository.findAllById(allProductIds);
        
        log.info("📦 Final: {} products for AI context", products.size());

        // 11. Convert sang ProductAiDto (JSON đơn giản cho Gemini)
        List<ProductAiDto> productContext = products.stream()
                .map(this::convertToAiDto)
                .collect(Collectors.toList());

        // 🟢 12. GỌI GEMINI API VỚI LỊCH SỬ 10 TIN NHẮN
        String geminiResponse = callGeminiApiWithHistory(request, productContext, recentMessages);
        log.info("🧠 Gemini response received");

        // 13. Parse JSON response từ Gemini
        GeminiAiResponse aiResponse = parseGeminiResponse(geminiResponse);

        // 14. Lấy thông tin đầy đủ các sản phẩm được gợi ý
        List<ChatResponse.ProductSuggestion> suggestedProducts = new ArrayList<>();
        if (aiResponse.getSuggestedProductIds() != null && !aiResponse.getSuggestedProductIds().isEmpty()) {
            suggestedProducts = fetchProductDetails(aiResponse.getSuggestedProductIds());
        }

        // 🟢 15. LƯU TIN NHẮN BOT VÀO DB (kèm product IDs)
        String productIdsString = aiResponse.getSuggestedProductIds() != null 
            ? aiResponse.getSuggestedProductIds().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(","))
            : null;
        
        ChatMessage botMessage = ChatMessage.builder()
            .session(session)
            .sender(ChatMessage.MessageSender.BOT)
            .content(aiResponse.getReplyText())
            .referenceProductIds(productIdsString)
            .build();
        chatMessageRepository.save(botMessage);
        log.info("🤖 Saved bot message to DB (with {} product refs)", 
            aiResponse.getSuggestedProductIds() != null ? aiResponse.getSuggestedProductIds().size() : 0);

        // 16. Trả về response cho frontend
        return ChatResponse.builder()
                .sessionId(session.getId()) // 🔥 GỬI sessionId về frontend
                .botMessage(aiResponse.getReplyText())
                .products(suggestedProducts)
                .build();
    }

    /**
     * Convert Product entity sang ProductAiDto để làm context cho AI
     */
    private ProductAiDto convertToAiDto(Product product) {
        // Tính price range từ variants
        String priceRange = calculatePriceRange(product.getProductVariants());

        // Lấy category name
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "Khác";
        
        // 🔥 Cắt ngắn description (tối đa 300 ký tự) để tránh tràn token
        String shortDescription = product.getDescription();
        if (shortDescription != null && shortDescription.length() > 300) {
            shortDescription = shortDescription.substring(0, 300) + "...";
        }
        
        // 🔥 Tính tồn kho từ variants và lấy unit
        int totalStock = 0;
        String unit = null;
        if (product.getProductVariants() != null && !product.getProductVariants().isEmpty()) {
            totalStock = product.getProductVariants().stream()
                .mapToInt(variant -> variant.getStockQuantity() != null ? variant.getStockQuantity() : 0)
                .sum();
            
            // Lấy unit từ tất cả variants (xử lý trường hợp variants có unit khác nhau)
            Set<String> units = product.getProductVariants().stream()
                .map(ProductVariant::getUnit)
                .filter(u -> u != null && !u.isEmpty())
                .collect(Collectors.toSet());
            unit = units.isEmpty() ? null : String.join("/", units);
        }
        
        // 🔥 Xác định stock status
        String stockStatus;
        if (totalStock == 0) {
            stockStatus = "OUT_OF_STOCK";
        } else if (totalStock <= 10) {
            stockStatus = "LOW_STOCK";
        } else {
            stockStatus = "IN_STOCK";
        }
        
        // 🔥 Parse attributes từ description (nếu có)
        // Patterns: "Xuất xứ: Việt Nam", "Nguồn gốc: Việt Nam", "Trọng lượng: 500g", "HSD: 6 tháng"
        // ⚠️ Note: Đây là giải pháp tạm thời cho đồ án. Hướng phát triển nên tách ra Attribute Table riêng.
        String origin = extractAttribute(shortDescription, "(xuất xứ|nguồn gốc)[:\\s]+([^\\n\\.,;]+)");
        String weight = extractAttribute(shortDescription, "trọng lượng[:\\s]+([^\\n\\.,;]+)");
        String expiryInfo = extractAttribute(shortDescription, "h[sđ]d?[:\\s]+([^\\n\\.,;]+)");
        
        // 🔥 Format createdAt (để AI biết sản phẩm mới)
        String createdAtStr = null;
        if (product.getCreatedAt() != null) {
            createdAtStr = new java.text.SimpleDateFormat("yyyy-MM-dd").format(
                java.sql.Timestamp.valueOf(product.getCreatedAt())
            );
        }
        
        // 🔥 Check if product is on flash sale (via promotionProducts)
        Boolean isFlashSale = false;
        if (product.getPromotionProducts() != null) {
            isFlashSale = product.getPromotionProducts().stream()
                .anyMatch(pp -> pp.getCampaign() != null 
                    && pp.getCampaign().getCampaignType() != null
                    && "FLASH_SALE".equals(pp.getCampaign().getCampaignType().name())
                    && pp.getCampaign().getIsActive());
        }
        
        // 🔥 Convert averageRating from BigDecimal to Double
        Double avgRating = product.getAverageRating() != null 
            ? product.getAverageRating().doubleValue() 
            : null;
        
        // 🔥 Convert sellNumber from Long to Integer (soldCount)
        Integer soldCount = product.getSellNumber() != null 
            ? product.getSellNumber().intValue() 
            : 0;

        return ProductAiDto.builder()
                .id(product.getId())
                .name(product.getName())
                .category(categoryName)
                .price(priceRange)
                .description(shortDescription)  // 🔥 Dùng description đã cắt ngắn
                .stockStatus(stockStatus)
                .totalStock(totalStock)
                .unit(unit)
                .origin(origin)
                .weight(weight)
                .expiryInfo(expiryInfo)
                // 🔥 METADATA cho AI
                .createdAt(createdAtStr)
                .isFeatured(product.getIsFeatured())
                .isFlashSale(isFlashSale)
                .averageRating(avgRating)
                .soldCount(soldCount)
                .build();
    }
    
    /**
     * Extract attribute từ description bằng regex
     * Ví dụ: "Xuất xứ: Việt Nam" → "Việt Nam"
     * ⚠️ Giải pháp tạm thời cho đồ án - Hướng phát triển nên dùng Attribute Table
     */
    private String extractAttribute(String description, String pattern) {
        if (description == null || description.isEmpty()) {
            return null;
        }
        
        try {
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(description);
            if (m.find()) {
                // Lấy group cuối cùng (vì pattern có thể có nhiều capturing groups)
                int groupCount = m.groupCount();
                return m.group(groupCount).trim();
            }
        } catch (Exception e) {
            log.debug("Failed to extract attribute with pattern: {}", pattern);
        }
        
        return null;
    }
    
    /**
     * Extract Product IDs từ lịch sử chat (để làm context cho AI)
     * Tách riêng thành method để dễ bảo trì và mở rộng
     */
    private Set<UUID> extractProductIdsFromHistory(List<ChatMessage> messages) {
        Set<UUID> productIds = new HashSet<>();
        
        for (ChatMessage msg : messages) {
            if (msg.getSender() == ChatMessage.MessageSender.BOT && msg.getReferenceProductIds() != null) {
                try {
                    // referenceProductIds format: "uuid1,uuid2,uuid3"
                    String[] idStrings = msg.getReferenceProductIds().split(",");
                    for (String idStr : idStrings) {
                        productIds.add(UUID.fromString(idStr.trim()));
                    }
                } catch (Exception e) {
                    log.warn("⚠️ Failed to parse referenceProductIds: {}", msg.getReferenceProductIds());
                }
            }
        }
        
        return productIds;
    }

    /**
     * 🍳 Phát hiện Recipe Intent - User hỏi về nấu ăn/công thức
     * Cần xử lý khác vì cần nhiều loại nguyên liệu đa dạng
     */
    private boolean detectRecipeIntent(String queryLower) {
        String[] recipeKeywords = {
            "nấu", "làm món", "chế biến", "công thức", "recipe",
            "làm sao để nấu", "cách làm", "cách nấu",
            "muốn nấu", "muốn làm", "thử làm",
            "món ăn", "bữa ăn", "bữa tối", "bữa sáng", "bữa trưa",
            "nguyên liệu", "ingredients"
        };
        
        for (String keyword : recipeKeywords) {
            if (queryLower.contains(keyword)) {
                log.info("🍳 Recipe intent keyword detected: '{}'", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 🥬 Extract các từ khóa nguyên liệu từ query
     * Dựa trên mapping món ăn → nguyên liệu phổ biến
     */
    private List<String> extractIngredientKeywords(String queryLower) {
        List<String> ingredients = new ArrayList<>();
        
        // Mapping món ăn phổ biến → nguyên liệu cần thiết
        Map<String, List<String>> recipeIngredients = new HashMap<>();
        
        // Canh chua
        recipeIngredients.put("canh chua", Arrays.asList("cá", "cà chua", "thơm", "dứa", "me", "giá đỗ", "rau muống", "ớt", "hành", "ngò"));
        recipeIngredients.put("canh", Arrays.asList("rau", "thịt", "xương", "hành", "ngò", "ớt"));
        
        // Phở
        recipeIngredients.put("phở", Arrays.asList("bánh phở", "thịt bò", "xương", "hành", "ngò", "giá đỗ", "rau thơm", "ớt", "chanh"));
        
        // Bún
        recipeIngredients.put("bún", Arrays.asList("bún", "thịt", "rau sống", "giá đỗ", "hành", "ớt", "nước mắm"));
        recipeIngredients.put("bún riêu", Arrays.asList("bún", "cua", "cà chua", "đậu hũ", "rau muống", "hành", "ớt"));
        recipeIngredients.put("bún bò", Arrays.asList("bún", "thịt bò", "xả", "ớt", "rau sống", "giá đỗ"));
        
        // Cơm
        recipeIngredients.put("cơm", Arrays.asList("gạo", "thịt", "rau", "trứng"));
        recipeIngredients.put("cơm chiên", Arrays.asList("cơm", "trứng", "hành", "tỏi", "đậu hà lan", "cà rốt"));
        
        // Salad
        recipeIngredients.put("salad", Arrays.asList("rau xà lách", "cà chua", "dưa leo", "hành tây", "trứng"));
        recipeIngredients.put("gỏi", Arrays.asList("rau", "thịt", "tôm", "đậu phộng", "hành", "ớt", "chanh"));
        
        // Lẩu
        recipeIngredients.put("lẩu", Arrays.asList("thịt", "rau", "nấm", "đậu hũ", "bún", "mì"));
        recipeIngredients.put("lẩu thái", Arrays.asList("tôm", "mực", "nấm", "rau muống", "cà chua", "ớt", "xả"));
        
        // Xào
        recipeIngredients.put("xào", Arrays.asList("rau", "thịt", "tỏi", "hành", "dầu ăn"));
        
        // Kho
        recipeIngredients.put("kho", Arrays.asList("thịt", "cá", "nước mắm", "đường", "tỏi", "ớt"));
        
        // Chiên/Rán
        recipeIngredients.put("chiên", Arrays.asList("thịt", "cá", "bột", "trứng", "dầu ăn"));
        recipeIngredients.put("rán", Arrays.asList("thịt", "cá", "bột", "trứng", "dầu ăn"));
        
        // Hấp
        recipeIngredients.put("hấp", Arrays.asList("cá", "tôm", "gừng", "hành", "xì dầu"));
        
        // Nướng
        recipeIngredients.put("nướng", Arrays.asList("thịt", "rau", "gia vị", "dầu"));
        
        // Tìm món ăn trong query và lấy nguyên liệu tương ứng
        for (Map.Entry<String, List<String>> entry : recipeIngredients.entrySet()) {
            if (queryLower.contains(entry.getKey())) {
                ingredients.addAll(entry.getValue());
                log.info("🥘 Found recipe '{}' → ingredients: {}", entry.getKey(), entry.getValue());
            }
        }
        
        // Loại bỏ trùng lặp
        return ingredients.stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * 🥗 Lấy sản phẩm đa dạng từ nhiều category cho công thức nấu ăn
     * Đảm bảo có: rau củ, thịt/cá, gia vị
     */
    private List<UUID> getDiverseIngredientsForRecipe(String queryLower, int limit) {
        List<UUID> diverseProducts = new ArrayList<>();
        
        try {
            // Các category search keywords để đảm bảo đa dạng nguyên liệu
            List<String> categoryKeywords = Arrays.asList(
                "rau",      // Rau củ quả
                "gia vị",   // Gia vị
                "thịt",     // Thịt
                "hải sản",  // Hải sản
                "trứng",    // Trứng
                "nấm"       // Nấm
            );
            
            int perCategory = Math.max(2, limit / categoryKeywords.size());
            
            for (String keyword : categoryKeywords) {
                try {
                    Page<EsProduct> results = elasticsearchSearchService.searchProducts(keyword, 0, perCategory);
                    List<UUID> categoryIds = results.getContent().stream()
                        .map(esProduct -> {
                            try {
                                return UUID.fromString(esProduct.getId());
                            } catch (IllegalArgumentException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    diverseProducts.addAll(categoryIds);
                    log.debug("🥬 Added {} products for category keyword '{}'", categoryIds.size(), keyword);
                } catch (Exception e) {
                    log.warn("⚠️ Failed to search for category '{}': {}", keyword, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("❌ Error getting diverse ingredients: {}", e.getMessage());
        }
        
        return diverseProducts.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Tính price range từ danh sách variants (sau khi trừ discount)
     * ✅ Ưu tiên getDiscountedPrice() nếu có discount
     */
    private String calculatePriceRange(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return "Liên hệ";
        }

        // 🔥 Tính giá THỰC TẾ (sau khi trừ discount)
        BigDecimal minPrice = variants.stream()
                .map(v -> v.hasDiscount() ? v.getDiscountedPrice() : v.getPrice())
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = variants.stream()
                .map(v -> v.hasDiscount() ? v.getDiscountedPrice() : v.getPrice())
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        if (minPrice.compareTo(maxPrice) == 0) {
            return String.format("%,.0fđ", minPrice);
        } else {
            return String.format("%,.0fđ - %,.0fđ", minPrice, maxPrice);
        }
    }

    /**
     * Phát hiện special intent (queries yêu cầu metadata-based filtering)
     * @return true nếu query cần ưu tiên ProductService, false nếu ưu tiên ES
     */
    private boolean detectSpecialIntent(String queryLower) {
        // Keywords cho metadata-based queries
        String[] specialKeywords = {
            "mới", "new", "latest", "vừa ra",
            "hot", "bán chạy", "trending", "phổ biến", "nhiều người mua",
            "khuyến mãi", "giảm giá", "flash sale", "sale", "ưu đãi",
            "nổi bật", "featured", "đặc biệt", "gợi ý",
            "tốt nhất", "chất lượng", "đánh giá cao", "review"
        };
        
        for (String keyword : specialKeywords) {
            if (queryLower.contains(keyword)) {
                log.info("🎯 Special intent keyword detected: '{}'", keyword);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Smart Fallback Strategy - Chọn sản phẩm dựa trên user intent
     * ♻️ TÁI SỬ DỤNG ProductService (giống ProductController) thay vì duplicate logic
     */
    private List<UUID> getFallbackProducts(String userQuery, int limit) {
        String queryLower = userQuery.toLowerCase();
        
        try {
            // 🔥 Strategy 1: User hỏi về SẢN PHẨM MỚI
            if (queryLower.contains("mới") || queryLower.contains("new") || 
                queryLower.contains("vừa ra") || queryLower.contains("latest")) {
                log.info("📍 Strategy: NEW products (via ProductService)");
                return productService.getLatestActiveProducts(0, limit).getContent().stream()
                    .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                    .collect(Collectors.toList());
            }
            
            // 🔥 Strategy 2: User hỏi về SẢN PHẨM HOT/TRENDING
            if (queryLower.contains("hot") || queryLower.contains("bán chạy") || 
                queryLower.contains("trending") || queryLower.contains("phổ biến") ||
                queryLower.contains("nhiều người mua")) {
                log.info("📍 Strategy: HOT/TRENDING products (via ProductService)");
                return productService.getBestSellingActiveProducts(0, limit).getContent().stream()
                    .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                    .collect(Collectors.toList());
            }
            
            // 🔥 Strategy 3: User hỏi về SẢN PHẨM ĐÁNH GIÁ TỐT
            if (queryLower.contains("tốt nhất") || queryLower.contains("chất lượng") || 
                queryLower.contains("review") || queryLower.contains("đánh giá cao") ||
                queryLower.contains("ngon")) {
                log.info("📍 Strategy: HIGH RATED products (via ProductService)");
                // ⚠️ getTopRatedProductsWithTopReview() trả về Page<ProductWithTopReviewResponse>
                // Nên dùng getFeaturedActiveProducts() thay thế (sản phẩm nổi bật thường có rating cao)
                return productService.getFeaturedActiveProducts(0, limit).getContent().stream()
                    .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                    .collect(Collectors.toList());
            }
            
            // 🔥 Strategy 4: User hỏi về SẢN PHẨM KHUYẾN MÃI/FLASH SALE
            if (queryLower.contains("khuyến mãi") || queryLower.contains("giảm giá") || 
                queryLower.contains("flash sale") || queryLower.contains("sale") ||
                queryLower.contains("ưu đãi")) {
                log.info("📍 Strategy: FLASH SALE products (via ProductService)");
                return productService.getFlashSaleProducts(0, limit).getContent().stream()
                    .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                    .collect(Collectors.toList());
            }
            
            // 🔥 Strategy 5: User hỏi về FEATURED (nổi bật)
            if (queryLower.contains("nổi bật") || queryLower.contains("featured") || 
                queryLower.contains("đặc biệt") || queryLower.contains("gợi ý")) {
                log.info("📍 Strategy: FEATURED products (via ProductService)");
                return productService.getFeaturedActiveProducts(0, limit).getContent().stream()
                    .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                    .collect(Collectors.toList());
            }
            
            // 🔥 Strategy 6: DEFAULT - Latest products (thay vì diverse)
            log.info("📍 Strategy: DEFAULT → Latest products (via ProductService)");
            return productService.getLatestActiveProducts(0, limit).getContent().stream()
                .map(com.greenconnect.greenconnect_api.dtos.response.ProductResponse::getId)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("❌ Error in getFallbackProducts, fallback to repository query: {}", e.getMessage());
            // Emergency fallback: Dùng repository trực tiếp (diverse strategy)
            return productRepository.findDiverseProductsForAi(3, limit).stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        }
    }

    /**
     * Gọi Gemini API với prompt và context
     */
    /**
     * Gọi Gemini API với lịch sử tin nhắn từ DB (không dùng request.getHistory() nữa)
     */
    private String callGeminiApiWithHistory(ChatRequest request, List<ProductAiDto> productContext, List<ChatMessage> dbHistory) {
        try {
            // Tạo system instruction (prompt chính)
            String systemPrompt = buildSystemPrompt(productContext);

            // Tạo request body cho Gemini
            Map<String, Object> requestBody = new HashMap<>();
            
            // System instruction (v1beta support)
            Map<String, Object> systemInstruction = new HashMap<>();
            Map<String, Object> systemParts = new HashMap<>();
            systemParts.put("text", systemPrompt);
            systemInstruction.put("parts", Collections.singletonList(systemParts));
            requestBody.put("system_instruction", systemInstruction);

            // Contents (history from DB + current query)
            List<Map<String, Object>> contents = new ArrayList<>();
            
            // Add history from DB (không dùng request.getHistory() nữa)
            if (dbHistory != null && !dbHistory.isEmpty()) {
                for (ChatMessage msg : dbHistory) {
                    Map<String, Object> content = new HashMap<>();
                    // Convert MessageSender enum → Gemini role
                    content.put("role", msg.getSender() == ChatMessage.MessageSender.USER ? "user" : "model");
                    Map<String, Object> parts = new HashMap<>();
                    parts.put("text", msg.getContent());
                    content.put("parts", Collections.singletonList(parts));
                    contents.add(content);
                }
            }

            // Add current user query
            Map<String, Object> userContent = new HashMap<>();
            userContent.put("role", "user");
            Map<String, Object> userParts = new HashMap<>();
            userParts.put("text", request.getUserQuery());
            userContent.put("parts", Collections.singletonList(userParts));
            contents.add(userContent);

            requestBody.put("contents", contents);

            // Generation config (yêu cầu trả về JSON - v1beta support)
            Map<String, Object> generationConfig = new HashMap<>();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 4096);
            generationConfig.put("response_mime_type", "application/json");
            requestBody.put("generation_config", generationConfig);

            // Gửi request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = geminiApiUrl + "?key=" + geminiApiKey;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 🔥 QUAN TRỌNG: In ra body lỗi mà Google trả về (sẽ cho biết chính xác sai ở đâu)
            log.error("❌ Gemini API Error Body: {}", e.getResponseBodyAsString());
            log.error("❌ Status Code: {}", e.getStatusCode());
            throw new AppException(ErrorCode.EXTERNAL_API_ERROR, "Gemini Refused: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("❌ Error calling Gemini API: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EXTERNAL_API_ERROR, "Failed to call Gemini API");
        }
    }

    /**
     * Tạo system prompt với context sản phẩm
     */
    private String buildSystemPrompt(List<ProductAiDto> productContext) {
        StringBuilder prompt = new StringBuilder();
        
        // 🔥 Thêm ngày giờ hiện tại
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        
        prompt.append("Bạn là trợ lý bán hàng ảo của ứng dụng \"Green Connect\".\n");
        prompt.append("Nhiệm vụ của bạn là tư vấn sản phẩm và giải đáp thắc mắc dựa trên danh sách sản phẩm cung cấp bên dưới.\n\n");
        
        // 🔥 Context quan trọng
        prompt.append("THÔNG TIN HỆ THỐNG:\n");
        prompt.append("- Ngày hôm nay: ").append(currentDate).append("\n");
        prompt.append("- Đơn vị tiền tệ: VND (Việt Nam Đồng)\n");
        prompt.append("- Ngôn ngữ: Tiếng Việt\n\n");
        
        // 🔥 Lấy thông tin footer động từ database
        prompt.append("THÔNG TIN VỀ SHOP GREEN CONNECT:\n");
        prompt.append("- Chuyên bán: Thực phẩm sạch, an toàn, chất lượng cao\n");
        prompt.append("- Giờ làm việc: Hoạt động 24/7\n");
        prompt.append("- Thanh toán: Hỗ trợ nhiều hình thức (COD, VNPAY)\n");
        prompt.append("- Giao hàng: Giao hàng tận nơi, đúng theo thời gian, miễn phí ship trong bán kính 5km\n");
        prompt.append("- Phí ship: Tính theo khoảng cách (miễn phí dưới 5km)\n");
        prompt.append("- Đổi trả: Chấp nhận đổi trả với lý do chính đáng (sản phẩm lỗi, không đúng mô tả, hết hạn)\n");
        
        // 🔥 Bổ sung thông tin liên hệ từ Footer (hotline, email, địa chỉ, social media)
        try {
            var footerData = footerService.getAllFooter(false);
            if (footerData != null && footerData.getSections() != null) {
                footerData.getSections().forEach(section -> {
                    if (section.getLinks() != null && !section.getLinks().isEmpty()) {
                        section.getLinks().forEach(link -> {
                            switch (link.getIconKey()) {
                                case PHONE:
                                    prompt.append("- Hotline: ").append(link.getValue()).append("\n");
                                    break;
                                case EMAIL:
                                    prompt.append("- Email: ").append(link.getValue()).append("\n");
                                    break;
                                case ADDRESS:
                                    prompt.append("- Địa chỉ: ").append(link.getValue()).append("\n");
                                    break;
                                case FACEBOOK:
                                    prompt.append("- Facebook: ").append(link.getValue()).append("\n");
                                    break;
                                case INSTAGRAM:
                                    prompt.append("- Instagram: ").append(link.getValue()).append("\n");
                                    break;
                                case TWITTER:
                                    prompt.append("- Twitter: ").append(link.getValue()).append("\n");
                                    break;
                                default:
                                    // Ignore other icon types (DOWNLOAD, etc.)
                                    break;
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not load footer data for chatbot prompt: {}", e.getMessage());
        }

        prompt.append("DANH SÁCH SẢN PHẨM HIỆN CÓ (Context):\n");
        try {
            String productsJson = objectMapper.writeValueAsString(productContext);
            prompt.append(productsJson);
        } catch (JsonProcessingException e) {
            log.error("Error converting products to JSON", e);
        }

        prompt.append("\n\nYÊU CẦU OUTPUT:\n");
        prompt.append("Bạn KHÔNG ĐƯỢC trả lời bằng văn bản thuần túy. Bạn BẮT BUỘC phải trả về định dạng JSON duy nhất như sau:\n");
        prompt.append("{\n");
        prompt.append("  \"reply_text\": \"Câu trả lời của bạn dành cho khách hàng, giải thích ngắn gọn và thân thiện.\",\n");
        prompt.append("  \"suggested_product_ids\": [\"id_san_pham_1\", \"id_san_pham_2\"]\n");
        prompt.append("}\n\n");

        prompt.append("QUY TẮC BẮT BUỘC:\n");
        prompt.append("1. 🚫 TUYỆT ĐỐI KHÔNG gợi ý sản phẩm có stockStatus = \"OUT_OF_STOCK\" hoặc totalStock = 0.\n");
        prompt.append("2. ⚠️ Ưu tiên sản phẩm có stockStatus = \"IN_STOCK\" hơn \"LOW_STOCK\".\n");
        prompt.append("3. 🌐 TUYỆT ĐỐI CHỈ TRẢ LỜI BẰNG TIẾNG VIỆT. Không được dùng tiếng Anh, tiếng Trung, hoặc bất kỳ ngôn ngữ nào khác. Trừ khi là tên riêng sản phẩm hoặc địa danh không thể dịch.\n");
        prompt.append("4. 💰 Đơn vị tiền tệ là VND, KHÔNG TỰ Ý quy đổi sang USD hoặc tiền tệ khác.\n");
        prompt.append("5. 📝 \"reply_text\": Nếu khách hỏi nấu ăn, hãy gợi ý ngắn gọn cách nấu và liệt kê nguyên liệu.\n");
        prompt.append("6. 🔢 \"suggested_product_ids\": Chỉ chọn TỐI ĐA 4-5 ID sản phẩm phù hợp nhất. Sắp xếp theo độ phù hợp giảm dần.\n");
        prompt.append("7. ❌ Không bịa ra sản phẩm không có trong danh sách.\n");
        prompt.append("8. 📦 Nếu không có sản phẩm phù hợp (hoặc tất cả đều hết hàng), để mảng rỗng [].\n");
        prompt.append("9. 🔍 Khi khách hỏi về xuất xứ/trọng lượng/hạn sử dụng, hãy dựa vào field origin/weight/expiryInfo.\n");
        prompt.append("10. 📅 Khi khách hỏi về độ tươi/còn hạn không, hãy so sánh với ngày hôm nay (").append(currentDate).append(").\n");
        prompt.append("\n🔥 METADATA - CÁCH SỬ DỤNG CÁC FIELD MỚI:\n");
        prompt.append("11. 🆕 \"createdAt\": Ngày tạo sản phẩm (YYYY-MM-DD). Khi khách hỏi \"sản phẩm mới\", ưu tiên sản phẩm có createdAt gần với ngày hôm nay.\n");
        prompt.append("12. ⭐ \"isFeatured\": true = sản phẩm nổi bật. Khi khách hỏi \"gợi ý\", \"đặc biệt\", \"nổi bật\", ưu tiên sản phẩm này.\n");
        prompt.append("13. ⚡ \"isFlashSale\": true = đang khuyến mãi. Khi khách hỏi \"giảm giá\", \"khuyến mãi\", \"flash sale\", ưu tiên sản phẩm này.\n");
        prompt.append("14. ⭐ \"averageRating\": Đánh giá trung bình (0-5). Khi khách hỏi \"chất lượng\", \"đánh giá cao\", \"tốt nhất\", ưu tiên sản phẩm có rating >= 4.0.\n");
        prompt.append("15. 🔥 \"soldCount\": Số lượng đã bán. Khi khách hỏi \"bán chạy\", \"hot\", \"phổ biến\", ưu tiên sản phẩm có soldCount cao.\n");
        prompt.append("16. ✍️ \"reply_text\" PHẢI LÀ VĂN BẢN LIỀN MẠCH, KHÔNG ĐƯỢC dùng ký tự xuống dòng (\\n), backslash (\\), hoặc các ký tự đặc biệt khác. Viết liên tục thành đoạn văn duy nhất.\n");
        
        prompt.append("\n🧠 QUYẾT ĐỊNH KHI NÀO TRẢ SẢN PHẨM:\n");
        prompt.append("17. ✅ Nếu khách hỏi về công thức nấu ăn, tìm mua nguyên liệu, hoặc hỏi giá cả → Hãy tìm ID sản phẩm phù hợp và điền vào suggested_product_ids (TỐI ĐA 4-5 sản phẩm).\n");
        prompt.append("18. ✅ Nếu khách hỏi về sản phẩm cụ thể (rau củ, trái cây, thịt, cá...) → Tìm và gợi ý sản phẩm phù hợp.\n");
        prompt.append("19. ❌ Nếu khách chỉ chào hỏi (Hi, Hello, Chào bạn), cảm ơn (Thanks, Cảm ơn, Cám ơn) → TUYỆT ĐỐI trả về mảng rỗng [] cho suggested_product_ids. KHÔNG ĐƯỢC bịa ra sản phẩm.\n");
        prompt.append("20. ℹ️ Nếu khách hỏi về chính sách shop (giao hàng, thanh toán, đổi trả, giờ làm việc, ship) → Trả lời dựa trên THÔNG TIN VỀ SHOP ở trên và trả về mảng rỗng [].\n");
        prompt.append("21. ⚠️ Nếu khách hỏi tổng quan \"Shop bán gì?\", \"Có những loại nào?\" → Chỉ gợi ý 2-3 sản phẩm featured (isFeatured = true) tiêu biểu.\n");
        prompt.append("22. 🚫 Nếu khách hỏi NGOÀI PHẠM VI (dạy tiếng Anh, toán, phỏng vấn, thời tiết, tin tức...) → Trả lời lịch sự: \"Xin lỗi, tôi chỉ hỗ trợ tư vấn về thực phẩm và sản phẩm Green Connect. Bạn có muốn tìm kiếm sản phẩm gì không?\" và trả về mảng rỗng [].\n");
        prompt.append("23. 💬 Khi trả lời chào hỏi/cảm ơn, hãy thân thiện và ngắn gọn. Ví dụ: \"Xin chào! Tôi có thể giúp gì cho bạn hôm nay?\" hoặc \"Không có gì! Nếu cần hỗ trợ gì, hãy cho tôi biết nhé!\".\n");
        
        prompt.append("\n💡 LƯU Ý QUAN TRỌNG:\n");
        prompt.append("- Khi khách hỏi về đặc điểm cụ thể (mới/hot/giảm giá/đánh giá cao), BẮT BUỘC phải dựa vào metadata để chọn sản phẩm đúng.\n");
        prompt.append("- Ví dụ: \"Có sản phẩm mới nào không?\" → Chọn sản phẩm có createdAt gần nhất.\n");
        prompt.append("- Ví dụ: \"Món nào bán chạy?\" → Chọn sản phẩm có soldCount cao nhất.\n");
        prompt.append("- Ví dụ: \"Có khuyến mãi không?\" → Chọn sản phẩm có isFlashSale = true.\n");

        return prompt.toString();
    }

    /**
     * Parse JSON response từ Gemini
     */
    private GeminiAiResponse parseGeminiResponse(String geminiResponse) {
        try {
            log.info("📥 Raw Gemini response: {}", geminiResponse);
            
            JsonNode root = objectMapper.readTree(geminiResponse);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode firstCandidate = candidates.get(0);
                
                // Kiểm tra finishReason
                String finishReason = firstCandidate.path("finishReason").asText();
                log.info("✅ Finish reason: {}", finishReason);
                
                JsonNode content = firstCandidate.path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    log.info("📝 Extracted text: {}", text);
                    
                    // ✨ FIX: Làm sạch markdown ```json và ``` nếu có
                    if (text.contains("```json")) {
                        text = text.replace("```json", "").replace("```", "");
                    } else if (text.contains("```")) {
                        text = text.replace("```", "");
                    }
                    text = text.trim(); // Xóa khoảng trắng thừa
                    
                    // Parse JSON từ text response
                    JsonNode aiResponse = objectMapper.readTree(text);
                    String replyText = aiResponse.path("reply_text").asText();
                    
                    // 🔥 CLEAN reply_text: Loại bỏ \n, \n\n, và format dư thừa
                    replyText = cleanBotResponse(replyText);
                    
                    List<UUID> productIds = new ArrayList<>();
                    JsonNode suggestedIds = aiResponse.path("suggested_product_ids");
                    if (suggestedIds.isArray()) {
                        for (JsonNode idNode : suggestedIds) {
                            try {
                                productIds.add(UUID.fromString(idNode.asText()));
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid UUID: {}", idNode.asText());
                            }
                        }
                    }
                    
                    return new GeminiAiResponse(replyText, productIds);
                }
            }
            
            throw new AppException(ErrorCode.EXTERNAL_API_ERROR, "Invalid Gemini response format");
            
        } catch (JsonProcessingException e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.EXTERNAL_API_ERROR, "Failed to parse Gemini response");
        }
    }

    /**
     * Làm sạch response text từ Gemini AI
     * Loại bỏ \n, \n\n, khoảng trắng thừa, và các ký tự format không cần thiết
     */
    private String cleanBotResponse(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        log.debug("🧹 Original text: {}", text);
        
        // 1. ⚠️ XỬ LÝ ESCAPED CHARACTERS TRƯỚC (\\n, \\t, \\r từ JSON string)
        text = text.replace("\\\\n", " ");  // \\n → space
        text = text.replace("\\\\t", " ");  // \\t → space
        text = text.replace("\\\\r", "");   // \\r → remove
        text = text.replace("\\\\", "");    // \\\\ → remove
        
        // 2. Thay \n\n (double newline) thành khoảng trắng
        text = text.replaceAll("\\n\\n+", " ");
        
        // 3. Thay \n (single newline) thành khoảng trắng
        text = text.replaceAll("\\n", " ");
        
        // 4. Loại bỏ các khoảng trắng thừa (nhiều space liên tiếp → 1 space)
        text = text.replaceAll("\\s+", " ");
        
        // 5. Loại bỏ markdown bold/italic nếu có (**text** → text)
        text = text.replaceAll("\\*\\*", "");
        text = text.replaceAll("\\*", "");
        
        // 6. Loại bỏ các ký tự control characters
        text = text.replaceAll("[\\x00-\\x1F\\x7F]", "");
        
        // 7. Trim đầu cuối
        text = text.trim();
        
        log.info("✅ Cleaned text: {}", text);
        
        return text;
    }

    /**
     * Lấy thông tin đầy đủ của các sản phẩm được gợi ý
     */
    private List<ChatResponse.ProductSuggestion> fetchProductDetails(List<UUID> productIds) {
        List<Product> products = productRepository.findAllById(productIds);
        
        return products.stream()
                .map(product -> {
                    // 🔥 Dùng averageRating từ cached field (ĐỒNG NHẤT với ProductAiDto)
                    Double avgRating = product.getAverageRating() != null 
                            ? product.getAverageRating().doubleValue() 
                            : 0.0;

                    // Lấy ảnh chính (ưu tiên mainImageUrl cached)
                    String mainImage = product.getMainImageUrlResolved();

                    // Tính price range
                    String priceRange = calculatePriceRange(product.getProductVariants());

                    // Lấy supplier name
                    String supplierName = product.getSupplier() != null 
                            ? product.getSupplier().getName() 
                            : "Green Connect";
                    
                    // 🔥 Lấy default variant ID (ưu tiên isDefault = true, fallback variant đầu tiên)
                    UUID defaultVariantId = null;
                    if (product.getProductVariants() != null && !product.getProductVariants().isEmpty()) {
                        defaultVariantId = product.getProductVariants().stream()
                            .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                            .findFirst()
                            .map(ProductVariant::getId)
                            .orElseGet(() -> product.getProductVariants().get(0).getId());
                    }

                    return ChatResponse.ProductSuggestion.builder()
                            .id(product.getId())
                            .defaultVariantId(defaultVariantId)
                            .name(product.getName())
                            .mainImage(mainImage)
                            .priceRange(priceRange)
                            .rating(avgRating)
                            .sold(product.getSellNumber())
                            .supplierName(supplierName)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Internal class để parse response từ Gemini
     */
    private static class GeminiAiResponse {
        private String replyText;
        private List<UUID> suggestedProductIds;

        public GeminiAiResponse(String replyText, List<UUID> suggestedProductIds) {
            this.replyText = replyText;
            this.suggestedProductIds = suggestedProductIds;
        }

        public String getReplyText() {
            return replyText;
        }

        public List<UUID> getSuggestedProductIds() {
            return suggestedProductIds;
        }
    }

    // ==================== NEW METHODS FOR CHAT HISTORY ====================

    @Override
    public ChatSessionListResponse getUserSessions(UUID userId) {
        log.info("📋 Loading sessions for user: {}", userId);
        
        List<ChatSession> sessions = chatSessionRepository.findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        
        List<ChatSessionListResponse.SessionItem> items = sessions.stream()
            .map(session -> {
                // Đếm số tin nhắn
                long messageCount = chatMessageRepository.countBySession(session);
                
                // Lấy tin nhắn cuối cùng
                List<ChatMessage> lastMessages = chatMessageRepository
                    .findBySession_IdOrderByCreatedAtDesc(session.getId(), PageRequest.of(0, 1));
                
                String lastMessage = lastMessages.isEmpty() ? "" : lastMessages.get(0).getContent();
                if (lastMessage.length() > 50) {
                    lastMessage = lastMessage.substring(0, 50) + "...";
                }
                
                return ChatSessionListResponse.SessionItem.builder()
                    .id(session.getId())
                    .title(session.getTitle())
                    .updatedAt(session.getUpdatedAt())
                    .messageCount((int) messageCount)
                    .lastMessage(lastMessage)
                    .build();
            })
            .collect(Collectors.toList());
        
        log.info("✅ Found {} sessions", items.size());
        
        return ChatSessionListResponse.builder()
            .sessions(items)
            .build();
    }

    @Override
    public ChatHistoryResponse getSessionHistory(UUID sessionId, UUID userId, int page, int size) {
        log.info("📜 Loading history for session: {} (page={}, size={})", sessionId, page, size);
        
        // Verify ownership
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // 🔥 Load messages với pagination (DESC order)
        List<ChatMessage> messages = chatMessageRepository
            .findBySession_IdOrderByCreatedAtDesc(sessionId, PageRequest.of(page, size));
        Collections.reverse(messages); // Đảo ngược để cũ → mới
        
        // Đếm tổng số messages
        long totalMessages = chatMessageRepository.countBySession(session);
        
        log.info("📦 Found {} messages (page {}/{}, total: {})", 
            messages.size(), page + 1, (totalMessages + size - 1) / size, totalMessages);
        
        // Convert to DTO (kèm product details)
        List<ChatHistoryResponse.ChatMessageDto> messageDtos = messages.stream()
            .map(msg -> {
                ChatHistoryResponse.ChatMessageDto dto = ChatHistoryResponse.ChatMessageDto.builder()
                    .id(msg.getId())
                    .sender(msg.getSender().name())
                    .content(msg.getContent())
                    .createdAt(msg.getCreatedAt())
                    .build();
                
                // 🔥 Nếu là BOT message và có referenceProductIds → fetch product details
                if (msg.getSender() == ChatMessage.MessageSender.BOT 
                    && msg.getReferenceProductIds() != null 
                    && !msg.getReferenceProductIds().isEmpty()) {
                    
                    try {
                        String[] idStrings = msg.getReferenceProductIds().split(",");
                        List<UUID> productIds = Arrays.stream(idStrings)
                            .map(String::trim)
                            .map(UUID::fromString)
                            .collect(Collectors.toList());
                        
                        // Fetch products từ DB
                        List<Product> products = productRepository.findAllById(productIds);
                        
                        // Convert sang ProductSuggestion
                        List<ChatHistoryResponse.ProductSuggestion> suggestions = products.stream()
                            .map(this::convertToHistoryProductDto)
                            .collect(Collectors.toList());
                        
                        dto.setSuggestedProducts(suggestions);
                        
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to load products for message {}: {}", msg.getId(), e.getMessage());
                        dto.setSuggestedProducts(Collections.emptyList());
                    }
                }
                
                return dto;
            })
            .collect(Collectors.toList());
        
        // 🔥 Calculate pagination info
        int totalPages = (int) Math.ceil((double) totalMessages / size);
        
        return ChatHistoryResponse.builder()
            .sessionId(session.getId())
            .sessionTitle(session.getTitle())
            .messages(messageDtos)
            .currentPage(page)
            .pageSize(size)
            .totalMessages(totalMessages)
            .totalPages(totalPages)
            .build();
    }

    @Override
    public UUID getCurrentSession(UUID userId) {
        log.info("🔍 Getting current session for user: {}", userId);
        
        // Tìm session active của user
        List<ChatSession> activeSessions = chatSessionRepository
            .findByUserIdAndIsActiveTrueOrderByUpdatedAtDesc(userId);
        
        if (!activeSessions.isEmpty()) {
            // Đã có session active → trả về session đầu tiên (mới nhất)
            UUID sessionId = activeSessions.get(0).getId();
            log.info("✅ Found active session: {}", sessionId);
            return sessionId;
        } else {
            // Chưa có session → tạo mới
            ChatSession newSession = ChatSession.builder()
                .userId(userId)
                .title("Chat " + new java.text.SimpleDateFormat("dd/MM HH:mm").format(new Date()))
                .isActive(true)
                .build();
            newSession = chatSessionRepository.save(newSession);
            log.info("✨ Created new session: {}", newSession.getId());
            return newSession.getId();
        }
    }

    @Override
    public void deleteSession(UUID sessionId, UUID userId) {
        log.info("🗑️ Deleting session: {}", sessionId);
        
        ChatSession session = chatSessionRepository.findById(sessionId)
            .orElseThrow(() -> new AppException(ErrorCode.CHAT_SESSION_NOT_FOUND));
        
        if (!session.getUserId().equals(userId)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        
        // Soft delete
        session.setIsActive(false);
        chatSessionRepository.save(session);
        
        log.info("✅ Session deleted (soft)");
    }

    /**
     * Convert Product → ProductSuggestion (cho history response)
     */
    private ChatHistoryResponse.ProductSuggestion convertToHistoryProductDto(Product product) {
        String priceRange = calculatePriceRange(product.getProductVariants());
        
        // 🔥 Lấy default variant ID (ưu tiên isDefault = true, fallback variant đầu tiên)
        UUID defaultVariantId = null;
        if (product.getProductVariants() != null && !product.getProductVariants().isEmpty()) {
            defaultVariantId = product.getProductVariants().stream()
                .filter(v -> Boolean.TRUE.equals(v.getIsDefault()))
                .findFirst()
                .map(ProductVariant::getId)
                .orElseGet(() -> product.getProductVariants().get(0).getId());
        }
        
        return ChatHistoryResponse.ProductSuggestion.builder()
            .id(product.getId())
            .defaultVariantId(defaultVariantId)
            .name(product.getName())
            .mainImage(product.getMainImageUrlResolved()) // 🔥 Dùng method có fallback
            .priceRange(priceRange)
            .rating(product.getAverageRating() != null ? product.getAverageRating().doubleValue() : 0.0)
            .sold(product.getSellNumber())
            .build();
    }
}
