# Multi-stage build: compile once, run lightweight
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
# Build project and copy runtime dependencies
RUN mvn -q -DskipTests clean package \
 && mvn -q -DskipTests dependency:copy-dependencies -DoutputDirectory=target/dependency

FROM eclipse-temurin:17-jre
WORKDIR /app
ENV JAVA_MAIN=com.ass1.server.Proxy
ENV JVM_OPTS=""
ENV JAVA_ARGS=""
COPY --from=build /workspace/target/solution-1.0-SNAPSHOT.jar ./app.jar
COPY --from=build /workspace/target/dependency/ ./lib/
# Default entry: run the class in JAVA_MAIN with classpath of app + deps
ENTRYPOINT ["sh","-lc","exec java $JVM_OPTS -cp app.jar:lib/* $JAVA_MAIN $JAVA_ARGS"]
