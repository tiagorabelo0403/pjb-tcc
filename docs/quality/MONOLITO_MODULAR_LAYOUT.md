# Monólito modular — layout canônico

## Objetivo
Normalizar a topologia do projeto para evitar drift entre agregador Maven, módulo de kernel extraído e módulo da aplicação Spring Boot.

## Layout canônico
- `pom.xml` na raiz: agregador e pai comum
- `pjb-core/src/main/java`: pacote extraído do kernel e futuras extrações
- `pjb-api/src/main/java`: aplicação Spring Boot, controllers, services, DTOs e integração operacional
- `pjb-api/src/main/resources`: configurações, migrations, seeds e recursos do módulo da aplicação
- `pjb-api/src/test/java`: testes do módulo da aplicação
- `pjb-api/src/test/resources`: recursos de teste do módulo da aplicação

## Regras de governança
1. A raiz não possui mais `src/*` compilável.
2. `pjb-api` não pode apontar para `../src/main/java` nem compilar código fora do próprio módulo.
3. Docker deve empacotar `pjb-api` e consumir o jar em `pjb-api/target`.
4. Scanners e scripts devem resolver o root canônico do módulo da aplicação antes de ler `src/*`.
5. Extrações futuras para `pjb-core` devem manter a raiz como agregador puro.

## Consequência prática
Essa normalização elimina ambiguidade de IDE, simplifica Docker, reduz acoplamento acidental entre módulos e deixa a transição do monólito modular auditável.
