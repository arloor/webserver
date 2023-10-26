FROM docker.io/openjdk:21-jdk
# 设置时区为上海，ubi9-micro内置了tzdata https://catalog.redhat.com/software/containers/ubi9/ubi/615bcf606feffc5384e8452e?container-tabs=packages
COPY cert/ /cert
COPY target/webserver-1.0-SNAPSHOT-all.jar /
CMD ["java","-jar","/webserver-1.0-SNAPSHOT-all.jar"]