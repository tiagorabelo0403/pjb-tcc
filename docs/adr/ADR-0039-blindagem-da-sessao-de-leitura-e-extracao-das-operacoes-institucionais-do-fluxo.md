# ADR-0039 — blindagem da sessão de leitura e extração das operações institucionais do fluxo nacional de comunicação

## Status
Aprovado

## Contexto
Após a rodada anterior, os pontos de entrada já estavam mais curtos, mas dois colaboradores ainda concentravam responsabilidades demais:

- `ProcessReadingWorkspaceFacade` ainda misturava carregamento protegido de contexto, resolução de sessão do leitor e projeção do workspace
- `NationalCommunicationFlowFacade` ainda misturava expedição/roteamento processual com toda a frente institucional de caixas, inbox, auditoria, provas, gates, queue, integrações externas, observabilidade e hardening

Esse desenho mantinha acoplamento alto em duas trust boundaries importantes:

- a sessão do painel de leitura, que depende de sigilo, autorização, modo e preset
- a operação institucional do fluxo de comunicação, que depende de acesso, visibilidade, concorrência, auditoria e observabilidade

## Decisão
Foram adotadas as seguintes decisões estruturais:

1. Extrair o carregamento e a composição da sessão do painel de leitura para `ProcessReadingWorkspaceSessionResolver`
2. Extrair a composição do catálogo de presets para `ProcessReadingPresetCatalogResolver`
3. Isolar snapshots/records próprios para o eixo de leitura:
   - `ProcessReadingWorkspaceContext`
   - `ProcessReadingWorkspaceSession`
   - `ProcessReadingPageCounter`
4. Extrair a frente institucional operacional do fluxo nacional de comunicação para `NationalCommunicationInstitutionalOperationsFacade`
5. Manter a `NationalCommunicationFlowFacade` focada em expedição, destinatário processual, ato canônico, roteamento institucional, fallback e painel

## Consequências
### Positivas
- menor acoplamento entre carregamento seguro e projeção do painel de leitura
- menor acoplamento entre expedição processual e operação institucional do fluxo nacional de comunicação
- trust boundaries mais explícitas
- maior previsibilidade para testes de separação estrutural
- melhor preparação para futura extração por capacidade dentro de um monólito modular forte

### Custos
- aumento controlado do número de colaboradores e records internos
- necessidade de manter testes de governança para evitar recontaminação das fachadas principais

## Notas de implementação
A rodada preserva contratos públicos e mantém a orientação do projeto:

- Java 21
- DDD
- sem comentários no código de produção
- robustez governamental/militar
- sem espalhar política de concorrência
- sem realocar mensagens operacionais para lugares improvisados
