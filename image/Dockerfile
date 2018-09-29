# Jaguar dockerfile
FROM ubuntu:16.04
MAINTAINER zhiyifang "zhiyifang123@gmail.com"

# ADD (sources.list is opt)
ADD sources.list /etc/apt/sources.list
ADD /target/assembly  /opt/jaguar

# Set ENV
ENV JAVA_MAX_MEM 1G
ENV JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
WORKDIR /opt

# Install JDK Maven && Download Jaguar
RUN apt-get update &&\
    apt-get upgrade -y &&\
    apt-get install -y openjdk-8-jdk nano &&\
    apt-get autoclean &&\
    apt-get autoremove

# Install Jaguar
RUN echo " /opt/jaguar/bin/karaf"  >> /opt/startup &&\
    chmod +x /opt/jaguar/bin/karaf  &&\
    chmod +x /opt/startup

# Expose ports
EXPOSE 8080 8181 8101

# Start jaguar
# Defining a command to be run after the docker is up
ENTRYPOINT ["/opt/startup"]