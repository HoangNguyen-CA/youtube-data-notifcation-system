
docker-up:
	docker compose up -d
build-all: mvn-build docker-build
mvn-build: 
	mvn clean package
docker-build:
	docker compose build --no-cache
docker-down:
	docker compose down
kafka-exec:
	docker exec --workdir /opt/kafka/bin/ -it broker sh

.PHONY: docker-up docker-build docker-down kafka-exec mvn-build build-all


