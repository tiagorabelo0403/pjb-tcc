# GeoIP — MaxMind GeoLite2-City

Diretório montado no container `backend` (`docker-compose.yml`, `/app/geoip:ro`) para o
geo-bloqueio de magistratura (`MagistraturaGeofencePolicyService` /
`MaxMindGeoLite2LookupAdapter`).

O arquivo `GeoLite2-City.mmdb` não é distribuído neste repositório — a licença da MaxMind exige
conta própria (gratuita) e license key individual. Sem o arquivo aqui, o adapter loga um aviso no
boot (`pjb.security.geofence.database-path não configurado`) e o lookup de geolocalização fica
indisponível de forma segura — nenhuma outra funcionalidade é afetada.

## Como habilitar

1. Criar conta gratuita em https://www.maxmind.com/en/geolite2/signup
2. Gerar uma license key em https://www.maxmind.com/en/accounts/current/license-key
3. Baixar `GeoLite2-City.mmdb` (formato binário, licença GeoLite2 End User License Agreement)
4. Colocar o arquivo neste diretório (`infra/geoip/GeoLite2-City.mmdb`)
5. Subir o `backend` normalmente — `PJB_SECURITY_GEOFENCE_DATABASE_PATH` já aponta por padrão
   para `/app/geoip/GeoLite2-City.mmdb` dentro do container (ver `docker-compose.yml`)

Para usar outro nome/caminho de arquivo, defina `PJB_SECURITY_GEOFENCE_DATABASE_PATH` no `.env`
apontando para o caminho dentro do container.

`GeoLite2-City.mmdb` está no `.gitignore` — nunca commitar a base binária.
