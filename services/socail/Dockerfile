# from openjdk:8u242-jdk
from openjdk:13.0-jdk-buster





COPY gradle /opt/app/gradle
COPY gradlew      /opt/app/gradlew
COPY build.gradle /opt/app/build.gradle

WORKDIR /opt/app

# download and resolve deps
RUN ./gradlew assemble --no-daemon

COPY build.gradle /opt/app/build.gradle
COPY src          /opt/app/src


RUN ./gradlew installDist --no-daemon

COPY config.properties          /opt/app/config.properties


CMD bash -c 'set -x; /opt/app/build/install/app/bin/app  /opt/app/config.properties'

