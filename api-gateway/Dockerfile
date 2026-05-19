FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
RUN apk add --no-cache maven
COPY pom.xml ./
COPY llm-service/pom.xml llm-service/
COPY plm-service/pom.xml plm-service/
COPY qlm-service/pom.xml qlm-service/
COPY api-gateway/pom.xml api-gateway/
COPY analytics-service/pom.xml analytics-service/
RUN mvn dependency:go-offline -pl api-gateway -am -q 2>/dev/null || true
COPY api-gateway/src api-gateway/src
RUN mvn package -pl api-gateway -am -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=builder /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
