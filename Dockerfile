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

# Java orqali parallel ravishda soxta port yaratamiz va botni ishga tushiramiz
ENTRYPOINT ["sh", "-c", "java -cp target/classes:$(mvn dependency:build-classpath | grep -v '\[INFO\]' | tr '\n' ':') -XX:+UseG1GC -Xmx300m -Xms200m -Djline.terminal=jline.UnsupportedTerminal  -jar /usr/share/maven/lib/maven-artifact-*.jar & jshell --execute 'try { java.net.ServerSocket s = new java.net.ServerSocket(8080); while(true) s.accept(); } catch(Exception e) {}' & mvn exec:java -Dexec.mainClass=uz.nozimjon.PdfConverterBot"]
