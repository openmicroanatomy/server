FROM gradle:jdk17-alpine AS build

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build --no-daemon

FROM eclipse-temurin:19.0.1_10-jre-ubi9-minimal AS main

RUN adduser -m -g 0 -u 1000 oma; mkdir /home/oma/server; chown -R oma /home/oma/server; microdnf -y update; microdnf -y install libtiff libxcb libXrender libXext

ADD natives/openslide-3.4.1_2-natives-linux.tar /opt/java/openjdk/lib
COPY --from=build --chown=oma /home/gradle/src/build/libs/*.jar /app/openmicroanatomy-server.jar

USER oma
WORKDIR /home/oma/server

EXPOSE 7777

CMD ["java", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseContainerSupport", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/openmicroanatomy-server.jar"]
