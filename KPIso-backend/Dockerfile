# Etapa 1: Compilación del proyecto
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copiar el archivo de configuración de dependencias para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar el código fuente y compilar el binario ejecutable
COPY src ./src
RUN mvn clean package -DskipTests=true

# Etapa 2: Entorno de ejecución optimizado
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar únicamente el archivo compilado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Exponer el puerto configurado en application.properties
EXPOSE 8080

# Comando de arranque de la API
ENTRYPOINT ["java", "-jar", "app.jar"]