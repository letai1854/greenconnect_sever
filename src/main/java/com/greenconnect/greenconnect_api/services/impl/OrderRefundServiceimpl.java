// package com.greenconnect.greenconnect_api.services.impl;

// import com.greenconnect.greenconnect_api.dtos.request.CreateOrderRefund;
// import com.greenconnect.greenconnect_api.dtos.response.OrderRefundRespone;
// import com.greenconnect.greenconnect_api.entities.Order;
// import com.greenconnect.greenconnect_api.entities.OrderRequest;
// import com.greenconnect.greenconnect_api.entities.RequestMedia;
// import com.greenconnect.greenconnect_api.entities.User;
// import com.greenconnect.greenconnect_api.enums.CreatedBy;
// import com.greenconnect.greenconnect_api.enums.PaymentMethod;
// import com.greenconnect.greenconnect_api.enums.RefundStatus;
// import com.greenconnect.greenconnect_api.enums.RequestStatus;
// import com.greenconnect.greenconnect_api.enums.RequestType;
// import com.greenconnect.greenconnect_api.enums.OrderStatus;
// import com.greenconnect.greenconnect_api.repositories.OrderRequestRepository;
// import com.greenconnect.greenconnect_api.repositories.OrderRepository;
// import com.greenconnect.greenconnect_api.repositories.UserRepository;
// import com.greenconnect.greenconnect_api.repositories.RequestMediaRepository;
// import com.greenconnect.greenconnect_api.services.OrderRefundService;
// import com.greenconnect.greenconnect_api.dtos.request.RequestOrdersRefund;
// import com.greenconnect.greenconnect_api.services.EmailService;
// import com.greenconnect.greenconnect_api.utils.SecurityUtils;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.UUID;

// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class OrderRefundServiceimpl implements OrderRefundService {

// 	private final OrderRequestRepository orderRequestRepository;
// 	private final OrderRepository orderRepository;
// 	private final UserRepository userRepository;
// 	private final RequestMediaRepository requestMediaRepository;
// 	private final EmailService emailService;

// 	@Override
// 	@Transactional
// 	public OrderRefundRespone create(CreateOrderRefund request) {
// 		// Basic validations
// 		Order order = orderRepository.findById(request.getOrderId()).orElseThrow(() ->
// 				new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
// 						"Order not found: " + request.getOrderId())
// 		);

// 		User user = userRepository.findById(request.getUserId()).orElseThrow(() ->
// 				new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.USER_NOT_FOUND,
// 						"User not found: " + request.getUserId())
// 		);

// 		// Build OrderRequest
// 		OrderRequest or = OrderRequest.builder()
// 				.order(order)
// 				.user(user)
// 				.requestType(RequestType.valueOf(request.getRequestType()))
// 				.createdBy(CreatedBy.USER)
// 				.reason(request.getReason())
// 				.status(RequestStatus.DANG_XU_LY)
// 				.refundAmount(request.getRefundAmount())
// 				.refundStatus(RefundStatus.PENDING_CUSTOMER_INFO)
// 				.customerBankAccountName(request.getCustomerBankAccountName())
// 				.customerBankAccountNumber(request.getCustomerBankAccountNumber())
// 				.customerBankCode(request.getCustomerBankCode())
// 				.processedByAdmin(null)
// 				.build();

// 		// Adjust behavior per rules (cancellation vs return and payment method)
// 		if (or.getRequestType() == RequestType.GOP_Y) {
// 			// if order.paymentMethod == COD, immediate complete
// 			if (order.getPaymentMethod() == PaymentMethod.COD) {
// 				or.setStatus(RequestStatus.HOAN_THANH);
// 				or.setRefundStatus(RefundStatus.COMPLETED);
// 				or.setRefundAmount(null);
// 			} else {
// 				// transfer method: set pending processing and use provided refund amount
// 				or.setStatus(RequestStatus.HOAN_THANH);
// 				or.setRefundStatus(RefundStatus.PENDING_PROCESSING);
// 			}
// 		} else if (or.getRequestType() == RequestType.GOP_Y) {
// 			or.setStatus(RequestStatus.DANG_XU_LY);
// 			or.setRefundStatus(RefundStatus.PENDING_CUSTOMER_INFO);
// 		}

// 	OrderRequest saved = orderRequestRepository.save(or);

// 		// If the request is a CANCEL and completed immediately (e.g., COD), mark order as canceled
// 		if (saved.getRequestType() == RequestType.GOP_Y && saved.getStatus() == RequestStatus.HOAN_THANH) {
// 			Order ord = saved.getOrder();
// 			if (ord != null) {
// 				ord.setOrderStatus(OrderStatus.DA_HUY);
// 				orderRepository.save(ord);
// 			}
// 		} else if (saved.getRequestType() == RequestType.GOP_Y) {
// 			// mark the order as having a return request pending
// 			Order ord = saved.getOrder();
// 			if (ord != null) {
// 				ord.setOrderStatus(OrderStatus.YEU_CAU_TRA_HANG);
// 				orderRepository.save(ord);
// 			}
// 		}

// 		// Save request media if any: request now contains a list of RequestMedia objects.
// 		List<UUID> mediaIds = new ArrayList<>();
// 		if (request.getRequestMediaIds() != null && !request.getRequestMediaIds().isEmpty()) {
// 			for (RequestMedia rmPayload : request.getRequestMediaIds()) {
// 				if (rmPayload == null) continue;
// 				if (rmPayload.getId() != null) {
// 					// update existing media if present
// 					RequestMedia existingMedia = requestMediaRepository.findById(rmPayload.getId()).orElse(null);
// 					if (existingMedia != null) {
// 						existingMedia.setMediaUrl(rmPayload.getMediaUrl());
// 						existingMedia.setMediaType(rmPayload.getMediaType());
// 						existingMedia.setOrderRequest(saved);
// 						requestMediaRepository.save(existingMedia);
// 						mediaIds.add(existingMedia.getId());
// 					}
// 				} else {
// 					// create new media record
// 					RequestMedia newMedia = RequestMedia.builder()
// 							.mediaUrl(rmPayload.getMediaUrl())
// 							.mediaType(rmPayload.getMediaType())
// 							.orderRequest(saved)
// 							.build();
// 					requestMediaRepository.save(newMedia);
// 					mediaIds.add(newMedia.getId());
// 				}
// 			}
// 		}

// 		// Build response
// 		OrderRefundRespone resp = OrderRefundRespone.builder()
// 				.id(saved.getId())
// 				.orderId(order.getId())
// 				.userId(user.getId())
// 				.requestType(saved.getRequestType().name())
// 				.reason(saved.getReason())
// 				.refundAmount(saved.getRefundAmount())
// 				.customerBankAccountName(saved.getCustomerBankAccountName())
// 				.customerBankAccountNumber(saved.getCustomerBankAccountNumber())
// 				.customerBankCode(saved.getCustomerBankCode())
// 				.refundStatus(saved.getRefundStatus() == null ? null : saved.getRefundStatus().name())
// 				.requestStatus(saved.getStatus() == null ? null : saved.getStatus().name())
// 				.createdAt(saved.getCreatedAt())
// 				.updatedAt(saved.getUpdatedAt())
// 				.requestMediaIds(mediaIds)
// 				.build();

// 		// TODO: trigger email to user if admin processed and status completed

// 		// If this request is already completed and processedByAdmin is null,
// 		// one could notify the user. For create-by-user flows we typically don't
// 		// send email here; admin actions trigger emails in update().

// 		return resp;
// 	}

// 	@Override
// 	@Transactional
// 	public OrderRefundRespone update(UUID id, RequestOrdersRefund request) {
// 		// Admin-only update


// 		OrderRequest existing = orderRequestRepository.findById(id).orElseThrow(() ->
// 				new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
// 						"Order request not found: " + id));
// 		// Remember previous status to detect transitions
// 		RequestStatus previousStatus = existing.getStatus();

// 		// Update allowed fields (from RequestOrdersRefund)
// 		if (request.getReason() != null) existing.setReason(request.getReason());
// 		if (request.getRefundAmount() != null) existing.setRefundAmount(request.getRefundAmount());
// 		if (request.getCustomerBankAccountName() != null)
// 			existing.setCustomerBankAccountName(request.getCustomerBankAccountName());
// 		if (request.getCustomerBankAccountNumber() != null)
// 			existing.setCustomerBankAccountNumber(request.getCustomerBankAccountNumber());
// 		if (request.getCustomerBankCode() != null) existing.setCustomerBankCode(request.getCustomerBankCode());

// 		// If admin provided a refundStatus or requestStatus string, attempt to parse and set
// 		if (request.getRefundStatus() != null) {
// 			try {
// 				existing.setRefundStatus(RefundStatus.valueOf(request.getRefundStatus()));
// 			} catch (IllegalArgumentException ex) {
// 				throw new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.BAD_REQUEST,
// 						"Invalid refundStatus: " + request.getRefundStatus());
// 			}
// 		}

// 		if (request.getRequestStatus() != null) {
// 			try {
// 				existing.setStatus(RequestStatus.valueOf(request.getRequestStatus()));
// 			} catch (IllegalArgumentException ex) {
// 				throw new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.BAD_REQUEST,
// 						"Invalid requestStatus: " + request.getRequestStatus());
// 			}
// 		}


// 		// Persist changes
// 		OrderRequest saved = orderRequestRepository.save(existing);

// 		// Handle request media updates/creations for admin update: if payload contains media objects,
// 		// update existing ones (by id) or create new ones when id is null.
// 		List<UUID> mediaIds = new ArrayList<>();
// 		if (request.getRequestMediaIds() != null && !request.getRequestMediaIds().isEmpty()) {
// 			for (RequestMedia rmPayload : request.getRequestMediaIds()) {
// 				if (rmPayload == null) continue;
// 				if (rmPayload.getId() != null) {
// 					RequestMedia existingMedia = requestMediaRepository.findById(rmPayload.getId()).orElse(null);
// 					if (existingMedia != null) {
// 						existingMedia.setMediaUrl(rmPayload.getMediaUrl());
// 						existingMedia.setMediaType(rmPayload.getMediaType());
// 						existingMedia.setOrderRequest(saved);
// 						requestMediaRepository.save(existingMedia);
// 						mediaIds.add(existingMedia.getId());
// 					}
// 				} else {
// 					RequestMedia newMedia = RequestMedia.builder()
// 							.mediaUrl(rmPayload.getMediaUrl())
// 							.mediaType(rmPayload.getMediaType())
// 							.orderRequest(saved)
// 							.build();
// 					requestMediaRepository.save(newMedia);
// 					mediaIds.add(newMedia.getId());
// 				}
// 			}
// 		}

// 		// Send notification and update order status when admin updates the request
// 		try {
// 			Order ord = saved.getOrder();

// 			// If admin marked the request as HOAN_THANH
// 			if (saved.getStatus() == RequestStatus.HOAN_THANH) {
// 				if (ord != null) {
// 					if (saved.getRequestType() == RequestType.GOP_Y) {
// 						ord.setOrderStatus(OrderStatus.DA_HUY);
// 					} else if (saved.getRequestType() == RequestType.GOP_Y) {
// 						// Completed return -> mark order as return-success
// 						ord.setOrderStatus(OrderStatus.TRA_HANG_THANH_CONG);
// 					}
// 					orderRepository.save(ord);
// 				}
// 			}

// 			// Email: only when a RETURN transitions into HOAN_THANH by admin
// 			if (previousStatus != RequestStatus.HOAN_THANH
// 					&& saved.getStatus() == RequestStatus.HOAN_THANH
// 					&& saved.getRequestType() == RequestType.GOP_Y) {
// 				String loginLink = "http://orderrefund/login/" + saved.getUser().getId();
// 				String subject = "[GreenConnect] Thông báo: Yêu cầu hoàn trả đã được chấp thuận";

// 				// Choose an Order instance to read orderCode/totalPayment for the email. Prefer saved.getOrder(),
// 				// but fall back to the request payload's orderId (admin may have passed it) to fetch the Order.
// 				Order emailOrder = saved.getOrder();
// 				if (emailOrder == null && request != null && request.getOrderId() != null) {
// 					emailOrder = orderRepository.findById(request.getOrderId()).orElse(null);
// 				}

// 				String refundAmountStr = saved.getRefundAmount() == null ? "(chưa cung cấp)" : saved.getRefundAmount().toString();
// 				String totalPaymentStr = emailOrder != null && emailOrder.getTotalPayment() != null ? emailOrder.getTotalPayment().toString() : "(không xác định)";
// 				String orderCodeStr = emailOrder != null && emailOrder.getOrderCode() != null ? emailOrder.getOrderCode() : "(không có mã đơn)";

// 				String body = String.format(
// 						"Chào %s,\n\n" +
// 								"Yêu cầu hoàn trả của bạn cho đơn hàng mã: %s đã được quản trị viên chấp thuận. Dưới đây là một số thông tin và hướng dẫn để bạn hoàn tất việc nhận tiền:\n\n" +
// 								"Thông tin yêu cầu:\n" +
// 								"- Loại yêu cầu: %s\n" +
// 								"- Số tiền hoàn trả (tạm): %s\n" +
// 								"- Tổng giá trị đơn hàng: %s\n\n" +
// 								"Hướng dẫn tiếp theo:\n" +
// 								"1) Vui lòng truy cập đường dẫn sau và cập nhật thông tin tài khoản ngân hàng (tên chủ tài khoản, số tài khoản, mã ngân hàng):\n" +
// 								"%s\n\n" +
// 								"2) Sau khi bạn gửi thông tin, bộ phận tài chính sẽ kiểm tra và xử lý trong vòng 3-7 ngày làm việc. Thời gian thực tế phụ thuộc vào quy trình ngân hàng và phương thức chuyển khoản.\n\n" +
// 								"Lưu ý bảo mật:\n" +
// 								"- Không chia sẻ đường dẫn này hoặc thông tin tài khoản với bất kỳ ai.\n" +
// 								"- Nếu bạn không yêu cầu hoàn trả hoặc nghi ngờ có hành vi gian lận, vui lòng liên hệ ngay support.\n\n" +
// 								"Hỗ trợ:\n" +
// 								"- Email: support@greenconnect.example\n" +
// 								"- Hotline: 0123-456-789\n\n" +
// 								"Trân trọng,\n" +
// 								"Đội ngũ GreenConnect",
// 						saved.getUser().getFullName() != null ? saved.getUser().getFullName() : saved.getUser().getEmail(),
// 						orderCodeStr,
// 						saved.getRequestType() != null ? saved.getRequestType().name() : "(không xác định)",
// 						refundAmountStr,
// 						totalPaymentStr,
// 						loginLink);

// 				// Use async email to avoid blocking admin update flow
// 				emailService.sendPlainEmailAsync(saved.getUser().getEmail(), subject, body);
// 			}
// 		} catch (Exception e) {
// 			log.warn("Failed to send refund notification email for request {}: {}", id, e.getMessage());
// 		}

// 		OrderRefundRespone resp = OrderRefundRespone.builder()
// 				.id(saved.getId())
// 				.orderId(saved.getOrder().getId())
// 				.userId(saved.getUser().getId())
// 				.requestType(saved.getRequestType().name())
// 				.reason(saved.getReason())
// 				.refundAmount(saved.getRefundAmount())
// 				.customerBankAccountName(saved.getCustomerBankAccountName())
// 				.customerBankAccountNumber(saved.getCustomerBankAccountNumber())
// 				.customerBankCode(saved.getCustomerBankCode())
// 				.refundStatus(saved.getRefundStatus() == null ? null : saved.getRefundStatus().name())
// 				.requestStatus(saved.getStatus() == null ? null : saved.getStatus().name())
// 				.createdAt(saved.getCreatedAt())
// 				.updatedAt(saved.getUpdatedAt())
// 				.build();

// 		return resp;
// 	}

// 	@Override
// 	@Transactional
// 	public void delete(UUID id) {
// 		if (!SecurityUtils.isAdmin()) {
// 			throw new com.greenconnect.greenconnect_api.exceptions.AppException(
// 					com.greenconnect.greenconnect_api.exceptions.ErrorCode.FORBIDDEN,
// 					"Only admin can delete order requests");
// 		}

// 		OrderRequest existing = orderRequestRepository.findById(id).orElseThrow(() ->
// 				new com.greenconnect.greenconnect_api.exceptions.AppException(
// 						com.greenconnect.greenconnect_api.exceptions.ErrorCode.NOT_FOUND,
// 						"Order request not found: " + id));

// 		// Soft-delete: just delete the record from repo
// 		orderRequestRepository.delete(existing);
// 	}

// 	@Override
// 	@Transactional
// 	public OrderRefundRespone updateAdmin(UUID id, com.greenconnect.greenconnect_api.dtos.request.UpdateOrderRequestAdmin request) {
// 		// Map admin DTO to RequestOrdersRefund and call existing update
// 		com.greenconnect.greenconnect_api.dtos.request.RequestOrdersRefund r = new com.greenconnect.greenconnect_api.dtos.request.RequestOrdersRefund();
// 		r.setReason(request.getReason());
// 		r.setRefundAmount(request.getRefundAmount());
// 		r.setCustomerBankAccountName(request.getCustomerBankAccountName());
// 		r.setCustomerBankAccountNumber(request.getCustomerBankAccountNumber());
// 		r.setCustomerBankCode(request.getCustomerBankCode());
// 		r.setBankTransactionCode(request.getBankTransactionCode());
// 		r.setRefundStatus(request.getRefundStatus());
// 		r.setRequestStatus(request.getRequestStatus());
// 	r.setRequestMediaIds(request.getRequestMediaIds());

// 		return update(id, r);
// 	}

// 	@Override
// 	@Transactional(readOnly = true)
// 	public java.util.List<OrderRefundRespone> findAllActive() {
// 		return findAll(false);
// 	}

// 	@Override
// 	@Transactional(readOnly = true)
// 	public java.util.List<OrderRefundRespone> findAll(Boolean includeDeleted) {
// 		java.util.List<com.greenconnect.greenconnect_api.entities.OrderRequest> list;
// 		// Fetch all (no soft-delete marker, so just get all records)
// 		list = orderRequestRepository.findAll();

// 		java.util.List<OrderRefundRespone> resp = new java.util.ArrayList<>();
// 		for (com.greenconnect.greenconnect_api.entities.OrderRequest or : list) {
// 			OrderRefundRespone r = OrderRefundRespone.builder()
// 					.id(or.getId())
// 					.orderId(or.getOrder() != null ? or.getOrder().getId() : null)
// 					.userId(or.getUser() != null ? or.getUser().getId() : null)
// 					.requestType(or.getRequestType() != null ? or.getRequestType().name() : null)
// 					.reason(or.getReason())
// 					.refundAmount(or.getRefundAmount())
// 			.customerBankAccountName(or.getCustomerBankAccountName())
// 				.customerBankAccountNumber(or.getCustomerBankAccountNumber())
// 				.customerBankCode(or.getCustomerBankCode())
// 				.refundStatus(or.getRefundStatus() == null ? null : or.getRefundStatus().name())
// 					.requestStatus(or.getStatus() == null ? null : or.getStatus().name())
// 					.createdAt(or.getCreatedAt())
// 					.updatedAt(or.getUpdatedAt())
// 					.build();
// 			resp.add(r);
// 		}
// 		return resp;
// 	}
// }
