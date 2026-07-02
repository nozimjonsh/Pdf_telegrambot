FROM maven:3.9.9-eclipse-temurin-21

COPY . .

RUN sed -i 's/<java.version>25<\/java.version>/<java.version>17<\/java.version>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.source>25<\/maven.compiler.source>/<maven.compiler.source>17<\/maven.compiler.source>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.target>25<\/maven.compiler.target>/<maven.compiler.target>17<\/maven.compiler.target>/g' pom.xml || true

RUN mvn clean compile -DskipTests

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "mvn exec:java -Dexec.mainClass=uz.nozimjon.PdfConverterBot & nc -lkp 8080"]
