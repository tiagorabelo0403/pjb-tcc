# Mapa de cloud do PJB

## Evidências reais no projeto

### Containerização
- `Dockerfile`
- `docker-compose.yml`
- `docker-compose.read-replica.yml`

### Kubernetes
- `infra/k8s/base/`
- `infra/k8s/overlays/prod/`
- `infra/k8s/overlays/prod-vpa/`
- `infra/k8s/overlays/prod-sovereign-autoscale/`
- `infra/k8s/overlays/prod-sovereign-adaptive-mesh/`
- `infra/k8s/overlays/prod-sovereign-governed-priority-mesh/`
- `infra/k8s/overlays/prod-sovereign-operational-resilience-mesh/`
- `infra/k8s/overlays/prod-read-replica/`

### Escalabilidade
- HPA base para API e worker
- overlay opcional com VPA em modo seguro
- overlay soberano com KEDA + VPA Initial
- overlay adaptativo com KEDA composto, tolerância por workload e distribuição por zona
- ingress, service, deployment, PDB e network policy

### Perfis de runtime
- `application-docker.yml`
- `application-k8s.yml`
- `application-prod.yml`

### Dependências e plataforma
- Redis
- Kafka
- PostgreSQL
- Spring Cloud Context

## Leitura correta

O PJB é cloud-ready e já possui base real para execução em ambiente de nuvem com contêineres e Kubernetes.

Isso não significa que o repositório já esteja totalmente amarrado a um provedor específico com IaC completa de AWS, Azure ou GCP.

## O que ainda é parcial

- não há, neste ponto, uma malha completa de Terraform ou Pulumi no repositório
- Cloud Run aparece como workflow de agente, não como prova principal de runtime do sistema inteiro
- a implantação final em provedor específico ainda depende do ambiente alvo


## Autoscaling soberano

A trilha `infra/k8s/overlays/prod-sovereign-autoscale/` substitui os HPAs base por `ScaledObject` do KEDA, mantendo o VPA em modo `Initial` para evitar disputa contínua entre escala horizontal e vertical na mesma carga.

A ideia operacional fica assim:

- API escala horizontalmente por taxa real de requisições HTTP e piso de horário institucional
- worker escala horizontalmente por backlog material do domínio, usando métricas de outbox e workitems pendentes
- VPA continua dimensionando requests na criação do pod, sem ficar reescrevendo recursos de pods já quentes

Pré-requisitos da trilha:

- Metrics Server para VPA
- Prometheus para consultas dos scalers
- operador KEDA instalado no cluster

## Malha governada por prioridade judicial

A trilha `infra/k8s/overlays/prod-sovereign-governed-priority-mesh/` fecha segurança, estabilidade e respaldo institucional no mesmo plano:

- namespace `pjb` passa a operar com Pod Security Admission em `restricted`
- `ValidatingAdmissionPolicy` e binding no namespace negam deployments fora do padrão mínimo da plataforma
- `PriorityClass` processual separa custódia, urgência/sigilo, sessão, recomposição e bulk
- `ResourceQuota` com `scopeSelector` por `PriorityClass` impede que classes baixas consumam o orçamento inteiro do namespace
- a fórmula do KEDA ganha peso maior para pressão de custódia, urgência, sigilo e sessão do que para recomposição ou bulk
- `PodDisruptionBudget` sobe o piso de disponibilidade e libera drenagem segura de pods doentes

## Malha adaptativa composta

A trilha `infra/k8s/overlays/prod-sovereign-adaptive-mesh/` sobe um degrau acima da soberana básica:

- o `pjb-api` escala por pressão composta de throughput, latência p95, erro 5xx e saturação de conexão
- o `pjb-worker` escala por pressão composta de outbox, workitems e lag de Kafka
- a API passa a exigir `minAvailable: 2` no `PodDisruptionBudget`
- API e worker passam a preferir distribuição por `topology.kubernetes.io/zone` e `kubernetes.io/hostname`
- a malha usa sensibilidade diferente por workload via `tolerance` do HPA gerado pelo KEDA

Esse desenho é mais inteligente porque evita escalar apenas por volume bruto. Ele reage também quando o sistema ainda não estourou em RPS, mas já mostra deterioração de latência, erro ou pressão no pool.


## Resiliência operacional endurecida

A trilha `infra/k8s/overlays/prod-sovereign-operational-resilience-mesh/` adiciona mais uma camada nativa do Kubernetes para evitar sobrecarga silenciosa:

- `LimitRange` extra com piso, teto e `maxLimitRequestRatio` para conter explosões de recurso por container
- `ResourceQuota` específico para armazenamento efêmero e PVCs
- `API Priority and Fairness` com níveis separados para service accounts críticas, workload normal e bulk no namespace `pjb`
- `ephemeral-storage` explícito em `pjb-api` e `pjb-worker`
- blueprints opcionais de reserva de nó em `infra/k8s/blueprints/capacity/`

Esse desenho endurece o cluster sem empurrar CRD opcional para o overlay principal e sem depender de provedor específico para o caminho base.


## Perfil de crise soberana

O overlay `prod-sovereign-crisis-containment` cria uma ativação declarativa de contenção para cenários de pressão severa. Ele mantém a malha crítica com piso maior de réplicas, desacelera recomposição de read models e aciona `pjb.api.crisis-control` para bloquear rotas bulk sem derrubar a superfície crítica.

## Data plane zero trust com borda de banco

A trilha `infra/k8s/overlays/prod-sovereign-zero-trust-data-plane/` sobe mais um nível de blindagem na horizontal:

- reaproveita a malha `prod-sovereign-operational-resilience-mesh` como base soberana;
- move a aplicação para entrada em `db-edge-rw` e `db-edge-ro`, em vez de falar diretamente com o namespace de banco;
- força `PJB_DB_READ_ROUTING_ENABLED=true` com rota explícita para leitura via borda ro;
- reduz o footprint de pool no datasource da aplicação para operar melhor com PgBouncer na frente do PostgreSQL;
- fecha `egress` de API e worker para o namespace `database-edge`, mantendo DNS, Redis, Elasticsearch e Kafka apenas onde necessário.

Essa trilha não substitui a malha de banco nem o edge TCP do compose HA; ela aproxima o desenho Kubernetes do mesmo modelo soberano de borda rw/ro já endurecido no footprint Docker.
