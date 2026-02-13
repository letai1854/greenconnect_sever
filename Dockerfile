# ========================================
# Stage 1: Build
# ========================================
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper và pom.xml trước (để cache dependencies)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached layer nếu pom.xml không đổi)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build application (skip tests cho nhanh)
RUN ./mvnw clean package -DskipTests

# ========================================
# Stage 2: Runtime
# ========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Tạo non-root user để tăng security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar file từ builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run application
# Koyeb sẽ tự động health check port 8080
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=prod", \
    "-jar", "app.jar"]
##