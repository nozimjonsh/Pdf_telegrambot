
FROM maven:3.9.9-eclipse-temurin-21 AS build
COPY . .
RUN sed -i 's/<java.version>25<\/java.version>/<java.version>17<\/java.version>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.source>25<\/maven.compiler.source>/<maven.compiler.source>17<\/maven.compiler.source>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.target>25<\/maven.compiler.target>/<maven.compiler.target>17<\/maven.compiler.target>/g' pom.xml || true
RUN mvn clean package -DskipTests

RUN MAIN_PATH=$(grep -rl "public static void main" src/main/java/ | head -n 1) && \
    echo $MAIN_PATH | sed 's|src/main/java/||' | sed 's|\.java||' | tr '/' '.' > target/main-class.txt

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /target/*.jar app.jar
COPY --from=build /target/classes /app/classes
COPY --from=build /target/main-class.txt /app/main-class.txt

ENTRYPOINT ["sh", "-c", "java -cp \"app.jar:/app/classes:$(find /root/.m2/repository -name '*.jar' 2>/dev/null | tr '\\n' ':')\" $(cat /app/main-class.txt)"]
