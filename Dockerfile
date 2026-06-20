FROM eclipse-temurin:25-jre
WORKDIR /app
ARG RELEASE_VERSION
ADD https://github.com/APDevTeam/ModMail/releases/download/${RELEASE_VERSION}/ModMail_${RELEASE_VERSION}.jar ModMail.jar
CMD ["java", "-jar", "ModMail.jar"]
