
FROM openjdk:9

RUN \
  mkdir /tmp/hightload && \
#  mkdir /tmp/data && \
  mkdir /tmp/hightload/data

WORKDIR /tmp/hightload
ADD ./webserver/target/scala-2.12 /tmp/hightload
#COPY ./data.zip /tmp/data
EXPOSE 80

CMD java -server -Xms3488m -Xmx3488m -XX:+UseParallelGC -Xmx3488m -Xms3488m -XX:NewSize=1300m -XX:MaxNewSize=1300m -XX:MaxDirectMemorySize=512m -XX:MaxMetaspaceSize=48m -XX:CompressedClassSpaceSize=48m -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Djava.lang.Integer.IntegerCache.high=11000000 -Xlog:gc* --add-modules java.xml.bind -jar ./webserver.jar /tmp/data/ /tmp/hightLoad/data/

