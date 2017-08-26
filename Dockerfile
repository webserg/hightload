

FROM java:8

RUN \
  mkdir /tmp/hightload && \
#  mkdir /tmp/data && \
  mkdir /tmp/hightload/data

WORKDIR /tmp/hightload
ADD ./webserver/target/scala-2.12 /tmp/hightload
#COPY ./data.zip /tmp/data
EXPOSE 80

CMD java -Xms3G -Xmx4G -jar ./webserver.jar

