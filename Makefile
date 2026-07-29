.PHONY: up down logs api web install test-api test-e2e openapi-check

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

openapi-check:
	bash scripts/check-openapi-sync.sh

# Requires API on :8080 (make api)
test-e2e:
	cd apps/web && npm run test:e2e

kind-up:
	bash scripts/kind-up.sh

k3d-up:
	bash scripts/k3d-up.sh

obs-up:
	docker compose --profile observability up -d prometheus grafana

obs-down:
	docker compose --profile observability stop prometheus grafana
