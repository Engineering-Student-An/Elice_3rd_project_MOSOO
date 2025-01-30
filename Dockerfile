FROM gradle:8.5-jdk17-alpine as builder
WORKDIR /build
# 그래들 캐시를 효율적으로 사용하기 위해 gradle-wrapper를 먼저 복사
COPY gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties /build/gradle/wrapper/

# 그래들 설정 파일을 복사하여 의존성 캐시를 활용
COPY build.gradle settings.gradle /build/
RUN gradle build -x test --parallel --continue > /dev/null 2>&1 || true

# 이후 애플리케이션 소스 코드를 복사
COPY . /build
RUN gradle build -x test --parallel

# 런타임 스테이지
FROM openjdk:17-jdk-slim-buster
WORKDIR /app

# 빌더 이미지에서 jar 파일만 복사
COPY --from=builder /build/build/libs/mosoo-backend-0.0.1-SNAPSHOT.jar .

EXPOSE 8080

# root 대신 nobody 권한으로 실행
USER nobody
ENTRYPOINT [ \
   "java", \
   "-jar", \
   "-Djava.security.egd=file:/dev/./urandom", \
   "-Dsun.net.inetaddr.ttl=0", \
   "mosoo-backend-0.0.1-SNAPSHOT.jar" \
]