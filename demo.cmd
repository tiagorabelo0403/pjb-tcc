@echo off
setlocal EnableExtensions

echo [PJB] Iniciando ambiente demo...

if not exist ".env" (
    echo [PJB] Criando .env a partir do .env.example...
    copy /Y ".env.example" ".env" >nul
    echo PJB_MASTER_KEY_BASE64=cGpiLW1hc3Rlci1rZXktZGVtby0yMDI2LXNlZ3VybzEyMw==>> ".env"
    echo PJB_ES_JAVA_OPTS=-Xms256m -Xmx256m>> ".env"
)

echo [PJB] Compilando (pule com Ctrl+C se ja compilou)...
call mvnw.cmd install -pl pjb-core -DskipTests -q
if errorlevel 1 (
    echo [ERRO] Falha ao compilar pjb-core
    exit /b 1
)
call mvnw.cmd package -pl pjb-api -DskipTests -q
if errorlevel 1 (
    echo [ERRO] Falha ao compilar pjb-api
    exit /b 1
)

echo [PJB] Subindo infra + backend...
docker compose --profile app up --build -d

echo.
echo [PJB] Aguardando backend ficar pronto (pode levar ate 2 minutos)...
:wait
timeout /t 5 /nobreak >nul
curl -fsS http://localhost:8080/livez >nul 2>&1
if errorlevel 1 goto wait

echo.
echo [PJB] Sistema pronto!
echo   Healthcheck : http://localhost:8080/livez
echo   Demo status : http://localhost:8080/demo/status
echo   API docs    : http://localhost:8080/swagger-ui.html
echo.
echo Para parar: docker compose down
