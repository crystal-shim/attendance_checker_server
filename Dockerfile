# 1. Build Stage
FROM gradle:8.7-jdk20 AS build

# Gradle 캐시 최적화
WORKDIR /app

# 먼저 의존성만 복사해서 캐싱
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle gradle
RUN gradle build --no-daemon || true

# 이후 전체 소스 복사
COPY . .

# Fat jar 빌드
RUN gradle shadowJar --no-daemon

# 2. Runtime Stage
FROM openjdk:20-jdk

WORKDIR /app

# Fat jar만 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 오픈 (Render는 자동 포워딩이라 EXPOSE는 옵션)
EXPOSE 8080

# 서버 실행
CMD ["java", "-jar", "app.jar"]