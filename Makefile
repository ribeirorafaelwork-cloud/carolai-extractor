PROJECT_NAME=carolaiextractor
COMPOSE=docker compose -f docker-compose.yml

.PHONY: up down build restart logs ps clean db-shell app-shell up-logs


up:
	$(COMPOSE) down --remove-orphans
	$(COMPOSE) build
	$(COMPOSE) up -d
	$(COMPOSE) logs -f $(PROJECT_NAME)-app

up-clean:
	./mvnw clean
	$(COMPOSE) down --volumes --remove-orphans
	$(COMPOSE) build --no-cache
	$(COMPOSE) up -d
	$(COMPOSE) logs -f $(PROJECT_NAME)-app

down:
	$(COMPOSE) down

build:
	$(COMPOSE) build

restart: down up

logs:
	$(COMPOSE) logs -f $(PROJECT_NAME)-app

ps:
	$(COMPOSE) ps

clean:
	$(COMPOSE) down -v

db-shell:
	docker exec -it $(PROJECT_NAME)_pg psql -U carolai -d $(PROJECT_NAME)

app-shell:
	docker exec -it $(PROJECT_NAME)_app /bin/sh
