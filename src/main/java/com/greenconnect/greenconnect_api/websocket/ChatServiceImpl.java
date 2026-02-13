package com.greenconnect.greenconnect_api.websocket;


import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.ChatMessageRequest;
import com.greenconnect.greenconnect_api.dtos.response.ConversationListItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.ConversationResponse;
import com.greenconnect.greenconnect_api.dtos.response.MessageResponse;
import com.greenconnect.greenconnect_api.entities.Conversation;
import com.greenconnect.greenconnect_api.entities.Message;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.enums.Role;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.ConversationRepository;
import com.greenconnect.greenconnect_api.repositories.MessageRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationService notificationService;
    private final ConversationMapper conversationMapper;

    @Override
    @Transactional
    public ConversationResponse startConversation(String customerEmail, String firstMessage) {
        log.info("🆕 [START CONVERSATION] User '{}' đang tạo conversation mới", customerEmail);
        
        try {
            User customer = userRepository.findByEmailIgnoreCase(customerEmail)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + customerEmail));

            // ✅ Kiểm tra xem user đã có conversation chưa (tránh duplicate nếu spam click)
            Page<Conversation> existingConversations = conversationRepository.findByCustomerEmailOrderByUpdatedAtDesc(
                customerEmail,
                org.springframework.data.domain.PageRequest.of(0, 1)
            );
            
            if (!existingConversations.isEmpty()) {
                Conversation existing = existingConversations.getContent().get(0);
                log.warn("⚠️ [DUPLICATE PREVENTED] User '{}' đã có conversation: {}", customerEmail, existing.getId());
                
                // Trả về conversation hiện tại thay vì tạo mới
                return ConversationResponse.builder()
                        .conversationId(existing.getId())
                        .status(existing.getStatus())
                        .build();
            }

            Conversation newConversation = Conversation.builder()
                    .customer(customer)
                    .status("NEW")
                    .assignee(null)
                    .build();
            Conversation savedConversation = conversationRepository.save(newConversation);

            Message firstMsg = Message.builder()
                    .conversation(savedConversation)
                    .sender(customer)
                    .content(firstMessage)
                    .messageType(com.greenconnect.greenconnect_api.enums.MessageType.TEXT)
                    .build();
            messageRepository.save(firstMsg);
            
            // ⭐ QUAN TRỌNG: Flush để đảm bảo createdAt được Hibernate gán giá trị
            conversationRepository.flush();
            
            log.info("✅ [CONVERSATION CREATED] ID: {}, CreatedAt: {}", 
                    savedConversation.getId(), savedConversation.getCreatedAt());
            
            // ================= ⭐ THÔNG BÁO CONVERSATION MỚI (ASYNC - KHÔNG CHẶN) ⭐ =================
            try {
                notificationService.notifyNewConversation(savedConversation, firstMessage);
                log.info("✅ [NOTIFICATION SENT] Broadcast thành công cho conversation: {}", savedConversation.getId());
            } catch (Exception notificationError) {
                // ⚠️ Nếu notification lỗi, chỉ log warning, KHÔNG rollback transaction
                log.error("⚠️ [NOTIFICATION FAILED] Không thể gửi thông báo, nhưng conversation đã được tạo: {}", 
                        notificationError.getMessage());
            }
            // ================================================================================
            
            return ConversationResponse.builder()
                    .conversationId(savedConversation.getId())
                    .status(savedConversation.getStatus())
                    .build();
                    
        } catch (AppException e) {
            log.error("❌ [START CONVERSATION ERROR] AppException: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ [START CONVERSATION ERROR] Unexpected error for user '{}': {}", customerEmail, e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể tạo cuộc trò chuyện: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Page<MessageResponse> getMessagesForConversation(UUID conversationId, String userEmail, Pageable pageable) {
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy cuộc trò chuyện: " + conversationId));

        // Kiểm tra quyền truy cập
        boolean isCustomerOfConversation = conversation.getCustomer().getId().equals(user.getId());
        Set<Role> userRoles = user.getActiveRoles();
        boolean isSupportAgent = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);

        if (!isCustomerOfConversation && !isSupportAgent) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện này.");
        }

        Page<Message> messagePage = messageRepository.findByConversation_IdOrderByCreatedAtDesc(conversationId, pageable);
        
        // ⭐ TỰ ĐỘNG đánh dấu đã đọc khi user lấy messages
        markConversationAsRead(conversationId, userEmail);
        
        return messagePage.map(messageMapper::toMessageResponse);
    }

    @Override
    @Transactional
    public void processAndBroadcastMessage(UUID conversationId, ChatMessageRequest messageRequest, String senderEmail) {
        User sender = userRepository.findByEmailIgnoreCase(senderEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người gửi với email: " + senderEmail));

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy cuộc trò chuyện: " + conversationId));
        
        // Kiểm tra quyền gửi tin nhắn (logic tương tự getMessages)
        // ...
        
        // Nếu đây là tin nhắn đầu tiên từ nhân viên, gán họ làm người phụ trách
        Set<Role> senderRoles = sender.getActiveRoles();
        boolean isAgent = senderRoles.contains(Role.ADMIN) || senderRoles.contains(Role.CUSTOMER_SUPPORT);
        if (isAgent && conversation.getAssignee() == null) {
            conversation.setAssignee(sender);
            conversation.setStatus("OPEN");
            conversationRepository.save(conversation);
        }

        // ⭐ TẠO VÀ LƯU TIN NHẮN VÀO DB
        Message message = messageMapper.toMessage(messageRequest);
        message.setConversation(conversation);
        message.setSender(sender);
        
        // ⭐ NẾU LƯU DB THẤT BẠI → EXCEPTION → CONTROLLER BẮT VÀ GỬI ERROR
        Message savedMessage = messageRepository.save(message);

        // ⭐ CHUẨN BỊ RESPONSE
        MessageResponse messageResponse = messageMapper.toMessageResponse(savedMessage);
        messageResponse.setTempMessageId(messageRequest.getTempMessageId()); // Gắn tempId để client tracking

        // ⭐⭐ BƯỚC 1: BROADCAST ĐẾN TOÀN BỘ CONVERSATION TRƯỚC (quan trọng!)
        // Đảm bảo TẤT CẢ mọi người (kể cả người gửi) nhận tin nhắn CÙNG LÚC
        // Người nhận: Thấy tin nhắn mới ngay lập tức
        // Người gửi: Client sẽ check tempId để tránh duplicate (tin nhắn đã có ở PENDING)
        String broadcastDestination = "/topic/conversations/" + conversationId;
        log.info("📡 [BROADCASTING] Gửi tin nhắn {} đến kênh: {}", savedMessage.getId(), broadcastDestination);
        messagingTemplate.convertAndSend(broadcastDestination, messageResponse);
        
        // ⭐⭐ BƯỚC 2: SAU ĐÓ GỬI ACK RIÊNG CHO NGƯỜI GỬI (để cập nhật status)
        // Client sẽ nhận broadcast trước (nhưng không thêm duplicate vì đã check tempId)
        // Sau đó nhận ACK này để update status từ PENDING → SENT (icon ⏳ → ✓)
        String ackDestination = "/user/queue/acks";
        messagingTemplate.convertAndSendToUser(
            senderEmail,
            ackDestination, 
            messageResponse
        );
        log.info("✅ [ACK SENT] Đã gửi xác nhận tin nhắn {} đến người gửi: {}", savedMessage.getId(), senderEmail);

        log.info("✅ [COMPLETE] Tin nhắn {} đã được xử lý hoàn tất cho conversation {}", 
                savedMessage.getId(), conversationId);
    }

    // ================= ⭐ CÁC METHOD MỚI BỔ SUNG ⭐ =================
    
    @Override
    @Transactional(readOnly = true)
    public Page<ConversationListItemResponse> getAllConversations(String userEmail, Pageable pageable) {
        log.info("📋 [GET ALL CONVERSATIONS] User '{}' đang lấy danh sách tất cả conversations", userEmail);
        
        // Kiểm tra quyền truy cập - chỉ admin và support agent được xem tất cả
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));
        
        Set<Role> userRoles = user.getActiveRoles();
        boolean isAuthorized = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);
        
        if (!isAuthorized) {
            throw new AccessDeniedException("Bạn không có quyền xem tất cả cuộc trò chuyện.");
        }
        
        Page<Conversation> conversationPage = conversationRepository.findAllByOrderByUpdatedAtDesc(pageable);
        
        log.info("✅ [GET ALL CONVERSATIONS] Tìm thấy {} conversations cho user '{}'", 
                conversationPage.getTotalElements(), userEmail);
        
        return conversationPage.map(conversationMapper::toConversationListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationListItemResponse> getConversationsByStatus(String status, String userEmail, Pageable pageable) {
        log.info("📋 [GET CONVERSATIONS BY STATUS] User '{}' đang lấy conversations với status '{}'", userEmail, status);
        
        // Kiểm tra quyền truy cập
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));
        
        Set<Role> userRoles = user.getActiveRoles();
        boolean isAuthorized = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);
        
        if (!isAuthorized) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện theo trạng thái.");
        }
        
        Page<Conversation> conversationPage = conversationRepository.findByStatusOrderByUpdatedAtDesc(status, pageable);
        
        log.info("✅ [GET CONVERSATIONS BY STATUS] Tìm thấy {} conversations với status '{}' cho user '{}'", 
                conversationPage.getTotalElements(), status, userEmail);
        
        return conversationPage.map(conversationMapper::toConversationListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationListItemResponse> getMyConversations(String userEmail, Pageable pageable) {
        log.info("📋 [GET MY CONVERSATIONS] Customer '{}' đang lấy danh sách conversations của mình", userEmail);
        
        User customer = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));
        
        // Chỉ khách hàng mới được xem conversations của chính mình
        Set<Role> userRoles = customer.getActiveRoles();
        boolean isCustomer = userRoles.contains(Role.CUSTOMER);
        
        if (!isCustomer) {
            throw new AccessDeniedException("Chỉ khách hàng mới có thể xem danh sách cuộc trò chuyện của mình.");
        }
        
        Page<Conversation> conversationPage = conversationRepository.findByCustomer_IdOrderByUpdatedAtDesc(customer.getId(), pageable);
        
        log.info("✅ [GET MY CONVERSATIONS] Customer '{}' có {} conversations", 
                userEmail, conversationPage.getTotalElements());
        
        return conversationPage.map(conversationMapper::toConversationListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationListItemResponse> getAssignedConversations(String userEmail, Pageable pageable) {
        log.info("📋 [GET ASSIGNED CONVERSATIONS] Agent '{}' đang lấy conversations được assign", userEmail);
        
        User agent = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));
        
        // Chỉ admin và support agent được xem assigned conversations
        Set<Role> userRoles = agent.getActiveRoles();
        boolean isAgent = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);
        
        if (!isAgent) {
            throw new AccessDeniedException("Bạn không có quyền xem cuộc trò chuyện được phân công.");
        }
        
        Page<Conversation> conversationPage = conversationRepository.findByAssignee_IdOrderByUpdatedAtDesc(agent.getId(), pageable);
        
        log.info("✅ [GET ASSIGNED CONVERSATIONS] Agent '{}' được assign {} conversations", 
                userEmail, conversationPage.getTotalElements());
        
        return conversationPage.map(conversationMapper::toConversationListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ConversationListItemResponse> searchConversations(String searchTerm, String userEmail, Pageable pageable) {
        log.info("🔍 [SEARCH CONVERSATIONS] Admin/Support '{}' đang tìm kiếm với từ khóa '{}'", userEmail, searchTerm);
        
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + userEmail));
        
        // Chỉ admin và support agent được search conversations
        Set<Role> userRoles = user.getActiveRoles();
        boolean isAgent = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);
        
        if (!isAgent) {
            throw new AccessDeniedException("Bạn không có quyền tìm kiếm cuộc trò chuyện.");
        }
        
        Page<Conversation> conversationPage = conversationRepository.searchByCustomerNameOrEmail(searchTerm, pageable);
        
        log.info("✅ [SEARCH CONVERSATIONS] Tìm thấy {} conversations với từ khóa '{}'", 
                conversationPage.getTotalElements(), searchTerm);
        
        return conversationPage.map(conversationMapper::toConversationListItem);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getUserConversation(String email) {
        log.info("🔍 [GET USER CONVERSATION] Đang lấy conversationId của user '{}'", email);
        
        // Kiểm tra user tồn tại (throw exception nếu không tìm thấy)
        userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng với email: " + email));
        
        // Lấy conversation mới nhất của user (theo updatedAt DESC)
        Page<Conversation> conversationPage = conversationRepository.findByCustomerEmailOrderByUpdatedAtDesc(
            email, 
            org.springframework.data.domain.PageRequest.of(0, 1)
        );
        
        if (conversationPage.isEmpty()) {
            throw new AppException(ErrorCode.NOT_FOUND, "Người dùng chưa có cuộc trò chuyện nào.");
        }
        
        Conversation conversation = conversationPage.getContent().get(0);
        
        log.info("✅ [GET USER CONVERSATION] Tìm thấy conversationId {} cho user '{}'", 
                conversation.getId(), email);
        
        return ConversationResponse.builder()
                .conversationId(conversation.getId())
                .status(conversation.getStatus())
                .build();
    }

    // ================= ⭐ MARK AS READ FUNCTIONALITY ⭐ =================
    
    @Override
    @Transactional
    public void markConversationAsRead(UUID conversationId, String userEmail) {
        log.info("📖 [MARK READ] Đánh dấu đã đọc conversation '{}' cho user '{}'", conversationId, userEmail);
        
        // Lấy user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User không tồn tại"));
        
        // Lấy conversation và kiểm tra quyền truy cập
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Conversation không tồn tại"));
        
        // Kiểm tra quyền (đồng bộ với getMessagesForConversation)
        boolean isCustomer = conversation.getCustomer().getId().equals(user.getId());
        Set<Role> userRoles = user.getActiveRoles();
        boolean isSupportAgent = userRoles.contains(Role.ADMIN) || userRoles.contains(Role.CUSTOMER_SUPPORT);
        
        if (!isCustomer && !isSupportAgent) {
            throw new AccessDeniedException("Bạn không có quyền truy cập conversation này");
        }
        
        // Cập nhật tất cả tin nhắn chưa đọc
        int updatedCount = messageRepository.markMessagesAsReadByConversation(conversationId, user.getId());
        
        log.info("✅ [MARK READ] Đã đánh dấu {} tin nhắn là đã đọc cho user '{}'", updatedCount, userEmail);
    }
    
    @Override
    @Transactional
    public void markMessageAsRead(UUID messageId, String userEmail) {
        log.info("📖 [MARK READ] Đánh dấu đã đọc message '{}' cho user '{}'", messageId, userEmail);
        
        // Lấy user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User không tồn tại"));
        
        // Lấy message
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Message không tồn tại"));
        
        // Kiểm tra: chỉ receiver mới có thể đánh dấu đã đọc (không thể đánh dấu tin nhắn của chính mình)
        if (message.getSender().getId().equals(user.getId())) {
            log.warn("⚠️ [MARK READ] User '{}' không thể đánh dấu tin nhắn của chính mình là đã đọc", userEmail);
            return;
        }
        
        // Cập nhật
        if (!message.getIsRead()) {
            message.setIsRead(true);
            messageRepository.save(message);
            log.info("✅ [MARK READ] Message '{}' đã được đánh dấu là đã đọc bởi '{}'", messageId, userEmail);
        } else {
            log.debug("ℹ️ [MARK READ] Message '{}' đã được đánh dấu đã đọc trước đó", messageId);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countUnreadMessages(UUID conversationId, String userEmail) {
        log.debug("🔢 [COUNT UNREAD] Đếm tin nhắn chưa đọc trong conversation '{}' cho user '{}'", 
                conversationId, userEmail);
        
        // Lấy user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "User không tồn tại"));
        
        // Đếm
        long count = messageRepository.countUnreadMessages(conversationId, user.getId());
        
        log.debug("✅ [COUNT UNREAD] Có {} tin nhắn chưa đọc", count);
        return count;
    }
}
