# Documentação do PJB

Aqui eu deixo apenas o que ajuda a entender, operar e evoluir o projeto sem poluir a raiz.

## Estrutura

- `adr/` — decisões arquiteturais
- `features/` — documentação funcional estável por domínio
- `quality/` — taxonomia, layout e guias de higiene
- `infrastructure/` — runtime, operação e footprint
- `status/` — estado consolidado e material de fechamento
- `reports/` — relatórios gerados e evidências auxiliares
- `rounds/` — histórico arquivado das rodadas
- `openapi/` — contratos expostos
- `postman/` — coleções de integração
- `database/` — material de banco e RLS
- `security/` — material de segurança institucional
- `frontend/` — apoio à integração com frontend

## Regra prática

- raiz = mapa principal
- `docs/` = detalhe técnico
- histórico = arquivado
- relatório gerado = não disputa protagonismo com documentação funcional
- mapa de pacotes vivos = README da raiz, para não espalhar documentação de estrutura em arquivos soltos
- organização processual, de controladores processuais por capacidade funcional ampla, secretarial, recursal, de comunicação processual, da espinha de serviços e controladores de comunicação institucional, do cluster de substituição nacional em DTOs e facades, de ajuizamento por eixo jurisdicional, de tribunal e do núcleo de prazo/calendário = README da raiz e testes arquiteturais `PjbProcessualPackageOrganizationArchTest`, `PjbProcessualCommunicationPackageOrganizationArchTest`, `PjbSecretariatAndLifecyclePackageOrganizationArchTest`, `PjbAjuizamentoJurisdictionPackageOrganizationArchTest`, `PjbTribunalRulesPackageOrganizationArchTest` e `PjbPrazosCalendarPackageOrganizationArchTest`, para a estrutura não voltar a achatar
