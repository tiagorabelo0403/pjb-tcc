# Marketplace Fase 2 — Complementação Documental Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar `POST /api/marketplace/v1/processos/{id}/documentos` para complementação documental pós-protocolo com armazenamento real, e corrigir o bug de jus postulandi herdado da Fase 1 (`ApiMarketplaceService.protocolar()` nunca dispensava `PROCURACAO`).

**Architecture:** Endpoint novo isolado num serviço próprio (`MarketplaceDocumentoComplementarService`), não em `ApiMarketplaceService` (evita inflar suas dependências com 6 componentes não usados por `protocolar()`). Reaproveita infraestrutura já existente no projeto: `ObjectStoragePort` para armazenamento real, `RepresentacaoProcessualPolicyService` (sobrecarga baseada em primitivos, sem `Usuario`) para resolver jus postulandi, `DocumentoSigiloClassifier` e um `DocumentContentValidator` extraído do `PastaDigitalService` para validação/classificação — mesma paridade de segurança do canal interno.

**Tech Stack:** Java 21, Spring Boot 3.x, PDFBox (já dependência do projeto), Flyway, JUnit 5 + Mockito + AssertJ, Testcontainers Postgres.

## Global Constraints

- DI exclusivamente por construtor — zero `@Autowired` em fields (CLAUDE.md).
- Zero comentários redundantes no código.
- O número de falhas de teste JAMAIS pode aumentar — cada task roda os testes afetados antes de seguir.
- Transações curtas — sem I/O pesado (gravação em `ObjectStoragePort`) dentro de `@Transactional` de banco quando evitável; ver Task 10 para a decisão específica.
- Migration nova numerada `V308` (última existente é `V307__ai_vector_store_pgvector.sql`).
- Todo campo novo em request/response DTO é aditivo — nenhum contrato existente muda de forma quebradiça.
- NUNCA commitar sem "APROVADO" explícito do usuário — cada task abaixo termina com um passo de commit que só deve rodar após esse sinal ter sido dado para a task (não é preciso pedir de novo por task se o usuário já autorizou a fatia inteira, mas o executor deve parar e confirmar se houver qualquer dúvida sobre o que foi aprovado).

---

## Task 1: Rede de segurança para `PastaDigitalService` (não existe teste hoje)

**Files:**
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/service/pastadigital/PastaDigitalServiceTest.java`

**Interfaces:**
- Consumes: `PastaDigitalService.anexarDocumentoPdf(Long processoId, MultipartFile arquivo, String titulo, Long criadoPor, String origemSistema, String categoriaRaw, String nivelSigiloRaw)` — assinatura já existente, sem mudança nesta task.

- [ ] **Step 1: Escrever os testes contra o comportamento atual**

```java
package com.tcc.pjb.backend.service.pastadigital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class PastaDigitalServiceTest {

    private ProcessoRepository processoRepository;
    private DocumentoProcessualRepository documentoRepository;
    private DocumentoPaginaRepository paginaRepository;
    private PjbAuthorizationService authorizationService;
    private PastaDigitalService service;

    @BeforeEach
    void setUp() {
        processoRepository = mock(ProcessoRepository.class);
        documentoRepository = mock(DocumentoProcessualRepository.class);
        paginaRepository = mock(DocumentoPaginaRepository.class);
        authorizationService = mock(PjbAuthorizationService.class);
        Processo processo = Processo.builder().id(1L).build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        doNothing().when(authorizationService).requireWriteProcesso(processo);
        when(documentoRepository.existsByProcessoIdAndSha256(any(), any())).thenReturn(false);
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            DocumentoProcessual d = inv.getArgument(0);
            d.setId(java.util.UUID.randomUUID());
            return d;
        });
        service = new PastaDigitalService(processoRepository, documentoRepository, paginaRepository,
                authorizationService, new DocumentoSigiloClassifier());
    }

    @Test
    void anexaPdfValidoComUmaPaginaComSucesso() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "peticao.pdf",
                "application/pdf", pdfComPaginas(1));

        var resp = service.anexarDocumentoPdf(1L, arquivo, "Peticao", 10L, "API", null, null);

        assertThat(resp.getNumeroPaginas()).isEqualTo(1);
        assertThat(resp.getNomeOriginal()).isEqualTo("peticao.pdf");
        verify(documentoRepository).save(any());
    }

    @Test
    void rejeitaArquivoVazio() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "vazio.pdf",
                "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("ausente ou vazio");
    }

    @Test
    void rejeitaArquivoAcimaDoLimiteDeTamanho() {
        byte[] grande = new byte[6 * 1024 * 1024];
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "grande.pdf",
                "application/pdf", grande);

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("Tamanho");
    }

    @Test
    void rejeitaArquivoNaoPdf() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "foto.png",
                "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("Formato");
    }

    @Test
    void rejeitaPdfCriptografado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "protegido.pdf",
                "application/pdf", pdfCriptografado());

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("senha");
    }

    @Test
    void rejeitaPdfSemPaginas() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "corrompido.pdf",
                "application/pdf", pdfComPaginas(0));

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("0 páginas");
    }

    @Test
    void rejeitaDuplicataPorSha256() throws Exception {
        when(documentoRepository.existsByProcessoIdAndSha256(any(), any())).thenReturn(true);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "repetido.pdf",
                "application/pdf", pdfComPaginas(1));

        assertThatThrownBy(() -> service.anexarDocumentoPdf(1L, arquivo, null, 10L, "API", null, null))
                .isInstanceOf(ErroDeValidacaoException.class)
                .hasMessageContaining("mesma hash");
    }

    private static byte[] pdfComPaginas(int quantidade) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < quantidade; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static byte[] pdfCriptografado() throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage());
            AccessPermission ap = new AccessPermission();
            StandardProtectionPolicy spp = new StandardProtectionPolicy("owner-pass", "user-pass", ap);
            doc.protect(spp);
            doc.save(out);
            return out.toByteArray();
        }
    }
}
```

- [ ] **Step 2: Rodar e confirmar que passa contra o código atual**

Run: `./mvnw test -pl pjb-api -Dtest=PastaDigitalServiceTest`
Expected: 7/7 verde (o teste é escrito contra o comportamento já existente, não deve falhar).

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/test/java/com/tcc/pjb/backend/service/pastadigital/PastaDigitalServiceTest.java
git commit -m "test(pastadigital): cobre PastaDigitalService antes de extrair validador compartilhado"
```

---

## Task 2: Extrair `DocumentContentValidator` e religar `PastaDigitalService`

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/service/document/DocumentContentValidator.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/pastadigital/PastaDigitalService.java`

**Interfaces:**
- Produces: `DocumentContentValidator.validarTamanho(long tamanhoBytes, String nomeOriginal)`,
  `validarExtensaoOuContentType(String nomeOriginal, String contentTypeDeclarado)`,
  `validarEstruturaPdf(byte[] bytes, String nomeOriginal) throws IOException` retornando
  `DocumentContentValidator.ValidatedPdf(int numeroPaginas)`. Usado por `PastaDigitalService` (esta
  task) e por `MarketplaceDocumentoComplementarService` (Task 10).

- [ ] **Step 1: Criar o validador**

```java
package com.tcc.pjb.backend.service.document;

import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class DocumentContentValidator {

    private static final long LIMITE_BYTES = 5L * 1024L * 1024L;

    public record ValidatedPdf(int numeroPaginas) {
    }

    public void validarTamanho(long tamanhoBytes, String nomeOriginal) {
        if (tamanhoBytes <= 0) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "arquivo")
                    .addMetadado("motivo", "arquivo ausente ou vazio");
        }
        if (tamanhoBytes > LIMITE_BYTES) {
            throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, nomeOriginal)
                    .addMetadado("tamanho_atual", tamanhoBytes)
                    .addMetadado("tamanho_limite", LIMITE_BYTES);
        }
    }

    public void validarExtensaoOuContentType(String nomeOriginal, String contentTypeDeclarado) {
        boolean pdf = (contentTypeDeclarado != null && contentTypeDeclarado.equals(MediaType.APPLICATION_PDF_VALUE))
                || (nomeOriginal != null && nomeOriginal.toLowerCase(Locale.ROOT).endsWith(".pdf"));
        if (!pdf) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, nomeOriginal)
                    .addMetadado("tipo_recebido", contentTypeDeclarado)
                    .addMetadado("tipo_esperado", MediaType.APPLICATION_PDF_VALUE);
        }
    }

    public ValidatedPdf validarEstruturaPdf(byte[] bytes, String nomeOriginal) throws IOException {
        try (PDDocument pdf = Loader.loadPDF(bytes)) {
            if (pdf.isEncrypted()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, nomeOriginal)
                        .addMetadado("motivo", "Documento possui senha");
            }
            int n = pdf.getNumberOfPages();
            if (n <= 0) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal)
                        .addMetadado("motivo", "PDF com 0 páginas");
            }
            return new ValidatedPdf(n);
        }
    }
}
```

- [ ] **Step 2: Religar `PastaDigitalService.anexarDocumentoPdf` para usar o validador**

Em `pjb-api/src/main/java/com/tcc/pjb/backend/service/pastadigital/PastaDigitalService.java`,
adicionar o campo (o `@RequiredArgsConstructor` do Lombok já gera o construtor com ele):

```java
    private final com.tcc.pjb.backend.service.document.DocumentContentValidator contentValidator;
```

Substituir o bloco de validação — de:

```java
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, "arquivo")
                    .addMetadado("motivo", "arquivo ausente ou vazio");
        }
        if (arquivo.getSize() > LIMITE_BYTES) {
            throw new ErroDeValidacaoException(TipoErroValidacao.TAMANHO_EXCEDIDO, arquivo.getOriginalFilename())
                    .addMetadado("tamanho_atual", arquivo.getSize())
                    .addMetadado("tamanho_limite", LIMITE_BYTES);
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        String contentType = arquivo.getContentType();
        if (!isPdf(arquivo)) {
            throw new ErroDeValidacaoException(TipoErroValidacao.FORMATO_INVALIDO, nomeOriginal)
                    .addMetadado("tipo_recebido", contentType)
                    .addMetadado("tipo_esperado", MediaType.APPLICATION_PDF_VALUE);
        }
```

para:

```java
        contentValidator.validarTamanho(arquivo == null ? 0 : arquivo.getSize(), arquivo == null ? null : arquivo.getOriginalFilename());

        String nomeOriginal = arquivo.getOriginalFilename();
        String contentType = arquivo.getContentType();
        contentValidator.validarExtensaoOuContentType(nomeOriginal, contentType);
```

Dentro do `try`, substituir:

```java
            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                if (pdf.isEncrypted()) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_PROTEGIDO, nomeOriginal)
                            .addMetadado("motivo", "Documento possui senha");
                }
                int n = pdf.getNumberOfPages();
                if (n <= 0) {
                    throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nomeOriginal)
                            .addMetadado("motivo", "PDF com 0 páginas");
                }
```

por:

```java
            var validado = contentValidator.validarEstruturaPdf(bytes, nomeOriginal);
            try (PDDocument pdf = Loader.loadPDF(bytes)) {
                int n = validado.numeroPaginas();
```

Remover o método privado `isPdf(MultipartFile file)` (agora morto — a lógica está no validador) e a
constante `LIMITE_BYTES` da classe (também morta).

- [ ] **Step 3: Rodar `PastaDigitalServiceTest` de novo — mesmo resultado, prova que a extração não mudou nada**

Run: `./mvnw test -pl pjb-api -Dtest=PastaDigitalServiceTest`
Expected: 7/7 verde, idêntico à Task 1.

- [ ] **Step 4: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/document/DocumentContentValidator.java pjb-api/src/main/java/com/tcc/pjb/backend/service/pastadigital/PastaDigitalService.java
git commit -m "refactor(document): extrai DocumentContentValidator do PastaDigitalService"
```

---

## Task 3: `CompletudeDocumentalPolicyService` — sobrecarga baseada em `Set<TipoDocumento>`

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/completude/CompletudeDocumentalPolicyService.java`
- Modify: `pjb-api/src/test/java/com/tcc/pjb/backend/service/completude/CompletudeDocumentalPolicyServiceTest.java`

**Interfaces:**
- Produces: `diagnosticar(RitoProcessual rito, Set<TipoDocumento> tiposPresentes, InstrumentoRepresentacaoProcessual instrumentoRepresentacao)` — usado por `MarketplaceDocumentoComplementarService` (Task 10), que soma `DocumentoProcessual` persistidos em vez de `Attachment` transientes.

- [ ] **Step 1: Escrever o teste da nova sobrecarga**

Adicionar em `CompletudeDocumentalPolicyServiceTest.java` (mesmo arquivo, novos `@Test`):

```java
    @Test
    void diagnosticarPorConjuntoDeTiposProduzMesmoResultadoQuePorAnexos() {
        var porAnexos = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO, List.of(
                tipado(TipoDocumento.PETICAO_INICIAL),
                tipado(TipoDocumento.PROCURACAO),
                tipado(TipoDocumento.CTPS)
        ));

        var porConjunto = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO,
                java.util.Set.of(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO, TipoDocumento.CTPS),
                null);

        assertThat(porConjunto.bloqueante()).isEqualTo(porAnexos.bloqueante());
        assertThat(porConjunto.faltantes()).containsExactlyInAnyOrderElementsOf(porAnexos.faltantes());
    }

    @Test
    void diagnosticarPorConjuntoVazioDispensaProcuracaoComJusPostulandi() {
        var d = service.diagnosticar(RitoProcessual.TRABALHISTA_ORDINARIO,
                java.util.Set.of(TipoDocumento.PETICAO_INICIAL, TipoDocumento.CTPS, TipoDocumento.CALCULO_INICIAL),
                InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);

        assertThat(d.bloqueante()).isFalse();
    }
```

- [ ] **Step 2: Rodar e confirmar que falha (método ainda não existe)**

Run: `./mvnw test-compile -pl pjb-api`
Expected: erro de compilação — `diagnosticar(RitoProcessual, Set, InstrumentoRepresentacaoProcessual)` não existe.

- [ ] **Step 3: Implementar a sobrecarga e fazer as duas existentes delegarem para ela**

Em `CompletudeDocumentalPolicyService.java`, substituir o corpo do método de 3 argumentos existente:

```java
    public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito,
                                                        List<Attachment> anexos,
                                                        InstrumentoRepresentacaoProcessual instrumentoRepresentacao) {
        Set<TipoDocumento> presentes = (anexos == null ? List.<Attachment>of() : anexos)
                .stream()
                .map(Attachment::getTipoDocumento)
                .filter(t -> t != null)
                .collect(Collectors.toSet());
        return diagnosticar(rito, presentes, instrumentoRepresentacao);
    }

    public DiagnosticoCompletudeDocumental diagnosticar(RitoProcessual rito,
                                                        Set<TipoDocumento> tiposPresentes,
                                                        InstrumentoRepresentacaoProcessual instrumentoRepresentacao) {
        RitoProcessual ritoResolvido = rito == null ? RitoProcessual.COMUM_ORDINARIO : rito;
        boolean dispensaProcuracao = instrumentoRepresentacao != null && instrumentoRepresentacao.isJusPostulandi();

        List<TipoDocumento> obrigatorios = ProceduralCatalogSupport.snapshot(ritoResolvido)
                .documents()
                .stream()
                .filter(ProceduralCatalogSupport.DocumentSpec::required)
                .map(ProceduralCatalogSupport.DocumentSpec::code)
                .filter(code -> !(dispensaProcuracao && code == TipoDocumento.PROCURACAO))
                .toList();

        Set<TipoDocumento> presentes = tiposPresentes == null ? Set.of() : tiposPresentes;

        List<TipoDocumento> faltantes = obrigatorios.stream()
                .filter(req -> !presentes.contains(req))
                .toList();

        return new DiagnosticoCompletudeDocumental(!faltantes.isEmpty(), faltantes, ritoResolvido);
    }
```

Remover o antigo corpo de `diagnosticar(rito, anexos, instrumento)` que calculava `presentes` inline
(agora feito pela delegação acima). O método de 2 argumentos (`diagnosticar(rito, anexos)`) continua
delegando para o de 3 (`return diagnosticar(rito, anexos, null);`) — sem mudança nessa parte.

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `./mvnw test -pl pjb-api -Dtest=CompletudeDocumentalPolicyServiceTest`
Expected: 13/13 verde (11 existentes + 2 novos).

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/completude/CompletudeDocumentalPolicyService.java pjb-api/src/test/java/com/tcc/pjb/backend/service/completude/CompletudeDocumentalPolicyServiceTest.java
git commit -m "feat(completude): sobrecarga de diagnosticar por Set<TipoDocumento>"
```

---

## Task 4: Migration V308 + colunas novas nas entidades

**Files:**
- Create: `pjb-api/src/main/resources/db/migration/V308__marketplace_documento_complementar.sql`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/document/DocumentoProcessual.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Processo.java`

**Interfaces:**
- Produces: `DocumentoProcessual.getTipoDocumento()/setTipoDocumento(TipoDocumento)`,
  `Processo.getInstrumentoRepresentacaoResolvido()/setInstrumentoRepresentacaoResolvido(String)` —
  usados por `MarketplaceDocumentoComplementarService` (Task 10) e `ApiMarketplaceService.protocolar`
  (Task 6).

- [ ] **Step 1: Migration**

```sql
alter table tb_documento_processual
    add column if not exists tipo_documento varchar(60);

alter table tb_processo
    add column if not exists instrumento_representacao_resolvido varchar(60);
```

- [ ] **Step 2: Campo novo em `DocumentoProcessual`**

Adicionar logo após o campo `categoria`:

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento")
    private com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento tipoDocumento;
```

E no builder interno (`DocumentoProcessualBuilder`), adicionar:

```java
        public DocumentoProcessualBuilder tipoDocumento(com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento tipoDocumento) { target.tipoDocumento = tipoDocumento; return this; }
```

(A classe já tem `@Getter @Setter` a nível de classe — não precisa de getter/setter manual.)

- [ ] **Step 3: Campo novo em `Processo`**

Adicionar logo após `connectorSubmissionStatus` (linha ~234):

```java
    @Column(name = "instrumento_representacao_resolvido")
    private String instrumentoRepresentacaoResolvido;
```

(`Processo` já tem `@Getter @Setter @Builder` a nível de classe — sem acessores manuais.)

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: compila limpo (mudança aditiva, nenhum construtor posicional de `DocumentoProcessual` ou
`Processo` é afetado — ambos usam `@Builder`/builder interno, não construtor posicional direto nos
call sites).

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/resources/db/migration/V308__marketplace_documento_complementar.sql pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/document/DocumentoProcessual.java pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Processo.java
git commit -m "feat(schema): tipo_documento em tb_documento_processual e instrumento_representacao_resolvido em tb_processo"
```

---

## Task 5: `MarketplaceRepresentacaoResolver` — resolução de jus postulandi sem `Usuario`

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceRepresentacaoResolver.java`
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceRepresentacaoResolverTest.java`

**Interfaces:**
- Produces: `resolve(RamoDireito ramo, RitoProcessual rito, String tribunal, String perfilAtorRaw)` →
  `InstrumentoRepresentacaoProcessual` (nullable). Usado por `ApiMarketplaceService.protocolar`
  (Task 6).
- Consumes: `RepresentacaoProcessualPolicyService.resolve(String, String, String, TipoUsuario, ...)` —
  sobrecarga já existente, sem mudança.

- [ ] **Step 1: Escrever o teste**

```java
package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import org.junit.jupiter.api.Test;

class MarketplaceRepresentacaoResolverTest {

    private final MarketplaceRepresentacaoResolver resolver =
            new MarketplaceRepresentacaoResolver(new RepresentacaoProcessualPolicyService());

    @Test
    void semPerfilAtorCaiNoDefaultDeMandatoAdJudicia() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, "TRT7", null);

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA);
    }

    @Test
    void perfilCidadaoEmRitoTrabalhistaResolveJusPostulandi() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, "TRT7", "CIDADAO");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_TRABALHISTA);
    }

    @Test
    void perfilCidadaoEmJuizadoEspecialCivelResolveJusPostulandiDoJuizado() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, "TJCE", "CIDADAO");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO);
    }

    @Test
    void perfilAtorInvalidoEhTratadoComoAusente() {
        InstrumentoRepresentacaoProcessual resolvido = resolver.resolve(
                RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, "TJCE", "valor-desconhecido-invalido");

        assertThat(resolvido).isEqualTo(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA);
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha (classe ainda não existe)**

Run: `./mvnw test-compile -pl pjb-api`
Expected: erro de compilação — `MarketplaceRepresentacaoResolver` não existe.

- [ ] **Step 3: Implementar**

```java
package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceRepresentacaoResolver {

    private final RepresentacaoProcessualPolicyService representacaoProcessualPolicyService;

    public MarketplaceRepresentacaoResolver(RepresentacaoProcessualPolicyService representacaoProcessualPolicyService) {
        this.representacaoProcessualPolicyService = Objects.requireNonNull(representacaoProcessualPolicyService);
    }

    public InstrumentoRepresentacaoProcessual resolve(RamoDireito ramo, RitoProcessual rito, String tribunal, String perfilAtorRaw) {
        TipoUsuario perfil = TipoUsuario.fromString(perfilAtorRaw);
        var policy = representacaoProcessualPolicyService.resolve(
                ramo == null ? null : ramo.name(),
                rito == null ? null : rito.name(),
                tribunal,
                perfil,
                null, null, null, false, false, null, null);
        if (!policy.regularidadeSuficiente()) {
            return null;
        }
        return InstrumentoRepresentacaoProcessual.fromString(policy.resolvedInstrument());
    }
}
```

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `./mvnw test -pl pjb-api -Dtest=MarketplaceRepresentacaoResolverTest`
Expected: 4/4 verde.

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceRepresentacaoResolver.java pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceRepresentacaoResolverTest.java
git commit -m "feat(marketplace): resolve InstrumentoRepresentacaoProcessual sem depender de Usuario"
```

---

## Task 6: Campo `perfilAtor` + retrofit do jus postulandi em `protocolar()`

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceProtocoloRequest.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/ApiMarketplaceService.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/surface/MarketplaceSurfaceFacadeService.java`
- Modify: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServiceCompletudeDocumentalUnitTest.java`
- Modify: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServiceCompletudeDocumentalTest.java`
- Modify: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServicePoloMaterializacaoTest.java`

**Interfaces:**
- Consumes: `MarketplaceRepresentacaoResolver.resolve(...)` (Task 5).
- Produces: `Processo.instrumentoRepresentacaoResolvido` populado ao final de `protocolar()` — lido por
  `MarketplaceDocumentoComplementarService` (Task 10) sem precisar re-resolver.

- [ ] **Step 1: Escrever os testes que provam a dispensa de PROCURACAO (falham primeiro)**

Adicionar em `ApiMarketplaceServiceCompletudeDocumentalUnitTest.java`:

```java
    @Test
    void clienteComPerfilAtorCidadaoEmRitoJuizadoDispensaProcuracao() {
        List<Attachment> semProcuracao = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        var request = baseRequestComPerfil(semProcuracao, "CIDADAO");
        var result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isTrue();
        assertThat(result.documentosFaltantes()).isEmpty();
    }

    @Test
    void clienteSemPerfilAtorContinuaExigindoProcuracao() {
        List<Attachment> semProcuracao = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        var request = baseRequestComPerfil(semProcuracao, null);
        var result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes()).containsExactly(TipoDocumento.PROCURACAO.name());
    }

    private ApiMarketplaceService.MarketplaceProtocoloRequest baseRequestComPerfil(List<Attachment> documentos, String perfilAtor) {
        return new ApiMarketplaceService.MarketplaceProtocoloRequest(
                "ref-0009999-30.2026.8.06.0001", "0009999-30.2026.8.06.0001", "ESTADUAL", "CIVIL", "CE", "Fortaleza",
                "ACAO_DE_COBRANCA", "Cobranca via marketplace", "Condenacao ao pagamento", null,
                "Cliente Marketplace Ltda", "12345678000199", "Fornecedor Reu Ltda", "98765432000188",
                BigDecimal.valueOf(5000), null, null, null, null, false, documentos, perfilAtor);
    }
```

Ajustar `service` no `@BeforeEach` para injetar o resolver:

```java
        service = new ApiMarketplaceService(ajuizamentoService, governanceService, new CompletudeDocumentalPolicyService(),
                new MarketplaceRepresentacaoResolver(new com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService()));
```

Import novo: `import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;`
e `import com.tcc.pjb.backend.service.api.MarketplaceRepresentacaoResolver;` (mesmo pacote, mas
explicitar não faz mal).

- [ ] **Step 2: Rodar e confirmar que falha (campo/construtor ainda não existem)**

Run: `./mvnw test-compile -pl pjb-api`
Expected: erro de compilação — `MarketplaceProtocoloRequest` não tem 21º parâmetro, construtor de
`ApiMarketplaceService` não aceita 4 argumentos.

- [ ] **Step 3: Adicionar `perfilAtor` nos dois records**

`model/dto/processo/marketplace/MarketplaceProtocoloRequest.java` — adicionar como último campo:

```java
        List<Attachment> documentos,
        String perfilAtor
) {
}
```

`ApiMarketplaceService.MarketplaceProtocoloRequest` (record aninhado) — mesmo, adicionar
`String perfilAtor` como último campo do record.

- [ ] **Step 4: Religar `protocolar()` para resolver o instrumento e usar a sobrecarga de 3 argumentos**

Em `ApiMarketplaceService.java`, adicionar o campo/construtor:

```java
    private final MarketplaceRepresentacaoResolver representacaoResolver;

    public ApiMarketplaceService(AjuizamentoService ajuizamentoService,
                                 MarketplaceGovernanceService governanceService,
                                 CompletudeDocumentalPolicyService completudeDocumentalPolicyService,
                                 MarketplaceRepresentacaoResolver representacaoResolver) {
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.completudeDocumentalPolicyService = Objects.requireNonNull(completudeDocumentalPolicyService);
        this.representacaoResolver = Objects.requireNonNull(representacaoResolver);
    }
```

Substituir, dentro de `protocolar()`:

```java
        var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), request.documentos());
```

por:

```java
        var instrumento = representacaoResolver.resolve(processo.getRamoDireito(), processo.getRito(),
                processo.getTribunal(), request.perfilAtor());
        processo.setInstrumentoRepresentacaoResolvido(instrumento == null ? null : instrumento.name());
        var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), request.documentos(), instrumento);
```

- [ ] **Step 5: Propagar o campo no facade**

Em `MarketplaceSurfaceFacadeService.protocolar(...)`, adicionar `request.perfilAtor()` como último
argumento posicional na construção do record aninhado (linha 74, após `request.documentos()`).

- [ ] **Step 6: Atualizar os 3 call sites de teste que quebraram**

Em `ApiMarketplaceServiceCompletudeDocumentalUnitTest.java` (método `baseRequest`, já existente),
`ApiMarketplaceServiceCompletudeDocumentalTest.java` (método `baseRequest`) e
`ApiMarketplaceServicePoloMaterializacaoTest.java` (método `baseRequest`): adicionar `null` como
último argumento posicional em cada `new ApiMarketplaceService.MarketplaceProtocoloRequest(...)`
existente (mantém o comportamento atual, sem `perfilAtor`).

Nos dois arquivos de IT (`@Tag("integration")`), o `@Autowired ApiMarketplaceService service` continua
funcionando sem mudança — o Spring injeta o `MarketplaceRepresentacaoResolver` real do contexto.

- [ ] **Step 7: Rodar tudo que foi tocado**

Run: `./mvnw test -pl pjb-api -Dtest=ApiMarketplaceServiceCompletudeDocumentalUnitTest,MarketplaceRepresentacaoResolverTest`
Expected: verde (5 + 4 = 9 testes).

Run: `./mvnw verify -pl pjb-api -Dit.test=ApiMarketplaceServiceCompletudeDocumentalTest,ApiMarketplaceServicePoloMaterializacaoTest -DfailIfNoTests=true -Dsurefire.skip=true`
Expected: verde (3 + 4 = 7 testes), confirma que o retrofit não quebrou os dois ITs existentes.

- [ ] **Step 8: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceProtocoloRequest.java pjb-api/src/main/java/com/tcc/pjb/backend/service/api/ApiMarketplaceService.java pjb-api/src/main/java/com/tcc/pjb/backend/service/api/surface/MarketplaceSurfaceFacadeService.java pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServiceCompletudeDocumentalUnitTest.java pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServiceCompletudeDocumentalTest.java pjb-api/src/test/java/com/tcc/pjb/backend/service/api/ApiMarketplaceServicePoloMaterializacaoTest.java
git commit -m "fix(marketplace): protocolar() dispensa PROCURACAO para jus postulandi via perfilAtor"
```

---

## Task 7: Evento `PROCESSO_DOCUMENTACAO_COMPLETADA`

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceGovernanceService.java`
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceGovernanceServiceDocumentacaoCompletadaTest.java`

**Interfaces:**
- Produces: `publicarEventoDocumentacaoCompletada(String clientId, Long processoId, String numeroProcesso, String protocoloMarketplace)` → `List<WebhookDeliveryView>`. Usado por `MarketplaceDocumentoComplementarService` (Task 10).

- [ ] **Step 1: Escrever o teste (Mockito nos repositórios, sem Spring context)**

```java
package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientApp;
import com.tcc.pjb.backend.model.entity.api.MarketplaceClientSubscription;
import com.tcc.pjb.backend.model.entity.api.MarketplaceIntegrationPlan;
import com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookEndpoint;
import com.tcc.pjb.backend.model.repository.MarketplaceClientAppRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceClientSubscriptionRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceIntegrationPlanRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookDeliveryRepository;
import com.tcc.pjb.backend.model.repository.MarketplaceWebhookEndpointRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketplaceGovernanceServiceDocumentacaoCompletadaTest {

    private MarketplaceClientSubscriptionRepository subscriptionRepository;
    private MarketplaceWebhookEndpointRepository webhookEndpointRepository;
    private MarketplaceWebhookDeliveryRepository deliveryRepository;
    private MarketplaceGovernanceService service;

    @BeforeEach
    void setUp() {
        subscriptionRepository = mock(MarketplaceClientSubscriptionRepository.class);
        webhookEndpointRepository = mock(MarketplaceWebhookEndpointRepository.class);
        deliveryRepository = mock(MarketplaceWebhookDeliveryRepository.class);
        MarketplaceIntegrationPlan plan = new MarketplaceIntegrationPlan();
        MarketplaceClientSubscription subscription = new MarketplaceClientSubscription();
        subscription.setPlan(plan);
        when(subscriptionRepository.findFirstByClientApp_ClientIdIgnoreCaseAndStatusOrderByStartedAtDesc("client-teste", "ATIVA"))
                .thenReturn(java.util.Optional.of(subscription));
        MarketplaceClientApp client = new MarketplaceClientApp();
        MarketplaceWebhookEndpoint endpoint = new MarketplaceWebhookEndpoint();
        endpoint.setClientApp(client);
        endpoint.setCallbackUrl("https://integrador.exemplo/webhook");
        endpoint.setEventFilter("*");
        endpoint.setStatus("ATIVO");
        when(webhookEndpointRepository.findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc("client-teste", "ATIVO"))
                .thenReturn(List.of(endpoint));
        when(deliveryRepository.save(any())).thenAnswer(inv -> {
            var d = (com.tcc.pjb.backend.model.entity.api.MarketplaceWebhookDelivery) inv.getArgument(0);
            d.setId(1L);
            d.setCreatedAt(Instant.now());
            return d;
        });
        service = new MarketplaceGovernanceService(
                mock(MarketplaceClientAppRepository.class),
                mock(MarketplaceIntegrationPlanRepository.class),
                subscriptionRepository,
                webhookEndpointRepository,
                deliveryRepository,
                new ObjectMapper(),
                mock(CryptoVaultService.class));
    }

    @Test
    void publicaEventoDocumentacaoCompletadaParaEndpointsAtivos() {
        var entregas = service.publicarEventoDocumentacaoCompletada("client-teste", 1L, "0001-1.2026", "client-teste:ref");

        assertThat(entregas).hasSize(1);
        assertThat(entregas.get(0).eventType()).isEqualTo("PROCESSO_DOCUMENTACAO_COMPLETADA");
    }
}
```

- [ ] **Step 2: Rodar e confirmar que falha (método ainda não existe)**

Run: `./mvnw test-compile -pl pjb-api`
Expected: erro de compilação.

- [ ] **Step 3: Implementar, espelhando `publicarEventoPendenciaDocumental`**

Adicionar em `MarketplaceGovernanceService.java`, logo após `publicarEventoPendenciaDocumental`:

```java
    @Transactional
    public List<WebhookDeliveryView> publicarEventoDocumentacaoCompletada(String clientId, Long processoId, String numeroProcesso,
                                                                            String protocoloMarketplace) {
        requireSubscription(clientId);
        List<MarketplaceWebhookEndpoint> endpoints = webhookEndpointRepository.findByClientApp_ClientIdIgnoreCaseAndStatusIgnoreCaseOrderByCreatedAtDesc(clientId, "ATIVO");
        if (endpoints.isEmpty()) {
            return List.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "PROCESSO_DOCUMENTACAO_COMPLETADA");
        payload.put("clientId", clientId);
        payload.put("processoId", processoId);
        payload.put("numeroProcesso", numeroProcesso);
        payload.put("protocoloMarketplace", protocoloMarketplace);
        payload.put("occurredAt", Instant.now().toString());
        String serialized = writeJson(payload);
        String payloadHash = Hashes.sha256HexBytes(serialized.getBytes(StandardCharsets.UTF_8));
        return endpoints.stream()
                .filter(endpoint -> acceptsEvent(endpoint.getEventFilter(), "PROCESSO_DOCUMENTACAO_COMPLETADA"))
                .map(endpoint -> {
                    MarketplaceWebhookDelivery delivery = new MarketplaceWebhookDelivery();
                    delivery.setEndpoint(endpoint);
                    delivery.setEventType("PROCESSO_DOCUMENTACAO_COMPLETADA");
                    delivery.setPayloadHash(payloadHash);
                    delivery.setStatus("PENDENTE");
                    delivery.setPayloadJson(serialized);
                    delivery.setResponseCode(null);
                    delivery.setAttempts(0);
                    delivery.setNextRetryAt(Instant.now());
                    delivery.setResponseExcerpt("Queued for outbound delivery by marketplace dispatcher.");
                    MarketplaceWebhookDelivery saved = deliveryRepository.save(delivery);
                    return new WebhookDeliveryView(saved.getId(), endpoint.getId(), endpoint.getCallbackUrl(), saved.getEventType(),
                            saved.getStatus(), saved.getResponseCode(), saved.getAttempts(), saved.getCreatedAt(), saved.getNextRetryAt(), saved.getDeliveredAt());
                })
                .toList();
    }
```

- [ ] **Step 4: Rodar e confirmar que passa**

Run: `./mvnw test -pl pjb-api -Dtest=MarketplaceGovernanceServiceDocumentacaoCompletadaTest`
Expected: 1/1 verde.

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceGovernanceService.java pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceGovernanceServiceDocumentacaoCompletadaTest.java
git commit -m "feat(marketplace): evento PROCESSO_DOCUMENTACAO_COMPLETADA"
```

---

## Task 8: DTOs do endpoint de complementação

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceComplementoDocumentalRequest.java`
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceComplementoDocumentalResponse.java`

**Interfaces:**
- Produces: os dois records, consumidos pelo controller (Task 11) e pelo facade (Task 11).

- [ ] **Step 1: Criar os records**

```java
package com.tcc.pjb.backend.model.dto.processo.marketplace;

import com.tcc.pjb.backend.model.dto.Attachment;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MarketplaceComplementoDocumentalRequest(
        @NotEmpty List<Attachment> documentos
) {
}
```

```java
package com.tcc.pjb.backend.model.dto.processo.marketplace;

import java.time.LocalDateTime;
import java.util.List;

public record MarketplaceComplementoDocumentalResponse(
        Long processoId,
        String numeroProcesso,
        String status,
        boolean documentacaoCompleta,
        List<String> documentosFaltantes,
        List<String> documentosRecebidos,
        LocalDateTime recebidoEm
) {
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: compila limpo (arquivos novos, sem consumidor ainda).

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceComplementoDocumentalRequest.java pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/processo/marketplace/MarketplaceComplementoDocumentalResponse.java
git commit -m "feat(marketplace): DTOs do endpoint de complementacao documental"
```

---

## Task 9: Exceção de estado (reaproveita `RecursoJaExistenteException`)

Nenhum arquivo novo — `RecursoJaExistenteException` (`service/exception/RecursoJaExistenteException.java`)
já existe, já é `@ResponseStatus(HttpStatus.CONFLICT)` e já tem handler dedicado em
`ApiExceptionHandler` (linha 197-200) que devolve `ProblemDetail` com a mensagem literal da exceção.
`MarketplaceDocumentoComplementarService` (Task 10) lança
`new RecursoJaExistenteException("Este processo já está com a documentação completa — nenhuma ação necessária.")`
quando o estado não é `PENDENTE_DOCUMENTACAO`. Sem task própria — documentado aqui para o executor não
recriar uma exceção que já existe.

---

## Task 10: `MarketplaceDocumentoComplementarService` — núcleo da Fase 2

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarService.java`

**Interfaces:**
- Consumes: `ObjectStoragePort.put(...)` (existente), `DocumentContentValidator` (Task 2),
  `DocumentoSigiloClassifier.classify(...)` (existente), `CompletudeDocumentalPolicyService.diagnosticar(rito, Set, instrumento)` (Task 3), `MarketplaceGovernanceService.publicarEventoDocumentacaoCompletada`
  (Task 7) e `publicarEventoPendenciaDocumental` (existente), `DocumentoProcessualRepository.findByProcessoId`/`existsByProcessoIdAndSha256`/`save` (existentes), `AuditLedgerService.appendSafely` (existente),
  `Processo.instrumentoRepresentacaoResolvido` (Task 4).
- Produces: `complementar(Long processoId, List<Attachment> documentos, String clientId)` →
  `MarketplaceComplementoDocumentalResponse`. Consumido pelo facade (Task 11).

Nota de transação: a gravação em `ObjectStoragePort` é I/O de arquivo local (não rede), então entra
no mesmo `@Transactional` do método — igual ao padrão já usado em `PastaDigitalService.anexarDocumentoPdf`,
que já persiste no mesmo padrão. Não é o `@Transactional` pesado que o `ADR-0007`/CLAUDE.md proíbe
(esse é sobre I/O de rede/HTTP externo dentro de transação de banco, não sobre filesystem local).

- [ ] **Step 1: Escrever o serviço completo**

```java
package com.tcc.pjb.backend.service.api;

import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceDocumentoComplementarService {

    private static final String STATUS_PENDENTE_DOCUMENTACAO = "PENDENTE_DOCUMENTACAO";
    private static final String STATUS_RECEBIDO = "RECEBIDO_MARKETPLACE";

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final ObjectStoragePort objectStorage;
    private final DocumentContentValidator contentValidator;
    private final DocumentoSigiloClassifier sigiloClassifier;
    private final CompletudeDocumentalPolicyService completudeDocumentalPolicyService;
    private final MarketplaceGovernanceService governanceService;
    private final AuditLedgerService auditLedger;

    public MarketplaceDocumentoComplementarService(ProcessoRepository processoRepository,
                                                   DocumentoProcessualRepository documentoRepository,
                                                   ObjectStoragePort objectStorage,
                                                   DocumentContentValidator contentValidator,
                                                   DocumentoSigiloClassifier sigiloClassifier,
                                                   CompletudeDocumentalPolicyService completudeDocumentalPolicyService,
                                                   MarketplaceGovernanceService governanceService,
                                                   AuditLedgerService auditLedger) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.objectStorage = Objects.requireNonNull(objectStorage);
        this.contentValidator = Objects.requireNonNull(contentValidator);
        this.sigiloClassifier = Objects.requireNonNull(sigiloClassifier);
        this.completudeDocumentalPolicyService = Objects.requireNonNull(completudeDocumentalPolicyService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @Transactional
    public MarketplaceComplementoDocumentalResponse complementar(Long processoId, List<Attachment> documentos, String clientId) {
        Processo processo = processoRepository.findById(processoId)
                .filter(p -> p.getConnectorProtocolReference() != null
                        && p.getConnectorProtocolReference().startsWith(clientId + ":"))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo não encontrado para este cliente."));

        if (!STATUS_PENDENTE_DOCUMENTACAO.equals(processo.getConnectorSubmissionStatus())) {
            throw new RecursoJaExistenteException("Este processo já está com a documentação completa — nenhuma ação necessária.");
        }

        List<String> documentosRecebidos = new ArrayList<>();
        for (Attachment attachment : documentos) {
            if (attachment.getTipoDocumento() == null) {
                throw new ErroDeValidacaoException(TipoErroValidacao.CAMPO_OBRIGATORIO, "tipoDocumento")
                        .addMetadado("motivo", "tipoDocumento obrigatório para cada documento enviado");
            }
            byte[] bytes = attachment.getContent();
            String nome = attachment.getName();
            contentValidator.validarTamanho(bytes == null ? 0 : bytes.length, nome);
            contentValidator.validarExtensaoOuContentType(nome, attachment.getContentType());

            String sha256 = Hashes.sha256HexBytes(bytes);
            if (documentoRepository.existsByProcessoIdAndSha256(processoId, sha256)) {
                continue;
            }

            try {
                contentValidator.validarEstruturaPdf(bytes, nome);
            } catch (IOException e) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                        .addMetadado("erro_tecnico", e.getClass().getSimpleName());
            }

            var cls = sigiloClassifier.classify(nome, null);
            NivelSigilo procSigilo = processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
            NivelSigilo sigiloDoc = maxSigilo(procSigilo, cls.minSigilo());

            String key = "marketplace/" + processoId + "/" + UUID.randomUUID();
            try {
                objectStorage.put(key, new ByteArrayInputStream(bytes), bytes.length,
                        attachment.getContentType(), java.util.Map.of());
            } catch (IOException e) {
                throw new ErroDeValidacaoException(TipoErroValidacao.ARQUIVO_CORROMPIDO, nome, e)
                        .addMetadado("erro_tecnico", "falha ao gravar no armazenamento de objetos");
            }

            DocumentoProcessual doc = DocumentoProcessual.builder()
                    .processo(processo)
                    .nomeOriginal(nome)
                    .titulo(nome)
                    .contentType(attachment.getContentType())
                    .tamanhoBytes((long) bytes.length)
                    .sha256(sha256)
                    .storageBackend("LOCALFS")
                    .storageUri(key)
                    .tipoDocumento(attachment.getTipoDocumento())
                    .categoria(cls.suggestedCategoria())
                    .nivelSigilo(sigiloDoc)
                    .origemSistema("MARKETPLACE_API")
                    .criadoEm(LocalDateTime.now())
                    .build();
            documentoRepository.save(doc);
            documentosRecebidos.add(attachment.getTipoDocumento().name());
        }

        Set<TipoDocumento> tiposPresentes = EnumSet.noneOf(TipoDocumento.class);
        for (DocumentoProcessual d : documentoRepository.findByProcessoId(processoId)) {
            if (d.getTipoDocumento() != null) {
                tiposPresentes.add(d.getTipoDocumento());
            }
        }

        InstrumentoRepresentacaoProcessual instrumento =
                InstrumentoRepresentacaoProcessual.fromString(processo.getInstrumentoRepresentacaoResolvido());
        var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), tiposPresentes, instrumento);
        List<String> documentosFaltantes = diagnostico.faltantes().stream().map(Enum::name).toList();
        boolean documentacaoCompleta = !diagnostico.bloqueante();

        if (documentacaoCompleta) {
            processo.setConnectorSubmissionStatus(STATUS_RECEBIDO);
            processo.setConnectorSubmissionMessage("Documentação completada via marketplace em " + LocalDateTime.now() + ".");
            governanceService.publicarEventoDocumentacaoCompletada(clientId, processo.getId(), processo.getNumeroProcesso(),
                    processo.getConnectorProtocolReference());
        } else {
            processo.setConnectorSubmissionMessage(
                    "Protocolo recebido via marketplace, pendente de documentacao obrigatoria: " + documentosFaltantes);
            governanceService.publicarEventoPendenciaDocumental(clientId, processo.getId(), processo.getNumeroProcesso(),
                    processo.getConnectorProtocolReference(), documentosFaltantes);
        }
        processoRepository.save(processo);

        auditLedger.appendSafely("MARKETPLACE_DOCUMENTOS_COMPLEMENTADOS", "PROCESSO", String.valueOf(processoId),
                "cliente=" + clientId + " recebidos=" + documentosRecebidos.size() + " completo=" + documentacaoCompleta);

        return new MarketplaceComplementoDocumentalResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getConnectorSubmissionStatus(),
                documentacaoCompleta,
                documentosFaltantes,
                documentosRecebidos,
                LocalDateTime.now());
    }

    private static NivelSigilo maxSigilo(NivelSigilo a, NivelSigilo b) {
        NivelSigilo x = a == null ? NivelSigilo.PUBLICO : a;
        NivelSigilo y = b == null ? NivelSigilo.PUBLICO : b;
        return x.getNivel() >= y.getNivel() ? x : y;
    }
}
```

- [ ] **Step 2: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: compila limpo. Se `Hashes.sha256HexBytes` não aceitar `byte[]` diretamente (confirmar
assinatura em `core/util/Hashes.java` antes de rodar — já usado com `byte[]` em
`MarketplaceGovernanceService.publicarEventoProtocolo`), ajustar a chamada conforme a assinatura real.

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarService.java
git commit -m "feat(marketplace): MarketplaceDocumentoComplementarService — nucleo da Fase 2"
```

---

## Task 11: Facade + Controller

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/api/surface/MarketplaceSurfaceFacadeService.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processo/ApiMarketplaceController.java`

**Interfaces:**
- Consumes: `MarketplaceDocumentoComplementarService.complementar(...)` (Task 10).

- [ ] **Step 1: Facade**

Em `MarketplaceSurfaceFacadeService.java`: adicionar campo/construtor para
`MarketplaceDocumentoComplementarService` e o método:

```java
    public MarketplaceComplementoDocumentalResponse complementarDocumentos(Long processoId,
                                                                            MarketplaceComplementoDocumentalRequest request,
                                                                            String clientId) {
        return documentoComplementarService.complementar(processoId, request.documentos(), clientId);
    }
```

(imports: `com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalRequest`,
`...MarketplaceComplementoDocumentalResponse`, `com.tcc.pjb.backend.service.api.MarketplaceDocumentoComplementarService`)

- [ ] **Step 2: Controller**

Em `ApiMarketplaceController.java`, adicionar:

```java
    @PostMapping("/processos/{id}/documentos")
    public ResponseEntity<MarketplaceComplementoDocumentalResponse> complementarDocumentos(
            @PathVariable Long id,
            @Valid @RequestBody MarketplaceComplementoDocumentalRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "marketplace_complementar_documentos", ApiVersion.V1);
        String clientId = authentication != null && authentication.getName() != null ? authentication.getName() : null;
        if (clientId == null || clientId.isBlank()) {
            clientId = marketplaceOAuth2Service.authorizeHttpRequest(servletRequest, "processos:documentos").clientId();
        }
        return ResponseEntity.ok(facadeService.complementarDocumentos(id, request, clientId));
    }
```

(imports: `com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalRequest`,
`...MarketplaceComplementoDocumentalResponse`, `org.springframework.web.bind.annotation.PathVariable`)

- [ ] **Step 3: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: compila limpo.

- [ ] **Step 4: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/api/surface/MarketplaceSurfaceFacadeService.java pjb-api/src/main/java/com/tcc/pjb/backend/controller/processo/ApiMarketplaceController.java
git commit -m "feat(marketplace): endpoint POST /api/marketplace/v1/processos/{id}/documentos"
```

---

## Task 12: Testes unitários do fluxo completo (Mockito)

**Files:**
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarServiceTest.java`

- [ ] **Step 1: Escrever os casos — posse negada, estado não-pendente, tipo ausente, duplicata, parcial, completo**

```java
package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.sigilo.DocumentoSigiloClassifier;
import com.tcc.pjb.backend.core.storage.ObjectStoragePort;
import com.tcc.pjb.backend.core.storage.ObjectWriteResult;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.document.DocumentContentValidator;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.RecursoJaExistenteException;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MarketplaceDocumentoComplementarServiceTest {

    private ProcessoRepository processoRepository;
    private DocumentoProcessualRepository documentoRepository;
    private ObjectStoragePort objectStorage;
    private MarketplaceGovernanceService governanceService;
    private AuditLedgerService auditLedger;
    private MarketplaceDocumentoComplementarService service;

    @BeforeEach
    void setUp() throws Exception {
        processoRepository = mock(ProcessoRepository.class);
        documentoRepository = mock(DocumentoProcessualRepository.class);
        objectStorage = mock(ObjectStoragePort.class);
        governanceService = mock(MarketplaceGovernanceService.class);
        auditLedger = mock(AuditLedgerService.class);
        when(objectStorage.put(any(), any(), org.mockito.ArgumentMatchers.anyLong(), any(), any()))
                .thenReturn(new ObjectWriteResult("k", URI.create("http://x/objects/k"), 10L, "sha256x", "sha384x"));
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new MarketplaceDocumentoComplementarService(processoRepository, documentoRepository, objectStorage,
                new DocumentContentValidator(), new DocumentoSigiloClassifier(), new CompletudeDocumentalPolicyService(),
                governanceService, auditLedger);
    }

    @Test
    void processoDeOutroClienteRetorna404() {
        Processo processo = Processo.builder().id(1L).connectorProtocolReference("outro-client:ref").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        assertThatThrownBy(() -> service.complementar(1L, List.of(), "client-teste"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void processoInexistenteRetorna404() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complementar(99L, List.of(), "client-teste"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void processoJaCompletoRetorna409() {
        Processo processo = Processo.builder().id(1L).connectorProtocolReference("client-teste:ref")
                .connectorSubmissionStatus("RECEBIDO_MARKETPLACE").build();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));

        assertThatThrownBy(() -> service.complementar(1L, List.of(), "client-teste"))
                .isInstanceOf(RecursoJaExistenteException.class)
                .hasMessageContaining("já está com a documentação completa");
    }

    @Test
    void anexoSemTipoDocumentoRetorna400() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        Attachment semTipo = Attachment.builder().name("x.pdf").content(pdf(1)).contentType("application/pdf").build();

        assertThatThrownBy(() -> service.complementar(1L, List.of(semTipo), "client-teste"))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void duplicataPorSha256EhIgnoradaSemGravarDeNovo() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(documentoRepository.existsByProcessoIdAndSha256(eq(1L), any())).thenReturn(true);
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of());
        Attachment repetido = attachment(TipoDocumento.PETICAO_INICIAL);

        var resp = service.complementar(1L, List.of(repetido), "client-teste");

        assertThat(resp.documentosRecebidos()).isEmpty();
        verify(documentoRepository, never()).save(any());
    }

    @Test
    void complementoParcialMantemPendenteERedispatchaEvento() {
        Processo processo = pendente();
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        DocumentoProcessual jaSalvo = DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PETICAO_INICIAL).build();
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of(jaSalvo));

        var resp = service.complementar(1L, List.of(attachment(TipoDocumento.PROCURACAO)), "client-teste");

        assertThat(resp.documentacaoCompleta()).isFalse();
        assertThat(resp.status()).isEqualTo("PENDENTE_DOCUMENTACAO");
        verify(governanceService, times(1)).publicarEventoPendenciaDocumental(any(), any(), any(), any(), any());
        verify(governanceService, never()).publicarEventoDocumentacaoCompletada(any(), any(), any(), any());
    }

    @Test
    void complementoTotalMudaStatusEDisparaEventoDeConclusao() {
        Processo processo = pendente();
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        DocumentoProcessual peticao = DocumentoProcessual.builder().tipoDocumento(TipoDocumento.PETICAO_INICIAL).build();
        when(documentoRepository.findByProcessoId(1L)).thenReturn(List.of(peticao));

        var resp = service.complementar(1L, List.of(attachment(TipoDocumento.PROCURACAO)), "client-teste");

        assertThat(resp.documentacaoCompleta()).isTrue();
        assertThat(resp.status()).isEqualTo("RECEBIDO_MARKETPLACE");
        verify(governanceService, times(1)).publicarEventoDocumentacaoCompletada(any(), any(), any(), any());
    }

    private Processo pendente() {
        return Processo.builder().id(1L).numeroProcesso("0001-1.2026")
                .connectorProtocolReference("client-teste:ref").connectorSubmissionStatus("PENDENTE_DOCUMENTACAO")
                .rito(RitoProcessual.COMUM_ORDINARIO).build();
    }

    private Attachment attachment(TipoDocumento tipo) {
        return Attachment.builder().name(tipo.name().toLowerCase() + ".pdf").tipoDocumento(tipo)
                .content(pdf(1)).contentType("application/pdf").build();
    }

    private static byte[] pdf(int paginas) {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> T eq(T value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
```

- [ ] **Step 2: Rodar**

Run: `./mvnw test -pl pjb-api -Dtest=MarketplaceDocumentoComplementarServiceTest`
Expected: 7/7 verde. Se `Processo.builder()...build()` não aceitar `connectorSubmissionStatus`/
`connectorProtocolReference` diretamente no builder (checar se são campos do `@Builder` da classe —
já confirmado que são campos simples com `@Getter @Setter` de classe, então o `@Builder` do Lombok os
inclui automaticamente), ajustar para `Processo p = Processo.builder()...build(); p.setX(...)`.

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarServiceTest.java
git commit -m "test(marketplace): cobre MarketplaceDocumentoComplementarService (posse, estado, duplicata, parcial, completo)"
```

---

## Task 13: IT com Testcontainers — armazenamento real de ponta a ponta

**Files:**
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarServiceIT.java`

- [ ] **Step 1: Escrever o IT**

```java
package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.document.DocumentContentService;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false"
})
class MarketplaceDocumentoComplementarServiceIT extends PjbIntegrationTestBase {

    @Autowired
    private MarketplaceDocumentoComplementarService service;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoRepository;

    @Autowired
    private DocumentContentService documentContentService;

    @MockitoBean
    private MarketplaceGovernanceService governanceService;

    @Test
    void complementaDocumentoGravaConteudoRealNoObjectStorageELeDeVolta() throws Exception {
        Processo processo = Processo.builder()
                .numeroUnificado("0009999-40.2026.8.06.0001")
                .numeroProcesso("0009999-40.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .connectorSystem("MARKETPLACE_API")
                .connectorProtocolReference("client-it:ref-40")
                .connectorSubmissionStatus("PENDENTE_DOCUMENTACAO")
                .dataCriacao(LocalDateTime.now())
                .build();
        processo = processoRepository.save(processo);

        Attachment anexo = Attachment.builder()
                .name("peticao.pdf")
                .contentType("application/pdf")
                .tipoDocumento(TipoDocumento.PETICAO_INICIAL)
                .content(pdf(1))
                .build();

        var resp = service.complementar(processo.getId(), List.of(anexo), "client-it");

        assertThat(resp.documentosRecebidos()).contains(TipoDocumento.PETICAO_INICIAL.name());

        var salvos = documentoRepository.findByProcessoId(processo.getId());
        assertThat(salvos).hasSize(1);
        assertThat(salvos.get(0).getTipoDocumento()).isEqualTo(TipoDocumento.PETICAO_INICIAL);
        assertThat(salvos.get(0).getStorageBackend()).isEqualTo("LOCALFS");
        assertThat(salvos.get(0).getStorageUri()).isNotBlank();

        var lido = documentContentService.resolvePdf(salvos.get(0));
        assertThat(lido.contentLength()).isGreaterThan(0);
    }

    private static byte[] pdf(int paginas) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < paginas; i++) {
                doc.addPage(new PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
```

- [ ] **Step 2: Rodar via Failsafe**

Run: `./mvnw verify -pl pjb-api -Dit.test=MarketplaceDocumentoComplementarServiceIT -DfailIfNoTests=true -Dsurefire.skip=true`
Expected: 1/1 verde.

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/test/java/com/tcc/pjb/backend/service/api/MarketplaceDocumentoComplementarServiceIT.java
git commit -m "test(marketplace): IT prova armazenamento real via ObjectStoragePort de ponta a ponta"
```

---

## Task 14: Fechar a dívida — `DEBT_LOG.md` e `README.md`

**Files:**
- Modify: `docs/quality/DEBT_LOG.md`
- Modify: `README.md`

- [ ] **Step 1: Atualizar `D-marketplace-sem-completude-documental`**

Trocar `**Status:** Fase 1 aplicada — ...` para `**Status:** Fase 1 e Fase 2 fechadas.` e acrescentar
um parágrafo descrevendo a Fase 2 (endpoint, storage real, evento `PROCESSO_DOCUMENTACAO_COMPLETADA`)
e o retrofit do jus postulandi na Fase 1, seguindo o mesmo estilo narrativo das entradas fechadas já
existentes no arquivo (ver `D-mni-litisconsorcio-primeira-pessoa` como referência de formato).

- [ ] **Step 2: Atualizar `README.md`**

Acrescentar parágrafo na mesma seção de fatias recentes, com a contagem final de testes somada
(contar os testes novos: 7 `PastaDigitalServiceTest` + 2 `CompletudeDocumentalPolicyServiceTest` + 4
`MarketplaceRepresentacaoResolverTest` + 2 `ApiMarketplaceServiceCompletudeDocumentalUnitTest` + 1
`MarketplaceGovernanceServiceDocumentacaoCompletadaTest` + 7
`MarketplaceDocumentoComplementarServiceTest` = 23 unit novos; 1
`MarketplaceDocumentoComplementarServiceIT` novo = 1 IT novo) sobre a base de 4.382 unit / 253 IT
deixada pela fatia MNI desta mesma sessão.

- [ ] **Step 3: Rodar a suíte completa tocada nesta fatia antes do commit final**

Run: `./mvnw test -pl pjb-api -Dtest=PastaDigitalServiceTest,CompletudeDocumentalPolicyServiceTest,MarketplaceRepresentacaoResolverTest,ApiMarketplaceServiceCompletudeDocumentalUnitTest,MarketplaceGovernanceServiceDocumentacaoCompletadaTest,MarketplaceDocumentoComplementarServiceTest`
Expected: todos verdes, 0 falhas.

Run: `./mvnw verify -pl pjb-api -Dit.test=ApiMarketplaceServiceCompletudeDocumentalTest,ApiMarketplaceServicePoloMaterializacaoTest,MarketplaceDocumentoComplementarServiceIT -DfailIfNoTests=true -Dsurefire.skip=true`
Expected: todos verdes, 0 falhas.

- [ ] **Step 4: Commit final**

```bash
git add docs/quality/DEBT_LOG.md README.md
git commit -m "docs: fecha D-marketplace-sem-completude-documental Fase 2"
```

---

## Self-review desta plan (feito pelo autor do plano, não repetir)

- **Cobertura do spec:** retrofit jus postulandi → Task 6; endpoint novo → Tasks 8-11; storage real →
  Task 10; validação de conteúdo → Task 2; sigilo → Task 10 (reaproveita `DocumentoSigiloClassifier`
  direto, sem extração porque já é stateless); auditoria → Task 10; rate limiting → Task 11; migration
  → Task 4; PastaDigitalServiceTest ausente → Task 1. Todas as seções do spec têm task correspondente.
- **Placeholders:** nenhum "TBD"/"implementar depois" — todo step tem código completo.
- **Consistência de tipos:** `DocumentContentValidator.ValidatedPdf`, `MarketplaceRepresentacaoResolver.resolve`,
  `MarketplaceDocumentoComplementarService.complementar` usam os mesmos nomes em toda task que os
  referencia.
- **Desvio do spec registrado:** o spec dizia "`ApiMarketplaceService` ganha o método
  `complementarDocumentos`" — a plan usa um serviço novo (`MarketplaceDocumentoComplementarService`)
  para não inflar as dependências de `ApiMarketplaceService` com 6 componentes que `protocolar()` não
  usa. Mesma funcionalidade, melhor fronteira de responsabilidade.
