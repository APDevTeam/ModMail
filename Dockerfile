FROM gradle:9.5-jdk25 AS build
WORKDIR /app
COPY . .
RUN gradle shadowJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/build/libs/ModMail.jar .
CMD ["java", "-jar", "ModMail.jar"]