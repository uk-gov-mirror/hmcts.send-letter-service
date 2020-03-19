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

Before a service can be onboarded, it needs to be set up in [service-auth-provider-app (S2S)](https://github.com/hmcts/service-auth-provider-app)
(see its [README](https://github.com/hmcts/service-auth-provider-app#configuration)).
Once microservice becomes known to S2S, it can be set up in Send Letter Service's [application config](src/main/resources/application.yaml).

### Creating a new folder on the FTP server

Each request to send a letter results in Send Letter Service uploading a zip file to a service-specific folder on the
FTP server that belongs to an external provider which prints and posts physical letters.
In order to enable Send Letter Service to work with a new service, an appropriate folder needs to be created
on the FTP server (both production and test instance). The change needs to be done by the service provider.

### Changes in application.yaml

This section describes the changes that need to be made in [application.yaml](src/main/resources/application.yaml) file.

#### Reports section

Under `reports.service-config` add a new entry that will provide a displayable name for the service.
This is needed for creating reports.

```yaml
reports:
  service-config:
    - service: ...
      ...
    - service: {name of the service in S2S, e.g. divorce}
      display-name: {displayable name of the service, e.g. Divorce}
```

#### FTP section

Under `ftp.service-folders` add a new entry that will provide information about the FTP folder where letters from
the service should be uploaded. Each service should have its own folder assigned.

```yaml
ftp:
  service-folders:
    - service: ...
      ...
    - service: {name of the service in S2S, e.g. divorce}
      folder: {destination folder name, e.g. DIVORCE}
```

**Note!**
This can only be deployed once ftp provider has created folder in all relevant environments.

## Using the service

This section contains some helpful information about talking to Send Letter Service.
Swagger specification for the API can be found [here](https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/send-letter-service.json)

### Sending a letter request

In order to request sending a letter, retrieve a token from [S2S](https://github.com/hmcts/service-auth-provider-app)
and call the send letter endpoint. Here's an example `curl` command for this:

```
curl -X POST -H "Content-Type: application/vnd.uk.gov.hmcts.letter-service.in.letter.v3+json" -H "ServiceAuthorization: Bearer {s2s_token}" "http://rpe-send-letter-service-{env}.service.core-compute-{env}.internal/letters" --data @body.json
```

where
* `env` is the environment name
* `s2s_token` is the service authentication token retrieved from [S2S](https://github.com/hmcts/service-auth-provider-app)
* `body.json` is a file whose content follows the following format:

```
{
  "documents": [
    {
      "content": "{Base64-encoded content of the PDF file}",
      "copies": {number of copies to print, e.g. 2}
    },
    ...
  ],
  "type": "{document type, as agreed between product teams}",
  "additional_data": { ... }
}
```

The order of documents in the envelope will be the same as provided in the request.
`additional_data` is an optional JSON object where services can store any additional information about the letter,
e.g. a corresponding ID in their system.

If your request is successful, you should receive a response containing the ID of the newly created letter:

```
{"letter_id":"f015a4dd-cfa5-4b2b-9fb0-e43ad6ceea35"}
```

A response like this means Send Letter Service has registered the request. The process of posting a letter
is asynchronous and takes days.

### Checking the status of a letter

Once the request to send a letter is accepted, the status of the letter can be checked using the status endpoint.
Here's an example command that sends the request:

```
curl -X GET -H "ServiceAuthorization: Bearer {s2s_token}" "http://rpe-send-letter-service-{env}.service.core-compute-{env}.internal/letters/{id}"
```

where
* `env` is the environment name
* `id` is the ID of the letter in Send Letter Service, as returned by send letter endpoint
* `s2s_token` is the service authentication token retrieved from [S2S](https://github.com/hmcts/service-auth-provider-app)

Here's what a response body should look like:

```
{
    "id": "0001307e-69e5-4ffe-b5f6-1acd73db95bb",
    "status": "Created",
    "checksum": "401b30e3b8b5d629635a5c613cdb7919",
    "created_at": "2020-01-12T01:02:03.123Z",
    "sent_to_print_at": "2020-01-13T01:02:03.123Z",
    "printed_at": "2020-01-14T01:02:03.123Z",
    "has_failed": false
}
```

A letter can be in one of the following statuses:
* `Created` - request has been accepted and is awaiting further processing
* `Uploaded` - Send Letter Service has uploaded the letter to the external provider's system
* `Posted` - letter has been physically posted
* `Aborted` - processing of the letter has been aborted (result of a manual intervention)
* `Skipped` - letter was skipped due to an unrecoverable issue (requires manual action)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
