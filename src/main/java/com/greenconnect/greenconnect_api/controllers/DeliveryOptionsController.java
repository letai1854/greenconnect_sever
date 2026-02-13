package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.DeliveryOptionRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.DeliveryOptionRespone;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.services.DeliveryOptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@RestController
@RequestMapping("/delivery-options")
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DeliveryOptionsController {

    DeliveryOptionService deliveryOptionService;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryOptionRespone>> create(@Valid @RequestBody DeliveryOptionRequest request) {
        log.info("POST /delivery-options - create delivery option: {}", request.getServiceName());
        DeliveryOptionRespone resp = deliveryOptionService.createDeliveryOption(request);
        return ResponseEntity.ok(ResponseUtil.success(resp, "Tạo phương thức giao hàng thành công"));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryOptionRespone>> update(@PathVariable("id") java.util.UUID id,
                                                                      @Valid @RequestBody DeliveryOptionRequest request) {
        log.info("PUT /delivery-options/{} - update delivery option", id);
        DeliveryOptionRespone resp = deliveryOptionService.updateDeliveryOption(id, request);
        return ResponseEntity.ok(ResponseUtil.success(resp, "Cập nhật phương thức giao hàng thành công"));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<java.util.List<DeliveryOptionRespone>>> getActive() {
        log.info("GET /delivery-options/active - public request");
        java.util.List<DeliveryOptionRespone> list = deliveryOptionService.getActiveDeliveryOptions();
        return ResponseEntity.ok(ResponseUtil.success(list, "Danh sách phương thức giao hàng đang hoạt động"));
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<java.util.List<DeliveryOptionRespone>>> getAll() {
        log.info("GET /delivery-options/all - admin request");
        java.util.List<DeliveryOptionRespone> list = deliveryOptionService.getAllDeliveryOptions();
        return ResponseEntity.ok(ResponseUtil.success(list, "Danh sách tất cả phương thức giao hàng"));
    }
}
