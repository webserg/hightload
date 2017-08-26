#
# Scala with sbt based on lightweight java docker image
#
# Manual sbt install is based on:
# http://www.scala-sbt.org/0.13/tutorial/Manual-Installation.html

FROM denvazh/java:openjdk8-jdk

# Versions
ENV SCALA_VERSION 2.12.2
ENV SBT_VERSION 0.13.15

# Install scala and sbt
RUN \
  echo 'Installing scala...' && \
  wget "http://www.scala-lang.org/files/archive/scala-$SCALA_VERSION.tgz" && \
  tar xzf scala-$SCALA_VERSION.tgz -C /tmp/ && \
  mv /tmp/scala-$SCALA_VERSION/* /usr/local/ && \
  rm -rf scala-$SCALA_VERSION.tgz && \
  echo 'Installing sbt...' && \
  wget "https://dl.bintray.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/$SBT_VERSION/sbt-launch.jar" -P /usr/local/bin/ && \
  echo '#!/bin/bash' > /usr/local/bin/sbt && \
  echo 'SBT_OPTS="-Xms512M -Xmx1G -Xss1M -XX:+CMSClassUnloadingEnabled"' >> /usr/local/bin/sbt && \
  echo 'java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"' >> /usr/local/bin/sbt && \
  chmod u+x /usr/local/bin/sbt && \
  echo 'Fetching all sbt related dependencies...' && \
  mkdir /tmp/hightload && \
#  mkdir /tmp/data && \
  mkdir /tmp/hightload/data

WORKDIR /tmp/hightload
ADD ./webserver/target/scala-2.12 /tmp/hightload
#COPY ./data.zip /tmp/data
EXPOSE 80

# Set scala home for current installation 
ENV SCALA_HOME /usr/local

CMD java -Xms3G -Xmx4G -jar ./webserver.jar

