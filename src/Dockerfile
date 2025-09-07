FROM openjdk:17-jdk-slim

# Directorio de trabajo
WORKDIR /app

# Copia el JAR generado en /app
COPY build/libs/OldBaker-0.0.1-SNAPSHOT.jar ./app.jar

# Exponer puerto HTTP/HTTPS según tu app
EXPOSE 8443

# Ejecutar la aplicación Java
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
