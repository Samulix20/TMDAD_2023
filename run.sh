cd websockets
./gradlew clean build
cd ..
docker compose up --force-recreate --build
