FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build

# Copy all files from the repository
COPY . .

# Automatically locate pom.xml anywhere in the repo (apna_bank1, apna-bank, etc.) and move it to /build
RUN POM_FILE=$(find . -name "pom.xml" | head -n 1); \
    if [ -n "$POM_FILE" ] && [ "$POM_FILE" != "./pom.xml" ]; then \
        SRC_DIR=$(dirname "$POM_FILE"); \
        echo "Found pom.xml in $SRC_DIR, moving files to root..."; \
        cp -r "$SRC_DIR"/* . ; \
    fi

# Ensure src code folder is in the root for Maven
RUN if [ -d "backend-java/src" ]; then cp -r backend-java/src ./src; fi
RUN if [ ! -d "src" ]; then \
        SRC_DIR=$(find . -type d -name "src" | head -n 1); \
        if [ -n "$SRC_DIR" ] && [ "$SRC_DIR" != "./src" ]; then cp -r "$SRC_DIR" ./src; fi; \
    fi

# Build the Java application
RUN mvn clean package -DskipTests

# Production Alpine Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["java", "-Xmx380m", "-jar", "app.jar"]
