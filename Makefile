# ════════════════════════════════════════════════════════════════════════════
#  BookPlus — Makefile
#  Prerequisites: Docker Desktop, docker compose v2, openssl, bash
# ════════════════════════════════════════════════════════════════════════════

.DEFAULT_GOAL := help
COMPOSE_FILE  := docker-compose.full.yml

.PHONY: help keys up down restart logs ps build test smoke clean

help: ## Show this help message
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "  Quick start:"
	@echo "    make keys   → generate RSA key pair"
	@echo "    make up     → start the full stack"
	@echo "    make logs   → tail all service logs"

keys: ## Generate RSA key pair for JWT signing (writes to .env)
	@bash scripts/generate-keys.sh

up: ## Start the full stack (builds images if needed)
	@test -f .env || (echo "❌  .env not found. Run 'make keys' first." && exit 1)
	docker compose -f $(COMPOSE_FILE) --env-file .env up --build -d
	@echo ""
	@echo "✅  BookPlus stack is starting..."
	@echo "   API Gateway  → http://localhost:8080"
	@echo "   Swagger UI   → http://localhost:8082/swagger-ui.html  (catalog)"
	@echo "   MailHog UI   → http://localhost:8025"
	@echo "   Kafka UI     → http://localhost:8090  (if enabled)"
	@echo ""
	@echo "   Run 'make logs' to tail logs or 'make ps' to check health."

down: ## Stop and remove all containers
	docker compose -f $(COMPOSE_FILE) down

restart: ## Restart all services without rebuilding
	docker compose -f $(COMPOSE_FILE) restart

build: ## Build all Docker images without starting
	docker compose -f $(COMPOSE_FILE) build

logs: ## Tail logs for all services (Ctrl+C to exit)
	docker compose -f $(COMPOSE_FILE) logs -f --tail=50

logs-%: ## Tail logs for a specific service: make logs-order-service
	docker compose -f $(COMPOSE_FILE) logs -f --tail=100 $*

ps: ## Show container status and health
	docker compose -f $(COMPOSE_FILE) ps

test: ## Run unit tests for all services
	@echo "🧪  Running tests for all services..."
	@for dir in book-plus-*/; do \
	  if [ -f "$$dir/pom.xml" ]; then \
	    echo "  → Testing $$dir..."; \
	    (cd "$$dir" && mvn -q test 2>&1 | tail -5) || echo "    ❌ Tests failed in $$dir"; \
	  fi \
	done
	@echo "✅  Done."

test-%: ## Run tests for a specific service: make test-order-service
	(cd book-plus-$* && mvn test)

smoke: ## Run the Fase 0 smoke test against the running gateway
	@bash scripts/smoke-test.sh

clean: ## Remove all containers, volumes and build artifacts
	docker compose -f $(COMPOSE_FILE) down -v --remove-orphans
	@for dir in book-plus-*/; do \
	  if [ -f "$$dir/pom.xml" ]; then \
	    (cd "$$dir" && mvn -q clean); \
	  fi \
	done
	@echo "🧹  Clean complete."
