# Dockerfile
FROM --platform=linux/arm64 gradle:7.6.1-jdk17

WORKDIR /app


ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    TZ=Asia/Seoul

EXPOSE 8080
ENTRYPOINT ["sh","-c","java -Dserver.port=${SERVER_PORT} -Duser.timezone=${TZ} -jar /app/app.jar"]
