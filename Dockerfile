FROM openjdk:8u171-alpine3.8

ENV SCALA_VERSION 2.12.7
ENV SBT_VERSION 1.2.8
ENV MM_CERT_BUNDLE "/etc/ssl/certs/mm-cert-bundle.jks"
ENV SP_TRUSTSTORE "/etc/ssl/certs/truststore.jks"

ENV PATH /sbt/bin:$PATH

RUN apk add -U bash docker

ADD https://artifactory.awsmgmt.massmutual.com/artifactory/mm-certificates/mm-cert-bundle.jks $MM_CERT_BUNDLE
RUN chmod 777 $MM_CERT_BUNDLE

# Install SBT
RUN wget --no-check-certificate https://github.com/sbt/sbt/releases/download/v1.2.8/sbt-$SBT_VERSION.tgz && \
  tar -xzvf sbt-$SBT_VERSION.tgz && \
  sbt -Djavax.net.ssl.trustStore=$MM_CERT_BUNDLE sbtVersion

# add curl to it
RUN apk add --update \
    curl \
    && rm -rf /var/cache/apk/*

MAINTAINER SP Engineers <StreamingPlatform@iuo.MassMutual.com>
LABEL comments="Connect Manager"

WORKDIR /app

# Copy the current directory contents into the container at /app
ADD ./build.sbt /app
ADD ./src /app/src
ADD ./project /app/project
ADD ./truststore.jks /app
ADD ./truststore.jks $SP_TRUSTSTORE

RUN chmod 777 $SP_TRUSTSTORE && \
    chmod 777 $MM_CERT_BUNDLE && \
    cp $MM_CERT_BUNDLE /app

#create a java env variable file that sbt will read in before assembly
RUN echo '-Djavax.net.ssl.trustStore=$MM_CERT_BUNDLE'> /app/.jvmopts

# run clean assembly for app
RUN sbt -Djavax.net.ssl.trustStore=$MM_CERT_BUNDLE clean assembly

# Make port 80 available to the world outside this container
EXPOSE 8080

ADD ./entrypoint.sh /app

RUN chmod +x ./entrypoint.sh

VOLUME /etc/kafka/secrets

CMD ["/app/entrypoint.sh"]