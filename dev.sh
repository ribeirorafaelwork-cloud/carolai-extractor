#!/usr/bin/env bash
set -euo pipefail

# ─── Cores ────────────────────────────────────────────────────────────────────
RESET='\033[0m'
BOLD='\033[1m'
DIM='\033[2m'
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'

# ─── Helpers ──────────────────────────────────────────────────────────────────
log()  { echo -e "${DIM}[dev]${RESET} $*"; }
ok()   { echo -e "${GREEN}✔${RESET} $*"; }
warn() { echo -e "${YELLOW}⚠${RESET}  $*"; }
err()  { echo -e "${RED}✖${RESET}  $*" >&2; }
sep()  { echo -e "${DIM}────────────────────────────────────────────────────${RESET}"; }

SHOW_SQL=false
for arg in "$@"; do [[ "$arg" == "--sql" ]] && SHOW_SQL=true; done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ─── Banner ───────────────────────────────────────────────────────────────────
[[ -t 1 && -n "${TERM:-}" ]] && clear
echo -e "${CYAN}${BOLD}"
echo " ███████╗██╗  ██╗████████╗██████╗  █████╗  ██████╗████████╗ ██████╗ ██████╗"
echo " ██╔════╝╚██╗██╔╝╚══██╔══╝██╔══██╗██╔══██╗██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗"
echo " █████╗   ╚███╔╝    ██║   ██████╔╝███████║██║        ██║   ██║   ██║██████╔╝"
echo " ██╔══╝   ██╔██╗    ██║   ██╔══██╗██╔══██║██║        ██║   ██║   ██║██╔══██╗"
echo " ███████╗██╔╝ ██╗   ██║   ██║  ██║██║  ██║╚██████╗   ██║   ╚██████╔╝██║  ██║"
echo " ╚══════╝╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝"
echo -e "${RESET}${DIM}  carolai-extractor — ambiente de desenvolvimento${RESET}"
echo ""

# ─── Java 21 ──────────────────────────────────────────────────────────────────
sep
log "Verificando Java..."
export JAVA_HOME="$HOME/.sdkman/candidates/java/21.0.6-amzn"
export PATH="$JAVA_HOME/bin:$PATH"

if ! java -version 2>&1 | grep -q "21"; then
  err "Java 21 nao encontrado em $JAVA_HOME"
  err "Instale com: sdk install java 21.0.6-amzn"
  exit 1
fi
ok "Java $(java -version 2>&1 | awk -F '"' 'NR==1{print $2}')"

# ─── Postgres ─────────────────────────────────────────────────────────────────
sep
log "Verificando PostgreSQL..."
PG_CONTAINER="carolaiextractor_pg"

if docker inspect "$PG_CONTAINER" --format '{{.State.Status}}' 2>/dev/null | grep -q "running"; then
  ok "Container '$PG_CONTAINER' ja esta rodando"
else
  warn "Container '$PG_CONTAINER' nao esta rodando. Tentando iniciar..."
  if docker start "$PG_CONTAINER" 2>/dev/null; then
    ok "Container iniciado"
    sleep 3
  else
    err "Nao foi possivel iniciar '$PG_CONTAINER'"
    err "Suba o postgres manualmente: docker compose up -d postgres"
    exit 1
  fi
fi

if ! docker exec "$PG_CONTAINER" psql -U carolai -d carolaiextractor -c "SELECT 1" &>/dev/null; then
  log "Banco 'carolaiextractor' nao existe. Criando..."
  docker exec "$PG_CONTAINER" createdb -U carolai carolaiextractor 2>/dev/null && ok "Banco criado" || {
    err "Falha ao criar banco 'carolaiextractor'"
    exit 1
  }
else
  ok "Banco 'carolaiextractor' acessivel"
fi

# ─── Porta 8082 ───────────────────────────────────────────────────────────────
sep
log "Verificando porta 8082..."
if lsof -ti:8082 &>/dev/null; then
  warn "Porta 8082 em uso. Encerrando processo anterior..."
  lsof -ti:8082 | xargs kill -9 2>/dev/null || true
  sleep 1
  ok "Processo anterior encerrado"
else
  ok "Porta 8082 livre"
fi

# ─── Maven ────────────────────────────────────────────────────────────────────
sep
echo -e "${CYAN}${BOLD}▶  Iniciando carolai-extractor na porta 8082${RESET}"
echo -e "${DIM}   Health      →  http://localhost:8082/actuator/health${RESET}"
echo -e "${DIM}   Outbox      →  http://localhost:8082/internal/exports/stats${RESET}"
sep
echo ""

# ─── Filtro de logs ───────────────────────────────────────────────────────────
filter_logs() {
  while IFS= read -r line; do

    # Hibernate SQL/bind — oculto por padrao, visivel com --sql
    if [[ "$line" =~ (org\.hibernate\.(SQL|orm)|binding\ parameter|^[[:space:]]*(select|insert|update|delete|from|where|values|Hibernate:)) ]]; then
      if [[ "$SHOW_SQL" == "true" ]]; then
        echo -e "${DIM}${line}${RESET}"
      fi
      continue
    fi

    # Maven build — dimmed
    if [[ "$line" =~ ^\[INFO\]|\[WARNING\]|\[ERROR\] ]]; then
      echo -e "${DIM}${line}${RESET}"
      continue
    fi

    # Erros — sempre vermelho e em destaque
    if [[ "$line" =~ [[:space:]]ERROR[[:space:]] ]]; then
      echo -e "${RED}${BOLD}${line}${RESET}"
      continue
    fi

    # Warnings — amarelo
    if [[ "$line" =~ [[:space:]]WARN[[:space:]] ]]; then
      echo -e "${YELLOW}${line}${RESET}"
      continue
    fi

    # App pronto
    if [[ "$line" =~ "Started ExtractorApplication" ]]; then
      echo -e "${GREEN}${BOLD}${line}${RESET}"
      echo ""
      echo -e "${GREEN}${BOLD}  ✔  Extractor pronto! http://localhost:8082${RESET}"
      [[ "$SHOW_SQL" == "false" ]] && echo -e "${DIM}  dica: use \`carolai-extractor --sql\` para ver queries SQL${RESET}"
      sep
      continue
    fi

    # Logs da aplicacao (com.carolai / c.c.e) — branco/destaque
    if [[ "$line" =~ (com\.carolai|c\.c\.e) ]]; then
      echo -e "${WHITE}${line}${RESET}"
      continue
    fi

    # Resto — dimmed
    echo -e "${DIM}${line}${RESET}"
  done
}

cd "$SCRIPT_DIR"
mvn spring-boot:run -DskipTests \
  "-Dspring-boot.run.profiles=local" \
  2>&1 | filter_logs
