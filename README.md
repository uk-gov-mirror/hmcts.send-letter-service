# Send Letter Service

[![Build Status](https://travis-ci.org/hmcts/send-letter-service.svg?branch=master)](https://travis-ci.org/hmcts/send-letter-service)
[![codecov](https://codecov.io/gh/hmcts/send-letter-service/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/send-letter-service)

![Diagram](/doc/arch/diagram.png)

[Swagger API Specs](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/send-letter-service.json)

## Running the application

### Locally

Follow list of required environment variables defined in `./docker/env.list` in order to be able to run application.
Additionally local application will require configured database so add/amend `LETTER_TRACKING_DB_*` variables as needed.
Once everything is set up, run application by simply

```bash
$ ./gradlew bootRun
```

### Docker environment

Be sure DB configuration is accommodated for application defaults otherwise - change as needed via `./docker/env.list`.
In case `send-letter-database` does not exist in your docker environment, follow these steps:

```bash
$ docker build -t send-letter-database ./docker/database/

$ docker run -d -i -t --name bsp-send-letter-database --hostname send-letter-database --env-file ./docker/env.list send-letter-database
```

Important note: `--hostname send-letter-database` will be used to link it to service `run` command and also used with default value provided in application config.
There is no need to provide full `--env-file` for the DB container. Only `LETTER_TRACKING_DB_PASSWORD=password` is required

Launch the service from image hosted in Azure Container Registry:

```bash
$ docker run -t -i --name bsp-send-letter-service -p 8485:8485 --link bsp-send-letter-database --env-file ./docker/env.list hmcts/send-letter-service
```

After successful completion of last 2 `run` steps you should be able to see similar output

```bash
$ docker ps -a
```

CONTAINER ID | IMAGE | COMMAND | CREATED | STATUS | PORTS | NAMES
------------ | ----- | ------- | ------- | ------ | ----- | -----
9e3fc00c1824 | hmcts/send-letter-service | "/usr/bin/java -jar …" | 4 minutes ago | Up 4 minutes (healthy) | 0.0.0.0:8485->8485/tcp | bsp-send-letter-service
e68f86e63c93 | send-letter-database | "docker-entrypoint.s…" | 36 minutes ago | Up 36 minutes (healthy) | 5432/tcp | bsp-send-letter-database

Test:

```bash
$ curl http://localhost:8485/health
```
```json
{"status":"UP","details":{"diskSpace":{"status":"UP","details":{"total":62725623808,"free":57405022208,"threshold":10485760}},"db":{"status":"UP","details":{"database":"PostgreSQL","hello":1}},"liveness":{"status":"UP"},"refreshScope":{"status":"UP"},"hystrix":{"status":"UP"}}}
```

## Onboarding new services

Services team need to contact Bulk Scanning and Printing team to onboard their service.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
