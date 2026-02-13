# 🚀 HƯỚNG DẪN DEPLOY LÊN KOYEB

## 🔐 Bước 0: Chuẩn bị Environment Variables (LOCAL)

**⚠️ QUAN TRỌNG:** File `.env` chứa secrets và **KHÔNG BAO GIỜ** được commit vào Git!

1. **Copy template:**
   ```bash
   cp .env.example .env
   ```

2. **Điền thông tin thực tế vào `.env`:**
   - Database credentials từ Aiven Console
   - VNPay credentials từ VNPay Dashboard  
   - JWT secret key (generate random)
   - API keys (Google Maps, Gemini, Firebase, etc.)

3. **Verify `.gitignore` đã ignore `.env`:**
   ```bash
   cat .gitignore | grep ".env"
   ```
   Phải thấy: `.env` và `.env.local`

## ✅ Bước 1: Chuẩn bị trên GitHub
- Đảm bảo code đã push lên GitHub (không có `.env` trong repo)
- Đảm bảo có `Dockerfile` và `.dockerignore`

## 🌐 Bước 2: Deploy trên Koyeb

### 1. Tạo Service mới
1. Truy cập [Koyeb Dashboard](https://app.koyeb.com)
2. Click **"Create Service"**
3. Chọn **"Web service"**
4. Chọn **"GitHub"**

### 2. Connect GitHub Repository
1. Click **"Connect, build and deploy your code from a GitHub repository"**
2. Authorize Koyeb với GitHub account của bạn
3. Chọn repository: `letai1854/greenconnect_sever`
4. Branch: `main`

### 3. Configure Build Settings
- **Builder**: Docker
- **Dockerfile path**: `Dockerfile` (mặc định)
- **Build command**: (để trống, Dockerfile tự build)
- **Port**: `8080`
- **Protocol**: HTTP

### 4. Set Environment Variables (QUAN TRỌNG!)
Click **"Environment variables"** và thêm TẤT CẢ biến sau từ file `.env`:

#### 🗄️ Database
```
DB_URL=<YOUR_AIVEN_DB_URL>
DB_USERNAME=<YOUR_DB_USERNAME>
DB_PASSWORD=<YOUR_DB_PASSWORD>
```

**💡 Lấy từ file `.env` local của bạn - KHÔNG COMMIT VÀO GIT!**

#### 💳 VNPay
```
VNPAY_TMN_CODE=<YOUR_VNPAY_TMN_CODE>
VNPAY_HASH_SECRET=<YOUR_VNPAY_HASH_SECRET>
VNPAY_IP_ADDR=127.0.0.1
VNPAY_RETURN_URL=https://cloudgreenconnect.web.app/payment-result
VNPAY_CALLBACK_URL=<URL_KOYEB_CUA_BAN>/orders/vnpay-callback
```
**LẦU Ý:** Sau khi deploy xong, copy URL Koyeb và update `VNPAY_CALLBACK_URL`

#### 🔐 JWT
```
JWT_SIGNER_KEY=<YOUR_JWT_SIGNER_KEY>
```

#### 🤖 AI & APIs
```
GEMINI_API_KEY=<YOUR_GEMINI_API_KEY>
GOOGLE_MAPS_API_KEY=<YOUR_GOOGLE_MAPS_API_KEY>
RECOMBEE_DB_ID=<YOUR_RECOMBEE_DB_ID>
RECOMBEE_PRIVATE_TOKEN=<YOUR_RECOMBEE_TOKEN>
GHTK_API_TOKEN=<YOUR_GHTK_API_TOKEN>
```

#### 🔥 Firebase
```
FIREBASE_BUCKET_NAME=cloudgreenconnect.appspot.com
FIREBASE_WEB_API_KEY=<YOUR_FIREBASE_WEB_API_KEY>
```

#### 🌐 URLs
```
INVITATION_BASE_URL=https://cloudgreenconnect.web.app
```

### 5. Health Check (Optional)
- **Path**: `/` (hoặc `/actuator/health` nếu có actuator)
- **Port**: `8080`
- **Initial delay**: `90` seconds (để app khởi động)
- **Period**: `30` seconds

### 6. Instance Configuration
- **Instance type**: 
  - **Nano** (512 MB RAM) - Free tier, đủ cho development
  - **Small** (1 GB RAM) - Recommended cho production
  - **Medium** (2 GB RAM) - Nếu có nhiều traffic

- **Regions**: Chọn gần người dùng nhất
  - **Frankfurt (fra)** - Châu Âu
  - **Singapore (sin)** - Châu Á
  - **Washington (was)** - Mỹ

### 7. Deploy
1. Kiểm tra lại tất cả cấu hình
2. Click **"Deploy"**
3. Chờ 5-10 phút để build và deploy
4. Theo dõi logs trong tab **"Logs"**

## 🔍 Sau khi Deploy

### 1. Lấy Public URL
- Copy URL từ Koyeb dashboard:
  ```
  https://greenconnect-<random-id>.koyeb.app
  ```

### 2. Update VNPay Callback URL
1. Quay lại **Environment variables**
2. Sửa `VNPAY_CALLBACK_URL`:
   ```
   https://greenconnect-<your-id>.koyeb.app/orders/vnpay-callback
   ```
3. **Redeploy** để áp dụng thay đổi

### 3. Test API
```bash
# Test health
curl https://greenconnect-<your-id>.koyeb.app/

# Test login API
curl -X POST https://greenconnect-<your-id>.koyeb.app/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password"}'
```

### 4. Configure Custom Domain (Optional)
1. Vào **Domains** trong Koyeb
2. Add custom domain (vd: `api.greenconnect.com`)
3. Update DNS records theo hướng dẫn của Koyeb  4. Update `VNPAY_CALLBACK_URL` với domain mới

## ⚠️ LÚU Ý QUAN TRỌNG

### 1. Database Connection
- Aiven MySQL public access phải được bật
- Check firewall rules cho phép Koyeb IPs

### 2. Firebase Service Account
- File `serviceAccountKey.json` cần được add vào project
- Hoặc encode base64 và set vào env var `FIREBASE_CONFIG`

### 3. Build Time
- Lần đầu build có thể mất 5-10 phút (download dependencies)
- Các lần sau nhanh hơn nhờ Docker layer caching

### 4. Logs
- Xem logs real-time trong Koyeb dashboard
- Nếu có lỗi, check:
  - Environment variables đã đầy đủ chưa
  - Database connection có OK không
  - Port 8080 có được expose đúng không

### 5. Cold Start
- Koyeb Free tier có thể sleep sau 1 thời gian không dùng
- Request đầu tiên có thể chậm (cold start)
- Upgrade lên paid plan để tránh sleep

## 🔧 Troubleshooting

### Lỗi: "Application failed to start"
- Check logs: có thể thiếu env vars
- Verify database connection string
- Kiểm tra Java version trong Dockerfile (phải match pom.xml)

### Lỗi: "Port 8080 not accessible"
- Đảm bảo `server.port=8080` trong application.yml
- Dockerfile phải `EXPOSE 8080`
- Koyeb service phải configure port 8080

### Lỗi: "Cannot connect to Aiven MySQL"
- Kiểm tra Aiven MySQL có allow public access không
- Verify connection string có `ssl-mode=REQUIRED`
- Test connection từ local trước

### Build quá lâu
- Lần đầu build sẽ lâu vì download dependencies
- Nếu mỗi lần đều slow, check:  - `.dockerignore` có loại bỏ `target/` không
  - Maven dependencies có được cache không

## 📊 Monitoring

### View Metrics
- CPU, Memory usage trong tab **"Metrics"**
- Request/Response times
- Error rates

### Scaling
- Auto-scaling dựa trên CPU/Memory
- Manual scaling trong **"Instance"** settings

## 💰 Cost Estimation
- **Nano (Free)**: $0/month - 512 MB RAM, 1 vCPU
- **Small**: ~$5-10/month - 1 GB RAM, shared CPU
- **Medium**: ~$15-20/month - 2 GB RAM, dedicated CPU

## 🔄 CI/CD Integration
Koyeb tự động redeploy khi push lên GitHub branch `main`:
1. Commit changes
2. `git push origin main`
3. Koyeb tự động build và deploy
4. Check logs để verify

---

## 🔐 LẤY CREDENTIALS TỪ ĐÂU?

### 🗄️ Database (Aiven MySQL)
1. Truy cập [Aiven Console](https://console.aiven.io/)
2. Vào MySQL service của bạn
3. Tab **"Overview"** → Copy connection info:
   - Host, Port, Username, Password
   - Chọn "Allow public access from internet"

### 💳 VNPay
1. Đăng ký merchant: https://vnpay.vn/dang-ky-merchant
2. Sau khi được duyệt, vào VNPay Dashboard
3. Lấy `TMN_CODE` và `HASH_SECRET`

### 🔐 JWT Secret
- Generate random key: https://generate-random.org/encryption-key-generator
- Hoặc dùng: `openssl rand -base64 64`

### 🤖 Gemini AI
1. Vào [Google AI Studio](https://aistudio.google.com/)
2. Create API Key
3. Copy key

### 🗺️ Google Maps
1. Vào [Google Cloud Console](https://console.cloud.google.com/)
2. Enable Maps JavaScript API, Geocoding API
3. Create credentials → API Key

### 🔥 Firebase
1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Project Settings → Service accounts
3. Generate new private key (download JSON)
4. Copy bucket name từ Storage settings

### 📦 Recombee
1. Vào [Recombee Dashboard](https://www.recombee.com/)
2. Lấy Database ID và Private Token

### 🚚 GHTK
1. Đăng ký tài khoản GHTK
2. Vào dashboard → Lấy API Token

---

## 📞 Support
- Koyeb Docs: https://www.koyeb.com/docs
- Community: https://community.koyeb.com
- Email: support@koyeb.com
###