FROM alpine:latest

RUN apk --no-cache add maven curl tar gzip bash

# Use RUN curl instead of ADD command to allow docker to reuse this layer on subsequent builds
RUN curl "https://download.java.net/java/early_access/alpine/28/binaries/openjdk-11+28_linux-x64-musl_bin.tar.gz" -o /jdk.tar.gz
RUN mkdir -p /opt/jdk && tar xzf /jdk.tar.gz --strip-components=1 -C /opt/jdk
ENV PATH=/opt/jdk/bin:$PATH
ENV JAVA_HOME=/opt/jdk

ADD pom.xml /bug/
WORKDIR /bug
RUN mvn -B install dependency:go-offline
ADD src /bug/src
RUN mvn install dependency:copy-dependencies

CMD ["java", "-cp", "target/nethttpbug-0.1-SNAPSHOT.jar:target/dependency/*", "bug.Bug"]
