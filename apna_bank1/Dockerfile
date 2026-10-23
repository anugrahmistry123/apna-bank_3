FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN if [ -d "backend-java/src" ]; then cp -r backend-java/src ./src; fi
RUN if [ -d "apna-bank/backend-java/src" ]; then cp -r apna-bank/backend-java/src ./src; cp apna-bank/pom.xml .; fi
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-Xmx380m", "-jar", "app.jar"]
