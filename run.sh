#!/bin/bash
DEPLOYMENT="cloud"

# Init data directories
mkdir -p database/data
mkdir -p minio_data/tmdad
mkdir -p rabbit_data

# Gradle build
cd websockets
./gradlew clean build -x test || exit 1
cd ..

DOCKER_ARGS="--env-file ${DEPLOYMENT}.env --profile $DEPLOYMENT"

echo "${DEPLOYMENT}: $DOCKER_ARGS"

# Launch clean docker
docker compose $DOCKER_ARGS rm -f
docker compose $DOCKER_ARGS up