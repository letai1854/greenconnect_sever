package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.ShippingFeeRequest;
import com.greenconnect.greenconnect_api.dtos.response.ShippingFeeResponse;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.services.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShippingServiceImpl implements ShippingService {

    private final RestTemplate restTemplate;

    @Value("${ghtk.api.url}")
    private String ghtkApiUrl;

    @Value("${ghtk.api.token}")
    private String ghtkToken;

    @Override
    public ShippingFeeResponse calculateShippingFee(ShippingFeeRequest request) {
        if (request.getPickProvince() == null || request.getProvince() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        try {
            // --- BƯỚC 1: Gọi xteam lấy Distance ---
            // QUAN TRỌNG: Phải dùng Clean Address thì xteam mới tìm ra đường ở mọi tỉnh
            log.info(">>> STEP 1: Gọi xteam (Clean Address) để lấy Distance...");
            ShippingFeeResponse distanceResp = callGhtkApi(request, "xteam", true); 
            
            Double realDistance = 0.0;
            if (distanceResp != null && distanceResp.getSuccess()) {
                realDistance = distanceResp.getFee().getDistance();
                log.info("✅ Distance tìm được: {} km", realDistance);
            }

            // --- BƯỚC 2: Gọi none lấy Giá tiền rẻ ---
            log.info(">>> STEP 2: Gọi none (Clean Address) để lấy Giá tiền...");
            ShippingFeeResponse feeResp = callGhtkApi(request, "none", true);

            if (feeResp == null || !feeResp.getSuccess()) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
            }

            // --- BƯỚC 3: Gộp kết quả ---
            feeResp.getFee().setDistance(realDistance);

            log.info("🎉 KẾT QUẢ CUỐI: Fee={} VND, Distance={} km", 
                     feeResp.getFee().getFee(), 
                     feeResp.getFee().getDistance());

            return feeResp;

        } catch (Exception e) {
            log.error("Lỗi: {}", e.getMessage());
            e.printStackTrace();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    // Hàm gọi API (Có tham số useCleanAddress)
    // Hàm gọi API (Đã nâng cấp để Clean toàn bộ địa chỉ)
    private ShippingFeeResponse callGhtkApi(ShippingFeeRequest request, String forcedOption, boolean useCleanAddress) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Token", ghtkToken);
            headers.set("User-Agent", "PostmanRuntime/7.49.1");
            headers.setAccept(Collections.singletonList(MediaType.ALL));
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 1. LÀM SẠCH ĐỊA CHỈ NGƯỜI GỬI (PICK) - QUAN TRỌNG
            // Java đang gửi "Phường Tân Quy" -> Phải clean thành "Tân Quy" giống Postman
            String pickProvinceRaw = useCleanAddress ? cleanAddress(request.getPickProvince()) : request.getPickProvince();
            String pickDistrictRaw = useCleanAddress ? cleanAddress(request.getPickDistrict()) : request.getPickDistrict();
            String pickWardRaw     = useCleanAddress ? cleanAddress(request.getPickWard()) : request.getPickWard();

            // 2. LÀM SẠCH ĐỊA CHỈ NGƯỜI NHẬN
            String provinceRaw = useCleanAddress ? cleanAddress(request.getProvince()) : request.getProvince();
            String districtRaw = useCleanAddress ? cleanAddress(request.getDistrict()) : request.getDistrict();
            String wardRaw     = useCleanAddress ? cleanAddress(request.getWard()) : request.getWard();

            // Encode dữ liệu
            String pickProvince = encodeValue(pickProvinceRaw);
            String pickDistrict = encodeValue(pickDistrictRaw);
            String pickWard     = encodeValue(pickWardRaw);
            
            String province = encodeValue(provinceRaw);
            String district = encodeValue(districtRaw);
            String ward     = encodeValue(wardRaw);

            StringBuilder urlBuilder = new StringBuilder(ghtkApiUrl);
            urlBuilder.append("?pick_province=").append(pickProvince);
            urlBuilder.append("&pick_district=").append(pickDistrict);
            
            // Gửi pick_ward đã được clean
            if (pickWard != null && !pickWard.isEmpty()) {
                urlBuilder.append("&pick_ward=").append(pickWard);
            }

            // HACK URL: province + space + district (Đã clean)
            urlBuilder.append("&province=").append(province); 
            urlBuilder.append("%20district=").append(district);

            // Gửi ward đã được clean
            if (ward != null && !ward.isEmpty()) {
                urlBuilder.append("&ward=").append(ward);
            }
            
            urlBuilder.append("&weight=").append(request.getWeight());
            urlBuilder.append("&deliver_option=").append(forcedOption);
            urlBuilder.append("&transport=").append((request.getTransport() == null) ? "road" : request.getTransport());
           
            // log.info(">>> URL: {}", urlBuilder.toString()); // Bật lên để kiểm tra

            URI uri = URI.create(urlBuilder.toString());
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
            ResponseEntity<ShippingFeeResponse> response = restTemplate.exchange(uri, HttpMethod.GET, requestEntity, ShippingFeeResponse.class);
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    // Hàm xóa từ thừa - CHÌA KHÓA ĐỂ XTEAM KHÔNG BỊ 0 KM
    private String cleanAddress(String value) {
        if (value == null) return "";
        String s = value;
        s = s.replace("Tỉnh ", "").replace("Thành phố ", "")
             .replace("Huyện ", "").replace("Quận ", "").replace("Thị xã ", "")
             .replace("Xã ", "").replace("Phường ", "").replace("Thị trấn ", "");
        return s.trim();
    }

    private String encodeValue(String value) {
        if (value == null) return "";
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }
}