# Jaguar dockerfile
FROM ubuntu:16.04
MAINTAINER zhiyifang "zhiyifang123@gmail.com"

# ADD (sources.list is opt)
ADD sources.list /etc/apt/sources.list

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

# Download Jaguar
RUN wget http://repo1.maven.org/maven2/com/sdnlab/jaguar-karaf/0.1.0/jaguar-karaf-0.1.0.tar.gz &&\
    tar -zxf jaguar-karaf-0.1.0.tar.gz -C /opt/ &&\
    mv /opt/jaguar-karaf-0.1.0/ /opt/jaguar/

# Install Jaguar
RUN echo " /opt/jaguar/bin/karaf"  >> /opt/startup &&\
    chmod +x /opt/jaguar/bin/karaf  &&\
    chmod +x /opt/startup

# Expose ports
EXPOSE 8080 8181 8101

# Start jaguar
# Defining a command to be run after the docker is up
ENTRYPOINT ["/opt/startup"]
