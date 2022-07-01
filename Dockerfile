FROM openjdk:11-jre

RUN groupadd -g 1000 zkr \
 && useradd -r -m -u 1000 -g zkr zkr \
 && mkdir -p /opt/zkr

COPY build/libs/zkr-all.jar /opt/zkr/zkr-all.jar

RUN chown -hR zkr:zkr /opt/zkr

WORKDIR /opt/zkr
USER zkr:zkr
