FROM maven:3.9.5-eclipse-temurin-17-alpine AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# Copy source code
COPY src ./src

# Build the application (skip OpenAPI generation, use existing code)
RUN mvn clean package -DskipTests -Dmaven.javadoc.skip=true

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built jar
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
