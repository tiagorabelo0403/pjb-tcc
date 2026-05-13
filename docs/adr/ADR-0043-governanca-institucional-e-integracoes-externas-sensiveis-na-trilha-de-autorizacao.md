# ADR-0043 — governança institucional e integrações externas sensíveis na trilha de autorização

## Status
Aceito

## Contexto

Após a criação da trilha explicável e persistível de autorização, o PJB já conseguia produzir decisão auditável para leitura de processo, votos, documentos e escrita processual. Ainda assim, duas superfícies sensíveis permaneciam com proteção assimétrica:

- a autorização de caixas institucionais dependia principalmente de validação operacional direta, sem trilha formal persistível por decisão;
- o acesso a integrações externas sensíveis ainda era exposto sobretudo por métodos booleanos, o que era suficiente para pré-checks de interface, mas insuficiente para governança operacional, auditoria forense e explicabilidade em operações reais.

Esses dois eixos são particularmente sensíveis em ambiente judicial porque concentram capacidade institucional delegada, acesso indireto a bases externas e requisitos contextuais como delegação formal e contexto formal.

## Decisão

A trilha de autorização foi ampliada para cobrir também governança institucional e integrações externas sensíveis.

### 1. Nova avaliação de governança

Foi introduzido `PjbAuthorizationGovernanceAssessment`, capaz de representar:

- se a governança é exigida;
- se foi satisfeita;
- qual o canal de governança envolvido;
- qual o código normativo interno da decisão;
- qual o escopo da exigência;
- qual mensagem operacional deve ser apresentada ao usuário em caso de bloqueio.

### 2. Expansão da trilha formal

`PjbAuthorizationDecisionTrail` passou a carregar também o estado de governança. Esse material foi incorporado ao hash e à descrição auditável da decisão.

### 3. Capacidade institucional explicável

Foi criado `PjbAuthorizationInstitutionalCapabilityFacade` para produzir avaliação formal e persistível quando o usuário tenta operar caixas institucionais. Com isso, a autorização institucional deixa de depender apenas de exceção direta e passa a gerar trilha de decisão antes do bloqueio ou da concessão.

### 4. Integrações externas sensíveis com trilha formal

Foi criado `PjbAuthorizationSensitiveIntegrationFacade` para consolidar a governança e a trilha de operações sensíveis envolvendo:

- BNMP
- CNIB
- RENAJUD
- INFOJUD
- SISBAJUD
- SERASAJUD
- localização por CPF
- localização sem processo
- endereço estrito
- inteligência patrimonial
- mandados por pessoa

Essa fachada transforma decisões antes expostas apenas como booleanos em avaliações formais com trilha, risco e estado de governança.

## Consequências

### Positivas

- a governança institucional passa a ser rastreável e persistível;
- integrações externas sensíveis deixam de depender apenas de checagens dispersas ou silenciosas;
- o serviço principal de autorização permanece curto, com a complexidade segregada em colaboradores dedicados;
- o PJB avança em direção a uma linha de zero trust contextual também para operações institucionais e integrações externas.

### Negativas

- a malha de autorização passa a ter mais tipos internos e mais superfície de teste estrutural;
- operações de pré-check continuam podendo usar métodos booleanos, o que exige disciplina para que fluxos críticos prefiram os métodos `require*` quando a operação for efetivamente executada.
