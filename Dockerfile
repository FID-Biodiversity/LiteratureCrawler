FROM openjdk:11-buster
# OpenJDK 11 LTS support until October 2024
# https://access.redhat.com/articles/1299013

RUN useradd -u 7346 --create-home java \
	&& apt update \
	&& apt install -y maven \
	&& mkdir -p /usr/src/literature-crawler \
	&& chown java /usr/src/literature-crawler

USER java

WORKDIR /usr/src/literature-crawler

COPY --chown=java . ./

RUN mvn package -Dcom.sun.security.enableAIAcaIssuers=true

ENTRYPOINT java -jar /usr/src/literature-crawler/target/LiteratureCrawler.jar
