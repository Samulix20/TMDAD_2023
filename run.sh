#!/bin/bash

# Init data directories
mkdir -p database/data
mkdir -p minio_data/tmdad
mkdir -p rabbit_data

# Gradle build
cd websockets
./gradlew clean build -x test || exit 1
cd ..

# Launch clean docker
docker compose rm -f
docker compose up --force-recreate --build
