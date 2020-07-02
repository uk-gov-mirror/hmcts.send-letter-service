ARG APP_INSIGHTS_AGENT_VERSION=2.5.1

# Build image

FROM busybox:1 as downloader

RUN wget -P /tmp https://github.com/microsoft/ApplicationInsights-Java/releases/download/2.5.1/applicationinsights-agent-2.5.1.jar

# Application image

FROM hmctspublic.azurecr.io/base/java:openjdk-8-distroless-1.4

COPY --from=downloader /tmp/applicationinsights-agent-${APP_INSIGHTS_AGENT_VERSION}.jar /opt/app/

COPY lib/AI-Agent.xml /opt/app/
COPY build/libs/send-letter-service.jar /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:8485/health || exit 1

EXPOSE 8485

CMD ["send-letter-service.jar"]
