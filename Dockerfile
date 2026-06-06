# Build + run Spring Boot (multi-module) service
# Expected: `mvn -pl module-server -am package`

FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy pom files first (better Docker layer caching)
COPY pom.xml ./
COPY module-*/*/pom.xml ./
COPY module-*/pom.xml ./

# Copy source
COPY . .

# Build backend jar for module-server (repackages as Spring Boot jar)
RUN mvn -pl module-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

# jar produced by module-server
# (spring-boot-maven-plugin repackage)
COPY --from=builder /app/module-server/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]

