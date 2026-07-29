.PHONY: up down logs api web install test-api

up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

api:
	cd apps/api && ./gradlew bootRun --args='--spring.profiles.active=local'

web:
	cd apps/web && npm run dev

install:
	cd apps/web && npm install

test-api:
	cd apps/api && ./gradlew test

kind-up:
	bash scripts/kind-up.sh

k3d-up:
	bash scripts/k3d-up.sh
