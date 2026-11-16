# ==============================================================================
# Dockerfile Multi-stage para Spring WebFlux Backend
# Genera una imagen ligera basada en Eclipse Temurin JRE 17 Alpine
# ==============================================================================

# Etapa 1: Compilación con Maven
FROM maven:3.9.6-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copiar descriptor de dependencias
COPY pom.xml .

# Descargar dependencias en caché
RUN mvn dependency:go-offline -B || true

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen de ejecución ligera
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Crear usuario sin privilegios para ejecución segura
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copiar artefacto generado
COPY --from=builder /build/target/*.jar app.jar

# Exponer el puerto configurado por defecto
EXPOSE 8080

# Parámetros JVM optimizados para contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]

