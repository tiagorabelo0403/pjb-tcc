# Marketplace — Fase 2 (complementação documental) + retrofit de jus postulandi na Fase 1

Data: 2026-08-05
Status: aprovado para implementação

## Contexto

`D-marketplace-sem-completude-documental` tem Fase 1 fechada (sinal síncrono/assíncrono de
completude documental no protocolo). O próprio `DEBT_LOG.md` reserva a Fase 2 por nome: endpoint
`POST /processos/{id}/documentos` para complementação pós-protocolo, evento
`PROCESSO_DOCUMENTACAO_COMPLETADA`.

Investigação desta fatia encontrou três coisas que mudam o desenho original:

1. **`POST /api/v1/processos/{id}/documentos` já existe** (`PastaDigitalController`), mas é do canal
   interno (usuário logado, `authorizationService.requireWriteProcesso`). O endpoint novo do
   marketplace precisa de rota própria sob `/api/marketplace/v1` para não colidir e para usar o
   modelo de autenticação OAuth2 de cliente, não de usuário.
2. **A Fase 1 nunca persiste os documentos declarados no protocolo** — `documentos` é usado uma vez
   em `CompletudeDocumentalPolicyService.diagnosticar` e descartado. Não existe hoje nenhum
   `DocumentoProcessual` gerado pelo canal marketplace.
3. **`ApiMarketplaceService.protocolar()` usa a sobrecarga de 2 argumentos de `diagnosticar()`**, que
   nunca dispensa `PROCURACAO` para jus postulandi. Existe uma sobrecarga de 3 argumentos que já faz
   essa dispensa, mas ela depende de um `InstrumentoRepresentacaoProcessual` resolvido — e
   `RepresentacaoProcessualPolicyService` tem uma sobrecarga de `resolve(...)` baseada em primitivos
   (`String ramoRaw, String ritoRaw, String tribunalRaw, TipoUsuario perfilAtor, ...`) que não exige
   `Processo`/`Usuario` — construída exatamente para contextos sem entidade JPA carregada, como o
   marketplace.

## Escopo desta fatia

Dentro:
- Retrofit da Fase 1: campo aditivo `perfilAtor` em `MarketplaceProtocoloRequest`, resolução correta
  de `InstrumentoRepresentacaoProcessual` via a sobrecarga primitiva de
  `RepresentacaoProcessualPolicyService.resolve`, troca da chamada de `diagnosticar` para a
  sobrecarga de 3 argumentos — em `protocolar()` e no novo endpoint.
- Endpoint novo `POST /api/marketplace/v1/processos/{id}/documentos` com armazenamento real via
  `ObjectStoragePort`, validação de conteúdo, classificação de sigilo, auditoria, rate limiting,
  recálculo de completude e evento de conclusão.
- Migration nova: coluna `tipo_documento` em `tb_documento_processual`.
- Extração de um validador de conteúdo compartilhado entre `PastaDigitalService` e o fluxo novo do
  marketplace (mesma regra, um lugar só).

Fora (decisão explícita, não dívida silenciosa):
- Consolidação das três políticas de completude documental (catálogo estático, `tb_requisito_documental`,
  marketplace) numa fonte única — like já registrado no `DEBT_LOG` como fatia futura própria,
  candidato natural é `ProtocoloCompletudeValidator`. Não é tocado aqui.
- Verificação real de OAB do `perfilAtor` declarado pelo cliente marketplace — o campo é
  autodeclarado pelo integrador (o marketplace não tem cadastro de usuário para validar contra).
  Mesmo nível de confiança que o resto do payload de `MarketplaceProtocoloRequest` (nome da parte,
  CPF etc.), que já são autodeclarados sem verificação externa síncrona.
- Backends de `ObjectStoragePort` além de `LocalFilesystemObjectStorageAdapter` (S3/MinIO) — fora do
  escopo desta fatia, a porta já é a abstração correta e o adapter é trocável depois sem tocar quem
  a consome.

## 1. Retrofit da Fase 1 — jus postulandi

`model/dto/processo/marketplace/MarketplaceProtocoloRequest.java` (e o record aninhado espelhado em
`ApiMarketplaceService`) ganham campo aditivo:

```java
String perfilAtor // nullable; valores esperados: nomes de TipoUsuario (ex.: "CIDADAO", "ADVOGADO")
```

Em `ApiMarketplaceService.protocolar()`, antes de chamar `diagnosticar`:

```java
TipoUsuario perfil = TipoUsuario.fromString(request.perfilAtor()); // null-safe, retorna null se não reconhecer
var policy = representacaoProcessualPolicyService.resolve(
        processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
        processo.getRito() == null ? null : processo.getRito().name(),
        processo.getTribunal(),
        perfil,
        null, null, null, false, false, null, null);
InstrumentoRepresentacaoProcessual instrumento = policy.regularidadeSuficiente()
        ? InstrumentoRepresentacaoProcessual.fromString(policy.resolvedInstrument())
        : null;

var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), request.documentos(), instrumento);
```

Sem `perfilAtor` (cliente não migrou): `TipoUsuario.fromString(null)` retorna `null`,
`resolveInstrument` cai no default `MANDATO_AD_JUDICIA`, `dispensaMandatoFormal()` é `false` —
comportamento idêntico ao atual (PROCURACAO sempre exigida). Retrocompatível por construção, não por
teste manual.

O mesmo bloco de resolução é extraído para um método privado reaproveitado pelo endpoint de
documentos (item 3).

## 2. Endpoint novo — `POST /api/marketplace/v1/processos/{id}/documentos`

`ApiMarketplaceController` ganha o método; `MarketplaceSurfaceFacadeService` ganha o método
`complementarDocumentos` espelhando `protocolar`; `ApiMarketplaceService` ganha o método
`complementarDocumentos(Long processoId, List<Attachment> documentos, String clientId)`.

Request/response (records novos, mesmo pacote de `MarketplaceProtocoloRequest`):

```java
public record MarketplaceComplementoDocumentalRequest(@NotEmpty List<Attachment> documentos) {}

public record MarketplaceComplementoDocumentalResponse(
        Long processoId,
        String numeroProcesso,
        String status,
        boolean documentacaoCompleta,
        List<String> documentosFaltantes,
        List<String> documentosRecebidos,
        LocalDateTime recebidoEm) {}
```

`documentosFaltantes` e `documentosRecebidos` são ambos `TipoDocumento.name()` — mesmo formato que
`MarketplaceProtocoloResponse.documentosFaltantes` já usa hoje, para consistência entre os dois
endpoints. `documentosRecebidos` lista só os `tipoDocumento` efetivamente gravados nesta chamada
(exclui duplicatas ignoradas por SHA-256 repetido).

### Fluxo, em ordem

1. `rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "marketplace_complementar_documentos", ApiVersion.V1)`
   — mesmo domínio usado por `protocolar`.
2. Resolve `clientId` (mesmo padrão do controller: `Authentication` ou
   `marketplaceOAuth2Service.authorizeHttpRequest`).
3. Busca `Processo` por id. Não encontrado, ou
   `!processo.getConnectorProtocolReference().startsWith(clientId + ":")` → lança
   `RecursoNaoEncontradoException` (mapeada para **404**, mensagem "Processo não encontrado para este
   cliente." — não revela que o id pertence a outro cliente).
4. `!"PENDENTE_DOCUMENTACAO".equals(processo.getConnectorSubmissionStatus())` → lança
   `ProcessoDocumentacaoCompletaException` (nova, `service/exception/`), mapeada em
   `ApiExceptionHandler` seguindo o mesmo padrão de `RecursoJaExistenteException` (linha 197-200):
   `build(HttpStatus.CONFLICT, "conflict", safeMessage(ex), request, null)`, mensagem "Este processo
   já está com a documentação completa — nenhuma ação necessária."
5. Para cada `Attachment` em `documentos`:
   - `tipoDocumento() == null` → **400** (`ErroDeValidacaoException`, `CAMPO_OBRIGATORIO`,
     "tipoDocumento obrigatório para cada documento enviado").
   - `DocumentContentValidator.validar(attachment)` (novo componente, item 4) → mesmas regras do
     `PastaDigitalService`: PDF ou tipo aceito, ≤ 5MB, não criptografado, não corrompido. Falha →
     `ErroDeValidacaoException` reaproveitando os mesmos `TipoErroValidacao` que `PastaDigitalService`
     já usa (`FORMATO_INVALIDO`, `TAMANHO_EXCEDIDO`, `ARQUIVO_PROTEGIDO`, `ARQUIVO_CORROMPIDO`).
   - SHA-256 do conteúdo já existe pra esse `processoId`
     (`documentoRepository.existsByProcessoIdAndSha256`) → ignora como duplicata, não grava de novo,
     não entra em `documentosRecebidos`.
   - Novo: `DocumentoSigiloClassifier.classify(...)` (mesma lógica do `PastaDigitalService`) define
     `categoria`/`nivelSigilo`.
   - Grava via `ObjectStoragePort.put(key, ...)` — chave `"marketplace/" + processoId + "/" + uuid`.
   - Persiste `DocumentoProcessual` com `storageBackend="LOCALFS"`, `storageUri=<key>`,
     `tipoDocumento=attachment.getTipoDocumento()`, `origemSistema="MARKETPLACE_API"`,
     `criadoEm=LocalDateTime.now()`. **Sem** `pdf` bytea inline.
6. Recalcula: junta `tipoDocumento` de todos os `DocumentoProcessual` do processo
   (`documentoRepository.findByProcessoId(processoId)`, filtra `tipoDocumento != null`) num
   `Set<TipoDocumento>`. Resolve `instrumentoRepresentacao` (mesmo bloco do item 1, usando
   `processo.getRamoDireito()/getRito()/getTribunal()` — `perfilAtor` não é reenviado no complemento,
   fica congelado no que foi resolvido no protocolo original, guardado num campo novo
   `Processo.instrumentoRepresentacaoResolvido` (String, nullable) setado em `protocolar()`).
   Chama `completudeDocumentalPolicyService.diagnosticar(rito, tiposPresentes, instrumento)` — nova
   sobrecarga (item 5).
7. Se `!diagnostico.bloqueante()`:
   `processo.setConnectorSubmissionStatus("RECEBIDO_MARKETPLACE")`,
   `connectorSubmissionMessage` atualizada ("Documentação completada via marketplace em <data>."),
   `governanceService.publicarEventoDocumentacaoCompletada(...)` (novo método, espelha
   `publicarEventoPendenciaDocumental`, `eventType="PROCESSO_DOCUMENTACAO_COMPLETADA"`).
   Senão: mantém `PENDENTE_DOCUMENTACAO`, `governanceService.publicarEventoPendenciaDocumental(...)`
   de novo com a lista atualizada de faltantes.
8. `auditLedger.appendSafely("MARKETPLACE_DOCUMENTOS_COMPLEMENTADOS", "PROCESSO", processoId,
   "cliente=" + clientId + " recebidos=" + documentosRecebidos.size() + " completo=" + documentacaoCompleta)`.
9. `processoRepository.save(processo)`, retorna a resposta.

## 3. Migration V308

`tb_documento_processual` ganha `tipo_documento VARCHAR(60) NULL` (nome canônico do enum
`TipoDocumento`). Nullable porque documentos do canal interno (`PastaDigitalService`) continuam sem
esse campo — não é obrigatório fora do fluxo de completude.

`tb_processo` ganha `instrumento_representacao_resolvido VARCHAR(60) NULL` — congela o instrumento
resolvido no protocolo, reaproveitado no complemento sem precisar o cliente reenviar `perfilAtor`.

## 4. `DocumentContentValidator` (extração)

**Achado do self-review:** não existe hoje nenhum teste para `PastaDigitalService` nem
`PastaDigitalController` (`Glob`/`Grep` em `pjb-api/src/test` não retornou nada). Extrair a validação
de dentro de `anexarDocumentoPdf` sem rede de segurança seria mover código sensível (limite de
tamanho, detecção de criptografia, PDF corrompido) sem prova de que o comportamento foi preservado.

Ordem obrigatória desta parte da fatia:
1. Escreve `PastaDigitalServiceTest` (unit, Mockito) cobrindo o comportamento **atual** de
   `anexarDocumentoPdf`: PDF válido, arquivo vazio, acima de 5MB, não-PDF, criptografado, corrompido
   (0 páginas), duplicata por SHA-256 — contra o código como está hoje, antes de qualquer extração.
2. Só então extrai `DocumentContentValidator` (`@Component`, `service/document/`) com a lógica de
   `isPdf`/`LIMITE_BYTES`/checagem de criptografado/checagem de 0 páginas.
   `PastaDigitalService.anexarDocumentoPdf` passa a chamar o componente novo.
3. Reroda `PastaDigitalServiceTest` — mesmo comportamento, mesmo resultado, prova que a extração não
   mudou nada.

Isso dá ao `PastaDigitalService` a cobertura que faltava (dívida pré-existente, não desta fatia, mas
que a fatia teria escancarado se ignorada) e garante que o validador compartilhado nasce testado nos
dois lados desde o primeiro commit.

## 5. `CompletudeDocumentalPolicyService` — nova sobrecarga

```java
public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito,
                                                     Set<TipoDocumento> tiposPresentes,
                                                     InstrumentoRepresentacaoProcessual instrumentoRepresentacao)
```

As duas sobrecargas existentes (`List<Attachment>`) passam a extrair o `Set<TipoDocumento>` e delegar
para esta — mesmo corpo de regra, um lugar só faz o cálculo de fato.

## Matriz de erro

| Situação | Status | Corpo |
|---|---|---|
| Processo não pertence ao cliente / não existe | 404 | "Processo não encontrado para este cliente." |
| Processo não está `PENDENTE_DOCUMENTACAO` | 409 | "Este processo já está com a documentação completa — nenhuma ação necessária." |
| Anexo sem `tipoDocumento` | 400 | `CAMPO_OBRIGATORIO` |
| Anexo não-PDF / corrompido / criptografado / >5MB | 400/413 (conforme `TipoErroValidacao` já mapeia) | mesmas mensagens do `PastaDigitalService` |
| Sucesso, ainda incompleto | 200 | `documentacaoCompleta=false`, `documentosFaltantes` atualizado |
| Sucesso, completou | 200 | `documentacaoCompleta=true`, status `RECEBIDO_MARKETPLACE` |

## Plano de testes

- `ApiMarketplaceServiceCompletudeDocumentalUnitTest` (Fase 1, existente): acrescenta casos com
  `perfilAtor="CIDADAO"` em rito trabalhista/juizado dispensando `PROCURACAO`, e caso sem
  `perfilAtor` provando que nada muda (regressão do comportamento atual).
- Novo `ApiMarketplaceServiceComplementoDocumentalUnitTest` (Mockito puro): posse negada (404),
  estado não-pendente (409), anexo sem tipo (400), completude parcial (mantém pendente, re-dispara
  evento), completude total (muda status, dispara evento novo), duplicata por SHA-256 ignorada.
- Novo `ApiMarketplaceServiceComplementoDocumentalIT` (Testcontainers): grava de verdade via
  `ObjectStoragePort` local, lê de volta via `DocumentContentService.resolvePdf`, confirma
  `DocumentoProcessual` persistido com `tipoDocumento`/`storageUri` corretos.
- `DocumentContentValidatorTest` (novo, unit): casos de PDF válido, corrompido, criptografado,
  acima do limite — mesmos fixtures que já existem para `PastaDigitalService`, reaproveitados.
- Regressão: `PastaDigitalServiceTest` (novo, escrito antes da extração — ver seção 4) permanece verde
  depois que `anexarDocumentoPdf` passa a delegar para `DocumentContentValidator` — mesmo
  comportamento, código movido, não reescrito.
