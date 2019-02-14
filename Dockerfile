FROM hmcts/cnp-java-base:openjdk-8u191-jre-alpine3.9-1.0

COPY build/libs/send-letter-service.jar /opt/app/

HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD http_proxy="" wget -q --spider http://localhost:8485/health || exit 1

EXPOSE 8485

CMD ["send-letter-service.jar"]
