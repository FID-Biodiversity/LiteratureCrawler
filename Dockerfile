FROM openjdk:11-buster

RUN useradd -u 7346 --create-home java \
	&& apt update \
	&& apt install -y maven \
	&& mkdir -p /usr/src/literature-crawler \
	&& chown java /usr/src/literature-crawler

USER java

WORKDIR /usr/src/literature-crawler

COPY . .

RUN mvn package -DskipTests

COPY config/harvesting.yml config/

ENTRYPOINT java -jar /usr/src/literature-crawler/target/LiteratureCrawler.jar
