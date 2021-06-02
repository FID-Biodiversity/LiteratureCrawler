FROM openjdk:11-buster
COPY . /usr/src/literature-crawler/

WORKDIR /usr/src/literature-crawler
RUN apt update && apt install -y maven && mvn package -DskipTests

ENTRYPOINT java -jar /usr/src/literature-crawler/target/LiteratureCrawler.jar