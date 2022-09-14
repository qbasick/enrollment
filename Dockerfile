FROM openjdk:18
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} disk-api.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/disk-api.jar"]
