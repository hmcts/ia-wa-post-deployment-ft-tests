ARG APP_INSIGHTS_AGENT_VERSION=3.4.13

# Application image

FROM hmctspublic.azurecr.io/base/java:17-distroless

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/ia-wa-post-deployment-ft-tests.jar /opt/app/

EXPOSE 8888
CMD [ "ia-wa-post-deployment-ft-tests.jar" ]
