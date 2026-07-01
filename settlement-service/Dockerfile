FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY payment-common/pom.xml payment-common/
COPY payment-service/pom.xml payment-service/
COPY ledger-service/pom.xml ledger-service/
COPY settlement-service/pom.xml settlement-service/
COPY gateway-service/pom.xml gateway-service/
RUN ./mvnw dependency:go-offline -pl settlement-service -am
COPY payment-common/src payment-common/src
COPY settlement-service/src settlement-service/src
RUN ./mvnw clean package -pl settlement-service -am -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/settlement-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
