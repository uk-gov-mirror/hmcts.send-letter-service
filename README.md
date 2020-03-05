# Send Letter Service

[![Build Status](https://travis-ci.org/hmcts/send-letter-service.svg?branch=master)](https://travis-ci.org/hmcts/send-letter-service)
[![codecov](https://codecov.io/gh/hmcts/send-letter-service/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/send-letter-service)

![Diagram](/doc/arch/diagram.png)

[Swagger API Specs](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/send-letter-service.json)

## Table of Contents

* [Running the application](#running-the-application)
    * [Locally](#locally)
    * [Docker environment](#docker-environment)
* [Onboarding new services](#onboarding-new-services)
* [License](#license)

## Running the application

### Locally

Follow list of required environment variables defined in `./docker/env.list` in order to be able to run application.
Additionally local application will require configured database so add/amend `LETTER_TRACKING_DB_*` variables as needed.
Once everything is set up, run application by simply

```bash
$ ./gradlew bootRun
```

### Docker environment

For convenience there is a sample docker compose configuration file in [docker](docker/docker-compose-sample.yml) folder.
It should be sufficient to run service with single microservice set up: `send_letter_tests`.

```bash
$ docker-compose -f /home/doncem/workspace/hmcts/send-letter-service/docker/docker-compose-sample.yml up
```

Test:

```bash
$ curl http://localhost:8485/health
```

```json
{"status":"UP","details":{"diskSpace":{"status":"UP","details":{"total":62725623808,"free":57405022208,"threshold":10485760}},"db":{"status":"UP","details":{"database":"PostgreSQL","hello":1}},"liveness":{"status":"UP"},"refreshScope":{"status":"UP"},"hystrix":{"status":"UP"}}}
```

```bash
curl -X POST -H "Content-Type: application/json" "http://localhost:4552/lease" -d '{"microservice":"send_letter_tests","oneTimePassword":"OTP"}'

S2S_TOKEN

curl -X POST -H "Content-Type: application/vnd.uk.gov.hmcts.letter-service.in.letter.v2+json" -H "ServiceAuthorization: Bearer S2S_TOKEN" "http://localhost:8485/letters" -d '{"documents":["aGVsbG8="],"type":"BPS001"}'

{"letter_id":"f015a4dd-cfa5-4b2b-9fb0-e43ad6ceea35"}
```

Document provided in sample is not actual document.
It has to be valid PDF.

Swagger spec can be found [here](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/send-letter-service.json).

## Onboarding new services

Services will have to have secrets configured with [service-auth-provider-app](https://github.com/hmcts/service-auth-provider-app).
Once microservice is successfully configured it can be included in [application config](src/main/resources/application.yaml).
There are 2 places: reports and ftp configuration

### Reports

```yaml
reports:
  service-config:
    - service: send_letter_tests
      display-name: Bulk Print # what name to display in the report file
```

### FTP

```yaml
ftp:
  service-folders:
  - service: send_letter_tests
    folder: BULKPRINT
```

**Note!**
This can only be deployed once ftp provider has created folder in all relevant environments.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
