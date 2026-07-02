FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . .
RUN sed -i 's/<java.version>25<\/java.version>/<java.version>17<\/java.version>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.source>25<\/maven.compiler.source>/<maven.compiler.source>17<\/maven.compiler.source>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.target>25<\/maven.compiler.target>/<maven.compiler.target>17<\/maven.compiler.target>/g' pom.xml || true

RUN mvn org.apache.maven.plugins:maven-assembly-plugin:3.6.0:single -DdescriptorRef=jar-with-dependencies -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=build /target/*-jar-with-dependencies.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
