.PHONY: up down logs api web install

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
