FROM registry.access.redhat.com/ubi9 AS ubi-micro-build
RUN mkdir -p /mnt/rootfs
RUN dnf install --installroot /mnt/rootfs curl --releasever 9 --setopt install_weak_deps=false --nodocs -y && \
    dnf --installroot /mnt/rootfs clean all && \
    rpm --root /mnt/rootfs -e --nodeps setup

FROM quay.io/keycloak/keycloak:26.5.3 as builder
COPY --from=ubi-micro-build /mnt/rootfs /

WORKDIR /opt/keycloak

COPY certs/test-raid-org-au.pem .
COPY certs/demo-raid-org-au.pem .
COPY certs/prod-raid-org-au.pem .

RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore
RUN keytool -import -storepass password -noprompt -trustcacerts -alias test.raid.org.au -file test-raid-org-au.pem -keystore conf/server.keystore
RUN keytool -import -storepass password -noprompt -trustcacerts -alias demo.raid.org.au -file test-raid-org-au.pem -keystore conf/server.keystore
RUN keytool -import -storepass password -noprompt -trustcacerts -alias prod.raid.org.au -file test-raid-org-au.pem -keystore conf/server.keystore

# Added this line to copy custom theme into the image
COPY themes/raid-custom /opt/keycloak/themes/raid-custom
COPY themes/ardc-branding /opt/keycloak/themes/ardc-branding

RUN /opt/keycloak/bin/kc.sh build

COPY build/libs/raid-iam.jar /opt/keycloak/providers/

WORKDIR /opt/keycloak

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]