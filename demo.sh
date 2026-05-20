#!/usr/bin/env bash
set -euo pipefail

echo "[PJB] Iniciando ambiente demo..."

if [ ! -f ".env" ]; then
    echo "[PJB] Criando .env a partir do .env.example..."
    cp .env.example .env
    echo "PJB_MASTER_KEY_BASE64=cGpiLW1hc3Rlci1rZXktZGVtby0yMDI2LXNlZ3VybzEyMw==" >> .env
    echo "PJB_ES_JAVA_OPTS=-Xms256m -Xmx256m" >> .env
fi

echo "[PJB] Compilando..."
./mvnw install -pl pjb-core -DskipTests -q
./mvnw package -pl pjb-api -DskipTests -q

echo "[PJB] Subindo infra + backend..."
docker compose --profile app up --build -d

echo "[PJB] Aguardando backend ficar pronto (pode levar ate 2 minutos)..."
until curl -fsS http://localhost:8080/livez >/dev/null 2>&1; do
    sleep 5
done

echo ""
echo "[PJB] Sistema pronto!"
echo "  Healthcheck : http://localhost:8080/livez"
echo "  Demo status : http://localhost:8080/demo/status"
echo "  API docs    : http://localhost:8080/swagger-ui.html"
echo ""
echo "Para parar: docker compose down"
