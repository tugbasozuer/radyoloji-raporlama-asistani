# ---------- 1. Asama: derleme (Maven + JDK 21) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Once yalnizca pom.xml kopyalanir ve bagimliliklar indirilir. Kaynak kod degisince bu katman
# yeniden kullanilir (Docker katman onbellegi); bagimliliklar her seferinde yeniden inmez.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Sonra kaynak kod kopyalanir ve jar uretilir. Testler burada calistirilmaz:
# Testcontainers Docker gerektirir; testler CI adiminda (GitHub Actions) calisacak.
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- 2. Asama: calisma (yalnizca JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Uygulama root yerine yetkisiz bir kullanici ile calisir.
RUN useradd --system --create-home appuser
USER appuser

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
