#!/usr/bin/env sh

print_help() {
  echo "Script to run docker containers for Spring Boot Template API service

  Usage:

  ./run-in-docker.sh [OPTIONS]

  Options:
    --clean, -c                         Clean and install current state of source code
    --install, -i                       Install current state of source code
    --with-flyway, -f                   Run docker compose with flyway enabled
    --param PARAM=, -p PARAM=           Parse script parameter
    --help, -h                          Print this help block

  Available parameters:
    APPINSIGHTS_INSTRUMENTATIONKEY      Defaults to '00000000-0000-0000-0000-000000000000'
    FTP_FINGERPRINT                     Defaults to 'fingerprint'
    FTP_HOSTNAME                        Defaults to 'hostname'
    FTP_PORT                            Defaults to '22'
    FTP_PRIVATE_KEY                     Defaults to 'private'
    FTP_PUBLIC_KEY                      Defaults to 'public'
    FTP_REPORTS_FOLDER                  Defaults to '/reports/'
    FTP_TARGET_FOLDER                   Defaults to '/target/'
    FTP_USER                            Defaults to 'user'
    LETTER_TRACKING_DB_PASSWORD         Defaults to 'password'
    S2S_URL                             Defaults to 'false' - disables health check
    SCHEDULING_ENABLED                  Defaults to 'false'
    ENCRYPTION_ENABLED                  Defaults to 'false'
    ENCRYPTION_PUBLIC_KEY               Defaults to 'public'
  "
}

# script execution flags
GRADLE_CLEAN=false
GRADLE_INSTALL=false
FLYWAY_ENABLED=false

# environment variables
APPINSIGHTS_INSTRUMENTATIONKEY="00000000-0000-0000-0000-000000000000"
FTP_FINGERPRINT="fingerprint"
FTP_HOSTNAME="hostname"
FTP_PORT=22
FTP_PRIVATE_KEY="private"
FTP_PUBLIC_KEY="public"
FTP_REPORTS_FOLDER="/reports/"
FTP_TARGET_FOLDER="/target/"
FTP_USER="user"
LETTER_TRACKING_DB_PASSWORD="password"
S2S_URL=false
SCHEDULING_ENABLED=false
ENCRYPTION_ENABLED=false
ENCRYPTION_PUBLIC_KEY="public"

execute_script() {
  cd $(dirname "$0")/..

  if [ ${GRADLE_CLEAN} = true ]
  then
    echo "Clearing previous build.."
    ./gradlew clean
  fi

  if [ ${GRADLE_INSTALL} = true ]
  then
    echo "Assembling distribution.."
    ./gradlew assemble
  fi

  echo "Assigning environment variables.."

  export APPINSIGHTS_INSTRUMENTATIONKEY=${APPINSIGHTS_INSTRUMENTATIONKEY}
  export FTP_FINGERPRINT=${FTP_FINGERPRINT}
  export FTP_HOSTNAME=${FTP_HOSTNAME}
  export FTP_PORT=${FTP_PORT}
  export FTP_PRIVATE_KEY=${FTP_PRIVATE_KEY}
  export FTP_PUBLIC_KEY=${FTP_PUBLIC_KEY}
  export FTP_REPORTS_FOLDER=${FTP_REPORTS_FOLDER}
  export FTP_TARGET_FOLDER=${FTP_TARGET_FOLDER}
  export FTP_USER=${FTP_USER}
  export LETTER_TRACKING_DB_PASSWORD=${LETTER_TRACKING_DB_PASSWORD}
  export S2S_URL=${S2S_URL}
  export SCHEDULING_ENABLED=${SCHEDULING_ENABLED}
  export ENCRYPTION_ENABLED=${ENCRYPTION_ENABLED}
  export ENCRYPTION_PUBLIC_KEY=${ENCRYPTION_PUBLIC_KEY}

  echo "Bringing up docker containers.."

  if [ ${FLYWAY_ENABLED} = true ]
  then
    docker-compose -f docker-compose-flyway.yml up
  else
    docker-compose up
  fi
}

while true ; do
  case "$1" in
    -h|--help) print_help ; shift ; break ;;
    -c|--clean) GRADLE_CLEAN=true ; GRADLE_INSTALL=true ; shift ;;
    -i|--install) GRADLE_INSTALL=true ; shift ;;
    -f|--with-flyway) FLYWAY_ENABLED=true ; shift ;;
    -p|--param)
      case "$2" in
        APPINSIGHTS_INSTRUMENTATIONKEY=*) APPINSIGHTS_INSTRUMENTATIONKEY="${2#*=}" ; shift 2 ;;
        FTP_FINGERPRINT=*) FTP_FINGERPRINT="${2#*=}" ; shift 2 ;;
        FTP_HOSTNAME=*) FTP_HOSTNAME="${2#*=}" ; shift 2 ;;
        FTP_PORT=*) FTP_PORT="${2#*=}" ; shift 2 ;;
        FTP_PRIVATE_KEY=*) FTP_PRIVATE_KEY="${2#*=}" ; shift 2 ;;
        FTP_PUBLIC_KEY=*) FTP_PUBLIC_KEY="${2#*=}" ; shift 2 ;;
        FTP_REPORTS_FOLDER=*) FTP_REPORTS_FOLDER="${2#*=}" ; shift 2 ;;
        FTP_TARGET_FOLDER=*) FTP_TARGET_FOLDER="${2#*=}" ; shift 2 ;;
        FTP_USER=*) FTP_USER="${2#*=}" ; shift 2 ;;
        LETTER_TRACKING_DB_PASSWORD=*) LETTER_TRACKING_DB_PASSWORD="${2#*=}" ; shift 2 ;;
        S2S_URL=*) S2S_URL="${2#*=}" ; shift 2 ;;
        SCHEDULING_ENABLED=*) SCHEDULING_ENABLED="${2#*=}" ; shift 2 ;;
        ENCRYPTION_ENABLED=*) ENCRYPTION_ENABLED="${2#*=}" ; shift 2 ;;
        ENCRYPTION_PUBLIC_KEY=*) ENCRYPTION_PUBLIC_KEY="${2#*=}" ; shift 2 ;;
        *) shift 2 ;;
      esac ;;
    *) execute_script ; break ;;
  esac
done
