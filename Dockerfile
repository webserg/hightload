FROM alpine:edge

# install bash
RUN \
  apk add --no-cache bash

# install java
RUN \
  apk add --no-cache openjdk9

# install mongodb
RUN \
  echo '@testing http://dl-4.alpinelinux.org/alpine/edge/testing' >> /etc/apk/repositories && \
  apk add --no-cache mongodb@testing && \
  rm /usr/bin/mongosniff /usr/bin/mongoperf

VOLUME ["/data/db"]

RUN \
  mkdir /tmp/hightload && \
  mkdir /tmp/data && \
  mkdir /tmp/hightload/data && \
  unzip /tmp/data/data.zip -d /tmp/hightload/data


WORKDIR /tmp/hightload
ADD ./webserver/target/scala-2.12 /tmp/hightload
COPY ./data.zip /tmp/data
RUN \
    process.sh
EXPOSE 80

CMD java -server -Xms3488m -Xmx3488m -XX:+UseParallelGC -XX:NewSize=1300m -XX:MaxNewSize=1300m -XX:MaxDirectMemorySize=512m -XX:MaxMetaspaceSize=48m -XX:CompressedClassSpaceSize=48m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Djava.lang.Integer.IntegerCache.high=11000000 -Xlog:gc* --add-modules java.xml.bind -jar ./webserver.jar /tmp/data/ /tmp/hightLoad/data/

