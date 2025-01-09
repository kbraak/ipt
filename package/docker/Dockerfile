FROM maven:3.8.8-eclipse-temurin-11 AS builder

ARG GIT_REVISION=docker

# Use GBIF's Maven mirror
RUN mkdir -p /root/.m2
COPY package/docker/m2-settings.xml /root/.m2/settings.xml

WORKDIR /usr/src/ipt
COPY pom.xml .
COPY src src
# Set the build number (shown in the footer) from the provided Git revision
RUN perl -pi -e 's(\${buildNumber})('${GIT_REVISION}')' src/main/resources/application.properties && \
    mvn -DskipTests install && \
    rm target/*.war && mv target/ipt-* target/ipt

FROM tomcat:9.0-jdk17@sha256:c7f5e6a8c40fdba9804f3c49bded2d2750c01d36380ecad872fb54c8182b544b
LABEL maintainers="Matthew Blissett <mblissett@gbif.org>"

ARG IPT_NAME=ROOT

ENV IPT_DATA_DIR=/srv/ipt

COPY --from=builder /usr/src/ipt/target/ipt /usr/local/tomcat/webapps/${IPT_NAME}

# Modify maxParameterCount in server.xml
RUN sed -i 's/maxParameterCount="1000"/maxParameterCount="10000"/g' /usr/local/tomcat/conf/server.xml

VOLUME /srv/ipt

EXPOSE 8080
CMD ["catalina.sh", "run"]

# Example for customizing the IPT in the Docker build
# (Many approaches are possible, such as changing the CSS, about.ftl etc).
#RUN curl -LSsfo /usr/local/tomcat/webapps/${IPT_NAME}/images/BID-v2.png https://cloud.gbif.org/bid/images/BID-v2.png \
#    && perl -pi -e 's[GBIF-2015-standard-ipt.png][BID-v2.png]' /usr/local/tomcat/webapps/${IPT_NAME}/WEB-INF/pages/inc/menu.ftl
