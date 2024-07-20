FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
COPY Credentials/object-detection-429915-3d479335d183.json Credentials/object-detection-429915-3d479335d183.json
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

