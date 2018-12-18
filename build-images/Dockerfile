FROM openjdk:8u171-jdk-slim-stretch

ENV SBT_VERSION 1.2.6

RUN apt-get update
RUN apt-get install -y curl git python-pip

RUN pip install awscli --ignore-installed six

RUN curl -L -o sbt-$SBT_VERSION.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb
RUN dpkg -i sbt-$SBT_VERSION.deb
RUN rm sbt-$SBT_VERSION.deb

RUN sbt sbtVersion
