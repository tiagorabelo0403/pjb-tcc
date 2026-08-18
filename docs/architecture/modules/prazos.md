# Modulo Prazos

## 1. Responsabilidade

O modulo `prazos` cria uma fronteira modular para calculo de prazo processual e analise de dia forense. Ele nao substitui o motor legado nesta onda; ele encapsula o acesso por port, policy e adapter para impedir que novos modulos acessem diretamente services, enums e DTOs legados.

## 2. Fronteira

- `domain`: valida janela operacional, normaliza parametros e decide quando exige conferencia manual.
- `application`: oferece caso de uso transacional somente leitura para calculo de prazo e dia forense.
- `api`: publica comandos e resultados internos sem entity JPA.
- `infrastructure`: adapta `PrazoProcessualNacionalService` para o contrato modular.
- `web`: nao criado nesta onda, porque endpoints antigos continuam funcionando.

## 3. Por que nao mover o legado agora

O nucleo de prazos ja existe em `core/prazos`, `platform/jusos` e `service/processual/prazo`. Mover essas classes em massa teria risco alto de quebrar calculo, recursal, calendario forense e controllers existentes. A decisao segura foi criar uma fachada modular de consumo futuro.

## 4. Conexoes com processo

Nesta onda, o modulo nao consulta processo diretamente. O comando recebe parametros processuais ja resolvidos: tipo de prazo, ramo, grau, tribunal, UF, comarca e data inicial. Uma proxima onda pode criar adapter para obter contexto por `processoId`.

## 5. Conexoes com notificacoes

Notificacoes ficaram fora do codigo desta onda. A fronteira criada permite que um modulo de notificacao consuma resultado de prazo sem conhecer `PrazoProcessualNacionalService` ou DTO HTTP legado.

## 6. Ports e adapters

- `PrazoProcessualPort`: contrato interno para calculo de prazo e analise de dia forense.
- `LegacyPrazoProcessualAdapter`: adapter em `infrastructure` que converte strings do contrato modular para enums legados e chama `PrazoProcessualNacionalService`.

## 7. Policies

`PrazoProcessualBoundaryPolicy` valida:

- data obrigatoria dentro da janela operacional;
- tipo de prazo obrigatorio;
- ramo e grau obrigatorios;
- codigo de tribunal obrigatorio e limitado;
- UF opcional com duas letras;
- comarca opcional limitada;
- `diasOverride` entre 1 e 3650;
- conferencia manual quando ha override, marco inicial nao util ou advertencia.

## 8. Tabelas

Nenhuma tabela nova foi criada. Esta onda nao altera persistencia nem migrations.

## 9. Services

- `PrazoProcessualApplicationService`
- `PrazoProcessualBoundaryPolicy`
- `LegacyPrazoProcessualAdapter`

## 10. Eventos

Nenhum evento novo foi publicado nesta onda. A integracao com notificacoes deve ser desenhada depois por evento ou facade propria.

## 11. Testes

- Teste de policy de dominio.
- Teste de application service usando port fake.
- Teste de adapter contra service legado mockado.
- Teste ArchUnit do modulo `prazos`.

## 12. Riscos

- O contrato usa strings para evitar vazar enums legados, entao o adapter ainda precisa mapear nomes validos.
- O modulo ainda nao resolve prazo por `processoId`.
- Notificacao e auditoria global permanecem fora desta rodada.

## 13. Proxima fase

Criar a fronteira de notificacoes consumindo resultados de prazo, sem acessar repositories legados diretamente, e depois migrar um fluxo pequeno de alerta de prazo para essa nova fronteira.
