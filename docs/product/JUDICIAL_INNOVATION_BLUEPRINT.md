# Judicial Innovation Blueprint

Este blueprint organiza as capacidades de inovação do PJB sem criar módulos paralelos. Cada iniciativa evolui um bounded context existente e preserva a disciplina de rotas, segurança, sigilo, auditoria e migração.

## Princípio de arquitetura

O PJB não deve apenas registrar atos processuais. A plataforma deve antecipar riscos, explicar próximos passos, preservar prova técnica e apoiar operadores humanos com decisões auditáveis.

## Capacidades adicionadas

| Capacidade | Pacote | Finalidade |
| --- | --- | --- |
| Simulação de impacto de mudança | `pjb-api/src/main/java/com/tcc/pjb/backend/core/governance/changeimpact` | Avaliar blast radius antes de alterações em rotas, segurança, banco, integração e domínio sensível. |
| Radar de saúde processual | `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/health` | Consolidar risco de atraso, nulidade, competência, sigilo, intimação, documento e fila. |
| Caixa-preta de protocolo | `pjb-api/src/main/java/com/tcc/pjb/backend/core/peticionamento/blackbox` | Preservar cadeia técnica de tentativa de protocolo, hashes, assinatura, conector e falha externa. |
| Linha do tempo em linguagem simples | `pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess` | Traduzir movimento técnico para explicação compreensível ao cidadão. |
| Centro público de confiança documental | `pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess` | Avaliar integridade, assinatura, carimbo temporal, revogação e versão pública. |
| Certidão técnica de indisponibilidade | `pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability` | Materializar prova de indisponibilidade e impacto em prazo. |
| Distribuição explicável | `pjb-api/src/main/java/com/tcc/pjb/backend/core/distribuicao/explainable` | Registrar critérios de alocação, revisão humana e confiança operacional. |
| Autopilot de secretaria | `pjb-api/src/main/java/com/tcc/pjb/backend/service/secretariat/autopilot` | Priorizar tarefas sem substituir decisão humana. |
| Inteligência de migração legada | `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/migracao/intelligence` | Classificar divergências antes da carga definitiva de acervo. |
| Gêmeo digital do tribunal | `pjb-api/src/main/java/com/tcc/pjb/backend/core/kernel/twin` | Simular backlog, capacidade semanal e necessidade de intervenção. |
| Balcão virtual inteligente | `pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/virtualcounter` | Roteamento inicial para cidadão, parte, secretaria, documento, audiência e assistência jurídica. |

## Regras de implantação

- Nenhuma inovação decide processo automaticamente.
- Toda recomendação sensível deve admitir revisão humana.
- Nenhuma superfície pública pode expor dado sigiloso sem versão pública saneada.
- Toda migração deve preservar referência de origem, divergência e trilha de reconciliação.
- Toda alteração em rota, segurança ou banco deve passar por simulação de impacto e guards.
