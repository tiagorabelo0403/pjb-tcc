# Guia De Revisão Do Projeto

Este repositório contém o backend do PJB, uma plataforma judicial em Java 21, Spring Boot 3 e Maven multi-module.

## Estrutura Principal

- `pjb-core`: núcleo modular extraído e contratos de modularidade.
- `pjb-api`: aplicação Spring Boot, domínio, controllers, serviços, integrações e testes.
- `docs`: arquitetura, produto, segurança, infraestrutura, OpenAPI e relatórios.
- `scripts`: guardas de qualidade, segurança e automação do repositório.
- `infra`: Docker, Kubernetes e configuração operacional.
- `config`: Checkstyle, SpotBugs e políticas de qualidade.

## Como Validar

```powershell
.\mvnw.cmd test -DtrimStackTrace=false
```

Build sem repetir testes:

```powershell
.\mvnw.cmd -DskipTests package
```

## Segurança Do Git

O repositório possui um hook de pre-commit e scripts de sincronização segura. Antes de enviar código ao GitHub, a automação verifica segredos, chaves, tokens e dados pessoais.

Documento técnico:

```text
docs/security/GIT_SAFE_SYNC.md
```

## Critério De Organização

O projeto evita versionar artefatos gerados, dados reais, credenciais, logs e arquivos locais de IDE. A raiz é mantida enxuta; documentação detalhada fica em `docs`.
