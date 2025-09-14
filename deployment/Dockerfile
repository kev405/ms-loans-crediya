# ==========================================================
# ETAPA 1: BUILD - Usa una imagen con JDK y Gradle para compilar
# ==========================================================
FROM gradle:8-jdk21 AS build

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /home/gradle/src

# Copia todo el código fuente de tu proyecto al contenedor
COPY . .

# Ejecuta el comando de build de Gradle para generar el JAR
# Usamos --no-daemon que es una buena práctica en contenedores
# El modificador -x test omite la ejecución de pruebas para acelerar el build
RUN gradle build --no-daemon -x test


# ==========================================================
# ETAPA 2: RUNTIME - Usa una imagen ligera solo con JRE
# ==========================================================
FROM eclipse-temurin:21-jre-jammy AS final

WORKDIR /app

# Copia ÚNICAMENTE el JAR generado desde la etapa 'build'
# Este comando es la clave de la compilación multi-etapa.
# Renombra directamente el JAR a app.jar, simplificando el proceso.
COPY --from=build /home/gradle/src/applications/app-service/build/libs/*.jar /app/app.jar

# Expone el puerto que usará la aplicación
EXPOSE 8081

# Configura las opciones de la JVM para el arranque
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

# Comando final para ejecutar la aplicación
ENTRYPOINT ["java","-jar","/app/app.jar"]