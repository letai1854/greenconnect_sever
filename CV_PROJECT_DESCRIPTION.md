# 📋 MÔ TẢ DỰ ÁN CHO CV

## 🌿 GREEN CONNECT - Nền Tảng E-Commerce Thực Phẩm Sạch với AI Chatbot

---

## 📌 Thông Tin Dự Án

| Thuộc Tính | Giá Trị |
|------------|---------|
| **Tên Dự Án** | Green Connect - Agricultural E-commerce Platform |
| **Vai Trò** | Backend Developer / Full-stack Developer |
| **Thời Gian** | 2025 |
| **Loại** | Đồ Án Tốt Nghiệp / Dự Án Thực Tế |
| **Số Entities** | 38+ database tables |
| **Số API Endpoints** | 100+ RESTful APIs |

---

## 🎯 Tổng Quan Dự Án

Phát triển hệ thống **E-commerce Platform** hoàn chỉnh cho lĩnh vực nông sản sạch, tích hợp **AI Chatbot thông minh** với Google Gemini, hỗ trợ **real-time messaging**, thanh toán trực tuyến và hệ thống quản lý đa vai trò.

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

### Backend
| Công Nghệ | Phiên Bản | Mô Tả |
|-----------|-----------|-------|
| **Java** | 17 LTS | Ngôn ngữ chính |
| **Spring Boot** | 3.5.6 | Framework chính |
| **Spring Security** | 6.x | Xác thực & phân quyền |
| **Spring Data JPA** | 3.x | ORM & Repository pattern |
| **Spring WebSocket** | 3.x | Real-time communication |
| **MySQL** | 8.0 | Database chính (Aiven Cloud) |
| **Elasticsearch** | 8.x | Full-text search engine |
| **Redis** | 7.x | Caching & Session management |

### Tích Hợp Bên Thứ Ba (3rd Party Integration)
| Service | Mục Đích |
|---------|----------|
| **Google Gemini AI** | AI Chatbot thông minh |
| **Firebase Cloud Messaging (FCM)** | Push Notifications đa thiết bị |
| **Firebase Storage** | Lưu trữ ảnh sản phẩm |
| **VNPay** | Cổng thanh toán trực tuyến |
| **GHTK (Giao Hàng Tiết Kiệm)** | API tính phí vận chuyển |
| **Google Maps API** | Xác thực địa chỉ giao hàng |
| **Recombee** | Recommendation Engine |
| **Google OAuth 2.0** | Đăng nhập Google |

### DevOps & Tools
| Tool | Mục Đích |
|------|----------|
| **Maven** | Build & Dependency Management |
| **Flyway** | Database Migration |
| **Lombok** | Boilerplate code reduction |
| **ModelMapper** | DTO - Entity mapping |
| **Apache POI** | Excel import/export |
| **Testcontainers** | Integration Testing |

---

## ⭐ Tính Năng Chính Đã Phát Triển

### 1. 🤖 AI Chatbot với Google Gemini
- **Xử lý ngôn ngữ tự nhiên (NLP)** để tìm kiếm sản phẩm
- **Tư vấn công thức nấu ăn** và gợi ý nguyên liệu tự động
- **Context-aware conversation**: Lưu lịch sử chat, load 10 tin nhắn gần nhất làm context
- **Smart intent detection**: Phát hiện ý định người dùng (NEW/HOT/SALE/FEATURED/HIGH_RATED)
- **Product recommendation**: Kết hợp Elasticsearch (70%) + ProductService (30%)
- **Session management**: Mỗi user có session riêng, lưu trữ vĩnh viễn

### 2. 💬 Real-time Messaging với WebSocket
- **STOMP protocol** over WebSocket với SockJS fallback
- **Private channels**: Kênh riêng cho từng cuộc hội thoại
- **JWT authentication** cho WebSocket connections
- **Heartbeat mechanism**: Giữ kết nối sống (25s interval)
- **Message status tracking**: Đã gửi/Đã nhận/Đã đọc
- **Typing indicators**: Thông báo đang nhập

### 3. 🔐 Authentication & Authorization
- **JWT-based stateless authentication** (Access + Refresh Token)
- **Multi-role system**: CUSTOMER, ADMIN, ORDER_MANAGER, PRODUCT_MANAGER, MARKETING_MANAGER, CUSTOMER_SUPPORT, SHIPPER
- **Google OAuth 2.0** đăng nhập/đăng ký
- **Two-Factor Authentication (2FA)** với OTP qua email
- **Password reset flow** với OTP verification
- **Token rotation**: Refresh token tự động gia hạn
- **Device management**: Hỗ trợ đăng nhập đa thiết bị (max 20)

### 4. 🔔 Push Notification System
- **Firebase Cloud Messaging (FCM)** multi-device
- **Device type classification**: Android, iOS, Web
- **Token lifecycle management**: Auto-update, deactivation
- **Targeted notifications**: Gửi theo user/role/topic
- **Notification history**: Lưu trữ và query trong database

### 5. 🔍 Advanced Search với Elasticsearch
- **Full-text search** với tiếng Việt
- **Multi-field filtering**: Category, Supplier, Price range, Stock status
- **Faceted search**: Aggregate theo nhiều tiêu chí
- **Sort options**: 8 kiểu sắp xếp (price, rating, sold, newest...)
- **Real-time sync**: MySQL → Elasticsearch tự động

### 6. 💳 Payment Integration
- **VNPay gateway** integration
- **IPN (Instant Payment Notification)** handling
- **Payment verification** với secure hash
- **Order status management**: Auto-update sau thanh toán
- **Refund processing**: Hoàn tiền tự động

### 7. 📊 Analytics Dashboard
- **Revenue analytics**: Daily, weekly, monthly reports
- **Comparison stats**: So sánh tuần này vs tuần trước
- **Top products/categories/suppliers** reports
- **Customer analytics**: Phân bổ theo tỉnh, khách VIP
- **Cache strategy**: 30-phút cache với rate limiting

### 8. 🛒 E-commerce Core Features
- **Product management**: CRUD với variants, images, certifications
- **Category hierarchy**: Danh mục đa cấp
- **Cart system**: Persistent cart với variants
- **Order workflow**: 7 trạng thái từ CHO_XAC_NHAN → DA_GIAO
- **Review & Rating**: Đánh giá với media uploads
- **Voucher system**: Discount codes với điều kiện áp dụng
- **Promotion campaigns**: Flash sale, bulk purchase
- **Favorites**: Wishlist với Elasticsearch sync

### 9. 🚚 Shipping Integration
- **GHTK API**: Tính phí vận chuyển real-time
- **Google Maps API**: Xác thực địa chỉ Việt Nam
- **Address management**: Nhiều địa chỉ giao hàng per user
- **Delivery options**: Cấu hình phương thức giao hàng

### 10. 🤝 AI Recommendation Engine
- **Recombee integration**: Personalized recommendations
- **User behavior tracking**: View, purchase, cart actions
- **Collaborative filtering**: "Khách hàng cũng xem..."
- **Content-based filtering**: Sản phẩm tương tự

---

## 🏗️ Kiến Trúc Hệ Thống

### Clean Architecture Pattern
```
├── controllers/          # REST API endpoints (34 controllers)
├── services/             # Business logic layer (43 services)
│   └── impl/            # Service implementations
├── repositories/         # Data access layer (JPA + Custom)
├── entities/             # Database entities (51 entities)
├── dto/                  # Data Transfer Objects
│   ├── request/         # Request DTOs
│   └── response/        # Response DTOs
├── config/               # Configuration classes (19 configs)
├── security/             # JWT filters, UserDetails
├── websocket/            # WebSocket configuration
├── elasticsearch/        # ES documents & repositories
├── exceptions/           # Custom exceptions & handlers
├── mappers/              # Entity-DTO mappers
├── utils/                # Utility classes
└── scheduler/            # Scheduled tasks
```

### Database Design
- **38+ tables** với quan hệ phức tạp
- **Indexing optimization**: 50+ custom indexes
- **Soft delete pattern**: isActive flags
- **Audit trail**: createdAt, updatedAt timestamps
- **UUID primary keys**: Distributed-friendly

---

## 📈 Kỹ Thuật Nổi Bật

### Performance Optimization
- ✅ **Database Indexing**: Composite indexes cho high-frequency queries
- ✅ **Caching Strategy**: Redis + In-memory caching
- ✅ **Pagination**: Cursor-based và offset pagination
- ✅ **Lazy Loading**: Hibernate fetch optimization
- ✅ **Connection Pooling**: HikariCP configuration

### Security Best Practices
- ✅ **OWASP compliance**: Input validation, SQL injection prevention
- ✅ **Password hashing**: BCrypt với salt
- ✅ **Token security**: Short-lived access + Long-lived refresh
- ✅ **CORS configuration**: Whitelist-based
- ✅ **Rate limiting**: API throttling
- ✅ **Secrets management**: Environment variables (.env)

### Code Quality
- ✅ **SOLID principles**: Single responsibility, Dependency injection
- ✅ **Design patterns**: Repository, Service, Factory, Builder
- ✅ **Exception handling**: Global exception handler với custom error codes
- ✅ **API standardization**: Consistent response format với ApiResponse wrapper
- ✅ **Documentation**: OpenAPI/Swagger specs

### Scalability Considerations
- ✅ **Stateless design**: JWT-based, no server sessions
- ✅ **Cloud-ready**: Aiven MySQL, Elastic Cloud, Firebase
- ✅ **Async processing**: @Async methods cho heavy operations
- ✅ **Event-driven**: ApplicationEvent cho decoupling

---

## 📊 Số Liệu Dự Án

| Metric | Giá Trị |
|--------|---------|
| Lines of Code | ~30,000+ |
| Database Tables | 38+ |
| REST Endpoints | 100+ |
| Services | 43+ |
| Controllers | 34+ |
| DTOs | 80+ |
| Unit Tests | Testcontainers |

---

## 🎓 Kỹ Năng Thu Được

### Technical Skills
- [x] RESTful API Design & Development
- [x] Spring Boot Ecosystem (Security, Data, WebSocket)
- [x] Database Design & Optimization (MySQL, Indexing)
- [x] Search Engine Integration (Elasticsearch)
- [x] AI/ML Integration (Google Gemini, Recombee)
- [x] Real-time Communication (WebSocket, STOMP)
- [x] Payment Gateway Integration (VNPay)
- [x] Cloud Services (Firebase, Aiven, Elastic Cloud)
- [x] Authentication/Authorization (JWT, OAuth2, 2FA)

### Soft Skills
- [x] System Design & Architecture
- [x] API Documentation
- [x] Problem Solving
- [x] Performance Optimization
- [x] Security Awareness

---

## 📝 Mô Tả Ngắn Cho CV (Copy-paste ready)

### Phiên Bản Đầy Đủ (150-200 từ)
> Phát triển hệ thống E-commerce Platform cho nông sản sạch sử dụng **Spring Boot 3.5**, **MySQL**, **Elasticsearch** và **Redis**. Tích hợp **AI Chatbot với Google Gemini** để tư vấn sản phẩm qua ngôn ngữ tự nhiên. Xây dựng hệ thống **real-time messaging** sử dụng **WebSocket/STOMP** với kênh riêng cho mỗi cuộc hội thoại. Triển khai xác thực **JWT + OAuth2 Google** với phân quyền 7 vai trò. Tích hợp **VNPay** cho thanh toán, **Firebase Cloud Messaging** cho push notifications đa thiết bị, và **GHTK API** cho tính phí vận chuyển. Thiết kế database 38+ tables với tối ưu indexing. Xây dựng **Dashboard Analytics** với caching strategy và rate limiting.

### Phiên Bản Ngắn Gọn (80-100 từ)
> Backend Developer cho nền tảng E-commerce nông sản sạch. Sử dụng **Spring Boot 3.5, MySQL, Elasticsearch, Redis**. Tích hợp **AI Chatbot (Google Gemini)**, **real-time chat (WebSocket)**, **thanh toán VNPay**, **push notification (FCM)**. Xây dựng xác thực **JWT/OAuth2** đa vai trò, **advanced search** với Elasticsearch, **analytics dashboard** với caching. Database 38+ tables, 100+ REST APIs.

### Bullet Points Cho CV
- 🔹 Phát triển RESTful API với Spring Boot 3.5, MySQL, Elasticsearch
- 🔹 Tích hợp AI Chatbot (Google Gemini) với context-aware conversation
- 🔹 Xây dựng real-time messaging với WebSocket/STOMP protocol
- 🔹 Triển khai xác thực JWT + Google OAuth2 với 7 vai trò phân quyền
- 🔹 Tích hợp cổng thanh toán VNPay với IPN handling
- 🔹 Thiết kế database 38+ tables với query optimization
- 🔹 Xây dựng Analytics Dashboard với caching strategy

---

## 🔗 Liên Kết

- **GitHub Repository**: [Link to repository]
- **API Documentation**: [Swagger/OpenAPI link]
- **Demo Video**: [Link to demo]

---

*Cập nhật lần cuối: Tháng 1/2026*
