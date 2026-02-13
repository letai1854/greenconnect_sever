# ========================================
# Stage 1: Build
# ========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml trước để cache dependencies
COPY pom.xml ./

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application (skip tests)
RUN mvn clean package -DskipTests -B

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