# Docker Compose Guard

OK: yes

## base

Files: `docker-compose.yml`

Sem violações estruturais detectadas.

### Notices

- `backend` — serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.
- `elasticsearch` — heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.

## base+replica

Files: `docker-compose.yml, docker-compose.read-replica.yml`

Sem violações estruturais detectadas.

### Notices

- `backend` — serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.
- `elasticsearch` — heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.

## base+ha

Files: `docker-compose.yml, docker-compose.ha.yml`

Sem violações estruturais detectadas.

### Notices

- `backend` — serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.
- `elasticsearch` — heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.
- `pgbouncer-ro` — fallback padrão da rota ro aponta para postgres, permitindo HA local sem read replica obrigatória.

## base+ha+replica

Files: `docker-compose.yml, docker-compose.ha.yml, docker-compose.read-replica.yml`

Sem violações estruturais detectadas.

### Notices

- `backend` — serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.
- `elasticsearch` — heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.
- `pgbouncer-ro` — fallback padrão da rota ro aponta para postgres, permitindo HA local sem read replica obrigatória.

## base+n8n

Files: `docker-compose.yml, docker-compose.n8n.yml`

Sem violações estruturais detectadas.

### Notices

- `backend` — serviço ficou sob profile app para não bloquear a subida da infraestrutura enquanto o pjb-api segue em recuperação de compile.
- `elasticsearch` — heap default local fixado em 512m com xpack.ml.enabled=false para reduzir falha de subida por pressão de memória.
