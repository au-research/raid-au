#FROM raid-api-base:latest
#
#EXPOSE 5005
#
## Needed to perform health check as curl is not available in container
## and brings a load of vulnerabilities with it
#COPY ./scripts/Status.class /opt/

FROM eclipse-temurin:17-alpine

COPY build/libs/raido-api-svc.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]