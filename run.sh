#!/bin/bash

cd websockets
./gradlew clean build -x test
cd ..
docker compose rm -f
docker compose up --force-recreate --build -d
