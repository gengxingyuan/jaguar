# Jaguar dockerfile
FROM ubuntu:16.04
MAINTAINER zhiyifang "zhiyifang123@gmail.com"

# Install JDK Maven && Download Jaguar
WORKDIR /home
RUN apt-get update \
    && apt-get install -y openjdk-8-jdk \
    && apt-get install -y git \
    && apt-get install -y wget \
    && apt-get install -y maven \
    && git clone https://gitlab.com/sdnlab/jaguar.git \
    && wget -q -O - https://raw.githubusercontent.com/opendaylight/odlparent/master/settings.xml > /etc/maven/settings.xml

# Install Jaguar
WORKDIR /home/jaguar
RUN mvn clean install -DskipTests

# Expose ports
EXPOSE 8080
EXPOSE 8181
EXPOSE 8101

# Start jaguar
CMD /home/jaguar/karaf/target/assembly/bin/karaf
