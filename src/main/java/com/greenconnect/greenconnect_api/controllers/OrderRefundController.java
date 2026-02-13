// package com.greenconnect.greenconnect_api.controllers;

// import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRefund;
// import com.greenconnect.greenconnect_api.dtos.request.RequestOrdersRefund;
// import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
// import com.greenconnect.greenconnect_api.dtos.response.OrderRefundRespone;
// import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
// import com.greenconnect.greenconnect_api.services.OrderRefundService;
// import com.greenconnect.greenconnect_api.utils.SecurityUtils;
// import jakarta.validation.Valid;
// import lombok.RequiredArgsConstructor;
// import lombok.experimental.FieldDefaults;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.web.bind.annotation.*;

// import java.util.UUID;

// import static lombok.AccessLevel.PRIVATE;

// @RestController
// @RequestMapping("/order-refunds")
// @RequiredArgsConstructor
// @FieldDefaults(level = PRIVATE, makeFinal = true)
// @Slf4j
// public class OrderRefundController {

//     OrderRefundService orderRefundService;

//     @PostMapping("/create")
//     public ResponseEntity<ApiResponse<OrderRefundRespone>> create(@Valid @RequestBody CreateOrderRefund request) {
//         OrderRefundRespone resp = orderRefundService.create(request);
//         return ResponseEntity.ok(ResponseUtil.success(resp, "Tạo yêu cầu hoàn/huỷ đơn thành công"));
//     }

//     @PutMapping("/update/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<ApiResponse<OrderRefundRespone>> update(@PathVariable("id") UUID id,
//                                                                    @Valid @RequestBody RequestOrdersRefund request) {
//         // admin-only enforced in service
//         OrderRefundRespone resp = orderRefundService.update(id, request);
//         return ResponseEntity.ok(ResponseUtil.success(resp, "Cập nhật yêu cầu thành công"));
//     }

//     @DeleteMapping("/delete/{id}")
//     public ResponseEntity<ApiResponse<Object>> delete(@PathVariable("id") UUID id) {
//         orderRefundService.delete(id);
//         return ResponseEntity.ok(ResponseUtil.success(null, "Xóa yêu cầu thành công"));
//     }

//     @GetMapping("/active")
//     public ResponseEntity<ApiResponse<java.util.List<OrderRefundRespone>>> getActive() {
//         java.util.List<OrderRefundRespone> list = orderRefundService.findAllActive();
//         return ResponseEntity.ok(ResponseUtil.success(list, "Danh sách yêu cầu hoàn/huỷ đang active"));
//     }

//     @GetMapping("/all")
//     public ResponseEntity<ApiResponse<java.util.List<OrderRefundRespone>>> getAll(@RequestParam(name = "includeInactive", required = false, defaultValue = "false") boolean includeInactive) {
//         java.util.List<OrderRefundRespone> list = orderRefundService.findAll(includeInactive);
//         return ResponseEntity.ok(ResponseUtil.success(list, "Danh sách tất cả yêu cầu hoàn/huỷ"));
//     }
// }

