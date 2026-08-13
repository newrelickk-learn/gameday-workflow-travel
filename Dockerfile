FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src/ ./src/
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/travel-*.jar app.jar

# New Relic Java Agentの導入
ADD https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-java.zip /tmp/newrelic-java.zip
RUN apt-get update && apt-get install -y unzip \
    && unzip /tmp/newrelic-java.zip -d /app \
    && rm /tmp/newrelic-java.zip \
    && apt-get remove -y unzip && apt-get autoremove -y && rm -rf /var/lib/apt/lists/*

EXPOSE 8004

ENTRYPOINT ["java", "-javaagent:/app/newrelic/newrelic.jar", "-jar", "app.jar"]
