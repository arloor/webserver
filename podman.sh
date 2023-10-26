#! /bin/sh

mvn clean package
podman login docker.io
podman build ./ -t docker.io/arloor/jetty
podman push docker.io/arloor/jetty
podman run \
-it --rm \
--name server \
--network=host \
-v /usr/share/nginx/html/blog:/blog \
-v /root/.acme.sh/arloor.dev:/arloor.dev \
docker.io/arloor/jetty:latest \
java -Dcontent.path=/blog  -Dudp.port=555 -Dtcp.port=555 -Dkeystore.path=/arloor.dev/arloor.dev.pfx  -jar /webserver-1.0-SNAPSHOT-all.jar