FROM maven:3.9.9-eclipse-temurin-21

COPY . .

# Versiyalarni to'g'rilaymiz
RUN sed -i 's/<java.version>25<\/java.version>/<java.version>17<\/java.version>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.source>25<\/maven.compiler.source>/<maven.compiler.source>17<\/maven.compiler.source>/g' pom.xml || true
RUN sed -i 's/<maven.compiler.target>25<\/maven.compiler.target>/<maven.compiler.target>17<\/maven.compiler.target>/g' pom.xml || true

# Loyihani kompilyatsiya qilamiz
RUN mvn clean compile -DskipTests

# Render kutadigan portni ochamiz
EXPOSE 8080

# Botni standart va toza usulda ishga tushiramiz
CMD ["mvn", "exec:java", "-Dexec.mainClass=uz.nozimjon.PdfConverterBot"]
