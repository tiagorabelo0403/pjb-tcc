# Matriz Nacional de Substituição de Sistemas Judiciais

Este documento orienta a evolução do AKASHIC-PJB OMEGA X como plataforma judicial nacional. A matriz evita duplicação de módulos, separa capacidades já existentes de lacunas reais e transforma a comparação com PJe, PJe 2.x, e-SAJ, eproc, Creta e Projudi em backlog governado.

## Premissas

- O PJB não deve reproduzir vícios de sistemas legados: acoplamento entre tela e domínio, rotas dispersas, dependência de fluxo local, assinatura digital frágil, pouca observabilidade e migração sem prova de integridade.
- Toda capacidade nova deve nascer em bounded context existente antes de criar novo pacote.
- A primeira escolha arquitetural deve ser reaproveitar `core.plataforma.substituicao`, `integration.judicial`, `integration.mni`, `core.procedural`, `core.processo`, `core.kernel.recursal`, `core.comunicacao.institucional`, `service.secretariat`, `core.security`, `core.lgpd` e `core.observability`.
- Nenhuma funcionalidade deve ser adicionada apenas para aumentar superfície HTTP; o contrato público deve vir depois do serviço de aplicação e da prova de governança.

## Inventário atual do PJB

| Eixo | Situação atual | Pacotes e artefatos existentes | Direção arquitetural |
|---|---|---|---|
| Substituição nacional | Presente | `core.plataforma.substituicao` | Consolidar matriz nacional de capacidades, readiness por tribunal e execução de corte controlado. |
| Migração de acervo | Parcial | `PjbSubstituicaoMigracaoIndustrialBatchService`, `core.processo.migracao` | Completar reconciliação de documentos, partes, movimentos, classes, assuntos, segredo e protocolo original. |
| Interoperabilidade | Presente | `integration.judicial`, `integration.mni`, conectores PJe, e-SAJ, eproc, Creta, Projudi, PDPJ, MNI | Evoluir para capability registry por tribunal e homologação de conectores. |
| Rotas institucionais | Em estabilização | `core.comunicacao.institucional`, controllers institucionais, `InstitutionalApiRoutes` | Manter base canônica única e proibir literais de rota. |
| Secretaria | Presente | `service.secretariat`, fila, agenda, painéis, topology | Fechar SLA operacional, retorno ao processo, gabinete, audiência, sessão e comunicação institucional. |
| Malha recursal | Presente | `core.kernel.recursal`, state machine, workspace, formalização | Consolidar subida multigrau, acórdão, sustentação, preparo, admissibilidade e preservação documental. |
| Assinatura e prova | Parcial | ICP-Brasil, PAdES-LTA, HSM mock, validação recursal | Separar evidência mockada, homologação e produção com TSA real e política documental por ato. |
| Portal público | Parcial | consulta pública, frontend delivery, painéis | Fechar consulta por chave, conferência de documento, push, jurisprudência, DJE e experiência externa. |
| Portal profissional | Parcial | peticionamento, advogado office, protocolo | Completar peticionamento inicial/intermediário por grau, colégio recursal, plantão e resposta por chave. |
| Operação nacional | Parcial | SLO, runtime pressure, telemetry, crisis mode | Criar readiness por tribunal, runbooks, fila morta, replay e disaster recovery operacional. |

## Matriz por sistema legado

### PJe e PJe 2.x

| Capacidade esperada | O PJB já possui | Falta fechar | Bounded context alvo |
|---|---|---|---|
| Tramitação processual padronizada | Núcleo processual, procedural, secretaria e gabinete | Readiness nacional por ramo, tribunal, grau, competência e unidade | `core.procedural`, `core.processo`, `service.secretariat` |
| Prática de atos processuais | Peticionamento, protocolo, documentos, comunicação | Experiência unificada por perfil, ato e grau | `core.peticionamento`, `core.comunicacao.judicial` |
| MNI e integração nacional | MNI, DataJud, conectores judiciais | Capability registry, homologação por tribunal e replay de falhas | `integration.mni`, `integration.judicial` |
| Plataforma multisserviço | Módulos internos e guards | Catálogo declarativo de capacidades por tribunal | `core.plataforma.substituicao` |
| Certificado e 2FA | ICP-Brasil, Gov.br, ABAC | Política de step-up por ato e perfil | `core.security`, `core.icp` |

### e-SAJ

| Capacidade esperada | O PJB já possui | Falta fechar | Bounded context alvo |
|---|---|---|---|
| Consulta processual pública | Consulta pública e leitura processual | Consulta amigável por grau, classe, parte, OAB, documento e chave | `core.processo.busca`, `core.frontend.delivery` |
| Peticionamento inicial e intermediário | Peticionamento e protocolo | Fluxos completos por 1º grau, 2º grau, colégio recursal e plantão | `core.peticionamento`, `core.kernel.recursal` |
| Conferência de documento digital | Hash, evidência, ICP e documentos | Verificação pública por código, QR, hash e número processual | `core.icp`, `core.document`, `core.security` |
| Push processual | Notificação e comunicação | Assinatura externa por processo, classe, parte e evento | `core.comunicacao.judicial`, `core.observability` |
| Jurisprudência e DJE | DJe e leitura | Consulta consolidada, indexação e publicação governada | `core.dje`, `core.processo.busca` |

### eproc

| Capacidade esperada | O PJB já possui | Falta fechar | Bounded context alvo |
|---|---|---|---|
| Consulta por chave do processo | Política de chave temporária, escopo, expiração, auditoria e revogação | Persistência, rotação e superfície pública controlada | `core.security.accesskey`, `core.processo.sigilo` |
| Consulta por chave de documento | Documento, hash, ICP | Chave documental com acesso ao inteiro teor autorizado | `core.document`, `core.icp` |
| Peticionamento rápido | Peticionamento e protocolo | Jornada mínima de alta velocidade para advogado e parte | `core.peticionamento`, `core.frontend.delivery` |
| Resposta por chave | Comunicação judicial e fila | Resposta externa vinculada à intimação/documento | `core.comunicacao.judicial`, `service.secretariat` |
| Implantação gradual | Substituição nacional e execução | Corte por unidade, ramo, competência e classe com rollback | `core.plataforma.substituicao` |

### Creta

| Capacidade esperada | O PJB já possui | Falta fechar | Bounded context alvo |
|---|---|---|---|
| Juizado Especial Federal | Procedural, previdenciário, recursal | Produto completo de JEF: atermação, audiência, perícia, cálculo, sentença e execução | `core.procedural`, `core.processo`, `core.kernel.recursal` |
| Turma recursal e uniformização | Malha recursal | Incidente de uniformização e TNU com políticas explícitas | `core.kernel.recursal` |
| Parte sem advogado | Gov.br, painel cidadão | Atermação assistida, linguagem simples e provas guiadas | `service.cidadao`, `core.frontend.delivery` |
| INSS e benefícios | Trilhos previdenciários | Integração documental e cálculo de benefício em rito simplificado | `core.processo.painel`, `core.financeiro` |

### Projudi

| Capacidade esperada | O PJB já possui | Falta fechar | Bounded context alvo |
|---|---|---|---|
| Tramitação simples e leve | Núcleo processual e secretaria | Perfil de implantação compacta para tribunal pequeno/médio | `infra`, `core.plataforma.substituicao` |
| Operação de baixo custo | Docker, K8s, H2/test, PostgreSQL | Runbook compacto, backup, restore e atualização segura | `docs/infrastructure`, `scripts` |
| Administração clara | Governança e rotas | Console administrativo mínimo por tribunal | `controller.admin`, `core.plataforma.substituicao` |
| Processo eletrônico completo | Domínio amplo | Corte de módulos opcionais sem quebrar núcleo | `pjb-core`, `pjb-api` |

## Backlog priorizado sem duplicação

| Prioridade | Entrega | Tipo | Deve reaproveitar | Critério de aceite |
|---|---|---|---|---|
| P0 | Guard de limpeza estrutural | Qualidade | `scripts`, `repository_layout_guard.py` | ZIP sem lixo, sem arquivos temporários e sem markdown solto na raiz. |
| P0 | Guard de rotas institucionais | Qualidade | `InstitutionalApiRoutes`, controllers institucionais | Nenhum controller institucional fora da base canônica. |
| P0 | Matriz de capacidades em código | Domínio | `core.plataforma.substituicao` | Catálogo sem sistemas legados duplicados e com lacunas rastreáveis. |
| P0 | Readiness por tribunal | Produto | `core.plataforma.substituicao.readiness` | Snapshot com bloqueios, homologação, capabilities e status de produção. |
| P0 | Indisponibilidade e impacto em prazo | Operação | `core.observability.unavailability`, `core.prazos` | Avaliação de indisponibilidade externa crítica e próximo dia útil. |
| P0 | Compatibilidade MNI | Interoperabilidade | `integration.mni.compatibility` | Matriz por tribunal e operação sem criar conector paralelo. |
| P1 | Readiness por tribunal | Produto | `PjbSubstituicaoTribunalHomologacaoProbeService`, `core.plataforma.substituicao.readiness` | Cada tribunal com status, bloqueios, conectores e plano de corte. |
| P1 | Chave de processo e documento | Produto | `core.security.accesskey`, `core.processo.sigilo`, `core.icp` | Acesso externo auditado, revogável e compatível com sigilo. |
| P1 | Portal público completo | Produto | `core.frontend.delivery`, `core.processo.busca` | Consulta pública, conferência, push, DJE e jurisprudência. |
| P2 | JEF/Creta completo | Produto | `core.procedural`, `core.kernel.recursal`, previdenciário | Atermação, perícia, cálculo, recurso e execução simplificados. |
| P2 | Runbook de operação nacional | Operação | `docs/infrastructure`, `core.observability` | SLO, backup, restore, replay, crise e disaster recovery documentados. |

## Regras de evolução

- Não criar outro pacote de substituição nacional fora de `core.plataforma.substituicao`.
- Não criar outro conector legado fora de `integration.judicial` ou `integration.mni`.
- Não criar rota institucional fora de `InstitutionalApiRoutes`.
- Não criar novo fluxo recursal fora de `core.kernel.recursal`.
- Não criar novo fluxo de secretaria fora de `service.secretariat`.
- Não criar novo portal externo sem passar por `core.frontend.delivery`.
- Não introduzir comentário explicativo em código-fonte.
- Não incluir arquivo temporário, log, build output ou cache no repositório.
