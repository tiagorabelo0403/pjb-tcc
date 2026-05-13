# Platform Structural Decomposition — Round 111

## Escopo
Esta rodada atacou a concentração residual da `QualifiedSignatureIdentityContextService`, que ainda misturava três subdomínios distintos em um único arquivo:

- resolução de lotação institucional
- materialização do contexto do certificado de entrada
- coerência entre identidade do usuário e identidade do certificado

## Mudanças aplicadas

### 1. Decomposição da `QualifiedSignatureIdentityContextService`
Foram extraídos suportes dedicados:

- `QualifiedSignatureInstitutionalAssignmentSupport`
- `QualifiedSignatureCertificateContextSupport`
- `QualifiedSignaturePersonIdentitySupport`

A classe principal passou a operar como orquestradora curta, preservando o contrato externo `resolve(...)` e delegando os subdomínios extraídos.

### 2. Extrações realizadas

#### `QualifiedSignatureInstitutionalAssignmentSupport`
Assumiu:

- refinamento do papel detalhado para secretarias especializadas
- leitura de pistas institucionais
- classificação de secretaria especializada
- resolução de lotação institucional
- resolução do órgão assinante
- composição do label de lotação
- fallback de tribunal padrão

#### `QualifiedSignatureCertificateContextSupport`
Assumiu:

- leitura do certificado via atributo servlet
- leitura do certificado PEM por header
- parsing de DN
- inferência de hints de papel/jurisdição
- coleta de SANs
- materialização do `EntryCertificateContext`

#### `QualifiedSignaturePersonIdentitySupport`
Assumiu:

- comparação nome/CPF/email/OAB/registro
- scoring de confiança
- composição do payload de coerência
- materialização do `ResolvedPersonIdentity`

## Resultado objetivo

- `QualifiedSignatureIdentityContextService`: `1678 -> 782` linhas
- a classe saiu da faixa de hotspot estrutural por tamanho
- o sweep de concorrência continua sem fronteiras assíncronas cruas fora da espinha permitida
- o guard transacional continua sem budgets faltantes nos hotspots mapeados

## Travas adicionadas

- `QualifiedSignatureIdentityContextRefinementArchitectureTest`
- atualização do `README.md` com memória de continuidade, rounds anteriores e próximos alvos técnicos

## Validação executada nesta rodada

- execução dos guards Python:
  - `architecture_hygiene_guard.py`
  - `constructor_injection_guard.py`
  - `runtime_concurrency_guard.py`
  - `transactional_hotspot_guard.py`
- pré-checagem de trailing whitespace nos arquivos alterados
- `git diff --cached --check`
- commit local validado:
  - `20c00c1` — `Round 111 - decompose qualified signature identity context`

## Pendências explícitas
Os maiores hotspots remanescentes continuam concentrados em:

- `SecretariatQueueQueryService`
- `TransitoJulgadoArquivamentoEngine`
- `TribunalRuleEngine`
- `PeticionamentoSessaoFacadeService`
- `NationalCommunicationInstitutionalSurfaceFacadeService`
- `NationalCommunicationInstitutionalGovernanceSurfaceFacadeService`
- `PjbArquiteturaSubstituicaoNacionalApplicationService`

## Observação honesta
A validação de build/teste completo com Maven não pôde ser executada neste ambiente porque o wrapper depende de resolução externa de artefatos e a rede está bloqueada.
