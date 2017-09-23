FROM alpine:edge

# install bash
RUN \
  apk add --no-cache bash

# install java
RUN \
  apk add --no-cache openjdk8

# install jq
RUN \
  apk add --no-cache jq

# install zip
RUN \
  apk add --no-cache zip

# install mongodb
RUN \
  apk add --no-cache mongodb

# install mongodb
RUN \
  apk add --no-cache mongodb-tools

RUN \
  mkdir /tmp/hightload

VOLUME ["/tmp/hightload/data"]

WORKDIR /tmp/hightload
RUN \
  mkdir /tmp/data
ADD ./webserver/target/scala-2.12 /tmp/hightload
COPY ./data.zip /tmp/data
RUN \
  mkdir /tmp/unzipped && \
  unzip /tmp/data/data.zip -d /tmp/unzipped; exit 0
RUN \
  mkdir -p /data/db
EXPOSE 80

CMD unzip /tmp/data/data.zip -d /tmp/unzipped && ./process.sh && java -server -Xms3488m -Xmx3488m -jar ./webserver.jar /tmp/data/ /tmp/unzipped

