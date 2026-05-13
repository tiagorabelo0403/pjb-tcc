package com.tcc.pjb.backend.core.preflight;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateIssue;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DocumentSpec;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ProceduralPreflightEngine {

    public sealed interface Severity permits Severity.BLOQUEANTE, Severity.CRITICO, Severity.AVISO, Severity.INFO {
        record BLOQUEANTE() implements Severity {}
        record CRITICO()    implements Severity {}
        record AVISO()      implements Severity {}
        record INFO()       implements Severity {}

        static Severity fromString(String s) {
            return switch (s == null ? "" : s.toUpperCase(Locale.ROOT)) {
                case "BLOQUEANTE" -> new BLOQUEANTE();
                case "CRITICO"   -> new CRITICO();
                case "AVISO"     -> new AVISO();
                default          -> new INFO();
            };
        }
        default boolean isBlocking() { return this instanceof BLOQUEANTE || this instanceof CRITICO; }
        default String label() {
            return switch (this) {
                case BLOQUEANTE b -> "BLOQUEANTE";
                case CRITICO c    -> "CRITICO";
                case AVISO a      -> "AVISO";
                case INFO i       -> "INFO";
            };
        }
    }

    
    public record PreflightIssue(
            String code,
            Severity severity,
            String message,
            String field,
            String suggestedFix
    ) {
        public boolean blocks() { return severity.isBlocking(); }
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("code", code == null ? "UNSPECIFIED" : code);
            out.put("severity", severity == null ? new Severity.INFO().label() : severity.label());
            out.put("message", message == null ? "Inconsistência procedural identificada." : message);
            out.put("field", field == null ? "" : field);
            out.put("suggestedFix", suggestedFix == null ? "" : suggestedFix);
            out.put("blocking", blocks());
            return Collections.unmodifiableMap(out);
        }
    }

    
    public record PreflightResult(
            String requestId,
            Instant evaluatedAt,
            boolean readyForSubmission,
            String resolvedRito,
            String resolvedClasseTpu,
            String resolvedTribunalCodigo,
            String resolvedConnector,
            List<String> requiredParties,
            List<String> missingParties,
            List<String> requiredDocuments,
            List<String> missingDocuments,
            List<PreflightIssue> issues,
            Map<String, Object> metadata
    ) {
        public boolean hasBlockers() {
            return issues.stream().anyMatch(PreflightIssue::blocks);
        }
        public List<PreflightIssue> blockers() {
            return issues.stream().filter(PreflightIssue::blocks).toList();
        }
        public List<PreflightIssue> warnings() {
            return issues.stream().filter(i -> !i.blocks()).toList();
        }
    }

    
    public record PreflightContext(
            String ritoRaw,
            String classeTpuRaw,
            String ramoDireitoRaw,
            String tribunalCodigoRaw,
            String uf,
            String comarca,
            String vara,
            BigDecimal valorCausa,
            List<String> partesPresentes,
            List<String> documentosPresentes,
            List<String> formatosArquivo,
            boolean assinadoDigitalmente,
            boolean possuiProcuracao,
            boolean recurso,
            boolean tutela,
            boolean segredoJustica,
            Map<String, Object> extras
    ) {
        public PreflightContext {
            partesPresentes = partesPresentes == null ? List.of() : List.copyOf(partesPresentes);
            documentosPresentes = documentosPresentes == null ? List.of() : List.copyOf(documentosPresentes);
            formatosArquivo = formatosArquivo == null ? List.of() : List.copyOf(formatosArquivo);
            extras = safeCopy(extras);
        }

        public static PreflightContext fromMap(Map<String, Object> m) {
            Objects.requireNonNull(m, "context map");
            List<String> partes = firstNonEmptyList(m,
                    "partesPresentes", "partes_presentes", "partes", "polos", "poloAtivo", "poloPassivo",
                    "requiredPartyRoles", "missingPartyRoles");
            List<String> documentos = firstNonEmptyList(m,
                    "documentosPresentes", "documentos_presentes", "documentos", "anexos", "requiredDocuments", "missingDocuments");
            List<String> formatos = firstNonEmptyList(m,
                    "formatosArquivo", "formatos_arquivo", "formatos", "mimeTypes", "mime_types");
            return new PreflightContext(
                    firstText(m, "rito", "rito_processual", "ritoProcessual", "procedimento"),
                    firstText(m, "classeTpu", "classe_tpu", "classeProcessual", "classe", "tipoAcao", "classeTpuCodigo", "classeTpuNome"),
                    firstText(m, "ramoDireito", "ramo_direito", "materia", "ramo"),
                    firstText(m, "tribunalCodigo", "tribunal_codigo", "tribunal", "siglaTribunal"),
                    firstText(m, "uf", "ufAutor", "ufReu", "estado"),
                    firstText(m, "comarca", "comarcaAutor", "comarcaReu", "municipio", "cidade"),
                    firstText(m, "vara", "varaDestino", "orgaoJulgador", "zonaEleitoral", "auditoriaMilitar"),
                    firstBigDecimal(m, "valorCausa", "valor_causa", "valor"),
                    partes,
                    documentos,
                    formatos,
                    firstBool(m, "assinadoDigitalmente", "assinaturaDigital", "signed", "assinado"),
                    firstBool(m, "possuiProcuracao", "temProcuracao", "haProcuracao") || documentos.stream().anyMatch(v -> normalizeTokenStatic(v).contains("PROCURACAO")),
                    firstBool(m, "recurso", "faseRecursal"),
                    firstBool(m, "tutela", "temTutela", "pedidoTutela"),
                    firstBool(m, "segredoJustica", "segredo_justica", "sigilo"),
                    safeCopy(m)
            );
        }
    }

    private static final BigDecimal LIMITE_JEC_ESTADUAL   = new BigDecimal("40000.00");
    private static final BigDecimal LIMITE_JEC_FEDERAL    = new BigDecimal("66000.00");
    private static final BigDecimal LIMITE_JEC_FAZENDA    = new BigDecimal("60000.00");
    private static final BigDecimal LIMITE_TURMA_RECURSAL = new BigDecimal("66000.00");

    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;

    public ProceduralPreflightEngine(ProceduralCanonicalResolver proceduralCanonicalResolver,
                                     CanonicalSanityGate canonicalSanityGate) {
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
    }

    public PreflightResult evaluate(PreflightContext ctx) {
        Objects.requireNonNull(ctx, "PreflightContext");

        String requestId = UUID.randomUUID().toString();
        List<PreflightIssue> issues = new ArrayList<>(32);

        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(buildCanonicalPayload(ctx));
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
        appendGateIssues(gateResult, issues);
        if ("CANONICAL_CONTEXT_INCOMPLETE".equals(gateResult.overallStatus())
                && issues.stream().noneMatch(issue -> "GATE_CANONICAL_CONTEXT_INCOMPLETE".equals(issue.code()))) {
            issues.add(new PreflightIssue(
                    "GATE_CANONICAL_CONTEXT_INCOMPLETE",
                    new Severity.BLOQUEANTE(),
                    "Contexto canônico insuficiente para protocolo automático.",
                    "rito/classe/tribunal",
                    "Informar rito, classe TPU e tribunal competente antes da submissão."
            ));
        }

        String rito = canonicalContext.rito() != null ? canonicalContext.rito().name() : resolveRito(ctx, issues);
        Optional<TpuClasseCnj> classeOpt = resolveClasseTpu(ctx, canonicalContext, rito, issues);
        TpuClasseCnj classe = classeOpt.orElse(null);

        Optional<NationalCompetenceMatrix> tribunalOpt = resolveTribunal(ctx, canonicalContext, rito, classe, issues);
        NationalCompetenceMatrix tribunal = tribunalOpt.orElse(null);
        String classeResolvida = classe != null ? classe.name() : null;
        String tribunalResolvido = tribunal != null ? tribunal.codigo() : null;

        JudicialSystem connector = verifyConnector(tribunal, gateResult, issues);

        DefinitionSnapshot snapshot = ProceduralCatalogSupport.snapshot(ProceduralRitoNames.parseOrDefault(rito, "COMUM_ORDINARIO"));
        List<String> requiredParties = snapshot.parties().stream()
                .filter(PartyRoleSpec::required).map(PartyRoleSpec::code).toList();
        List<String> missingParties = checkParties(ctx, snapshot.parties(), issues);

        List<String> requiredDocs = snapshot.documents().stream()
                .filter(DocumentSpec::required).map(DocumentSpec::code).toList();
        List<String> missingDocs = checkDocuments(ctx, snapshot.documents(), issues);

        checkValorCausa(ctx, rito, classe, issues);

        checkAssinatura(ctx, rito, classe, issues);

        checkPrazoFatal(ctx, rito, classe, issues);

        checkFormatos(ctx, tribunal, issues);

        checkStepUp(ctx, rito, classe, tribunal, issues);

        checkProcuracao(ctx, rito, issues);

        if (ProceduralRitoNames.isEleitoral(rito)) {
            checkEleitoralContexto(ctx, rito, classe, issues);
        }

        if (ProceduralRitoNames.isMilitar(rito)) {
            checkMilitarContexto(ctx, rito, classe, issues);
        }

        checkWorkflowCompleteness(rito, snapshot, issues);

        boolean pronto = issues.stream().noneMatch(PreflightIssue::blocks);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ritoResolvido", rito);
        metadata.put("classeTpuResolvida", classeResolvida != null ? classeResolvida : "NAO_RESOLVIDA");
        metadata.put("tribunalResolvido", tribunalResolvido != null ? tribunalResolvido : "NAO_RESOLVIDO");
        metadata.put("canonicalContext", canonicalContext.toMap());
        metadata.put("sanityGate", gateResult.toMap());
        metadata.put("sanityStatus", gateResult.overallStatus());
        metadata.put("totalIssues", issues.size());
        metadata.put("blockers", issues.stream().filter(PreflightIssue::blocks).count());
        metadata.put("warnings", issues.stream().filter(i -> !i.blocks()).count());

        return new PreflightResult(
                requestId,
                Instant.now(),
                pronto,
                rito,
                classeResolvida,
                tribunalResolvido,
                connector != null ? connector.name() : null,
                requiredParties,
                missingParties,
                requiredDocs,
                missingDocs,
                List.copyOf(issues),
                Collections.unmodifiableMap(metadata)
        );
    }

    private String resolveRito(PreflightContext ctx, List<PreflightIssue> issues) {
        String raw = coalesce(ctx.ritoRaw(), ctx.classeTpuRaw(), ctx.ramoDireitoRaw());
        if (raw == null || raw.isBlank()) {
            issues.add(issue("RITO_AUSENTE", new Severity.BLOQUEANTE(),
                    "Rito processual não identificado. Informe o rito ou classe TPU para prosseguir.",
                    "rito", "Forneça o campo 'rito' ou 'classeTpu' com o procedimento pretendido."));
            return "COMUM_ORDINARIO";
        }
        String resolved = ProceduralRitoNames.resolveName(ctx.ritoRaw(), ctx.ramoDireitoRaw(), ctx.classeTpuRaw());
        if (resolved.isBlank()) {
            issues.add(issue("RITO_NAO_RECONHECIDO", new Severity.CRITICO(),
                    "Rito '" + raw + "' não reconhecido no catálogo nacional. " +
                    "Verifique a classe TPU ou o nome do procedimento.",
                    "rito", "Consulte /catalog/classes para a lista completa de ritos suportados."));
            return "COMUM_ORDINARIO";
        }
        return resolved;
    }

    private Optional<TpuClasseCnj> resolveClasseTpu(PreflightContext ctx,
                                                      CanonicalContext canonicalContext,
                                                      String rito,
                                                      List<PreflightIssue> issues) {
        String canonicalClasseKey = coalesce(canonicalContext != null ? canonicalContext.classeTpuCodigo() : null,
                canonicalContext != null ? canonicalContext.classeTpuNome() : null);
        if (canonicalClasseKey != null) {
            Optional<TpuClasseCnj> byCanonical = resolveClasseByNameOrCode(canonicalClasseKey);
            if (byCanonical.isPresent()) return byCanonical;
        }
        if (ctx.classeTpuRaw() != null && !ctx.classeTpuRaw().isBlank()) {
            Optional<TpuClasseCnj> byName = TpuClasseCnj.porNome(ctx.classeTpuRaw());
            if (byName.isPresent()) return byName;
            try {
                int codigo = Integer.parseInt(ctx.classeTpuRaw().trim());
                Optional<TpuClasseCnj> byCodigo = TpuClasseCnj.porCodigo(codigo);
                if (byCodigo.isPresent()) return byCodigo;
            } catch (NumberFormatException ignored) {}
        }
        Optional<TpuClasseCnj> byRito = TpuClasseCnj.porNome(rito);
        if (byRito.isPresent()) return byRito;
        issues.add(issue("CLASSE_TPU_NAO_RESOLVIDA", new Severity.AVISO(),
                "Classe TPU do CNJ não resolvida. O sistema usará o rito inferido como referência, " +
                "mas o tribunal pode exigir a classe TPU oficial na submissão.",
                "classeTpu", "Informe o código numérico TPU (ex: 1000 para IPM) ou o nome oficial da classe."));
        return Optional.empty();
    }

    private Optional<NationalCompetenceMatrix> resolveTribunal(PreflightContext ctx,
                                                                 CanonicalContext canonicalContext,
                                                                 String rito,
                                                                 TpuClasseCnj classe,
                                                                 List<PreflightIssue> issues) {
        String canonicalTribunal = canonicalContext != null ? canonicalContext.tribunalCodigo() : null;
        if (canonicalTribunal != null && !canonicalTribunal.isBlank()) {
            Optional<NationalCompetenceMatrix> byCanonical = NationalCompetenceMatrix.porCodigo(canonicalTribunal);
            if (byCanonical.isPresent()) return byCanonical;
        }
        if (ctx.tribunalCodigoRaw() != null && !ctx.tribunalCodigoRaw().isBlank()) {
            Optional<NationalCompetenceMatrix> found = NationalCompetenceMatrix.porCodigo(ctx.tribunalCodigoRaw());
            if (found.isPresent()) return found;
            issues.add(issue("TRIBUNAL_CODIGO_INVALIDO", new Severity.CRITICO(),
                    "Código de tribunal '" + ctx.tribunalCodigoRaw() + "' não encontrado na matriz nacional.",
                    "tribunalCodigo", "Consulte /catalog/tribunals para a lista de tribunais cadastrados."));
        }
        if (ctx.uf() != null && !ctx.uf().isBlank()) {
            NationalCompetenceMatrix.RamoJusticaNacional ramoJustica = inferirRamoJustica(rito);
            Optional<NationalCompetenceMatrix> byUfRamo = NationalCompetenceMatrix.resolver(ctx.uf(), ramoJustica);
            if (byUfRamo.isPresent()) return byUfRamo;
            issues.add(issue("TRIBUNAL_NAO_INFERIDO", new Severity.AVISO(),
                    "Não foi possível inferir o tribunal a partir de UF='" + ctx.uf() +
                    "' e ramo='" + ramoJustica.name() + "'. " +
                    "Múltiplos tribunais cobrem essa UF ou o ramo não mapeou diretamente.",
                    "tribunalCodigo", "Informe o código do tribunal explicitamente (ex: 'TJSP', 'TRT2', 'TRE-SP')."));
        }
        if (ctx.uf() == null || ctx.uf().isBlank()) {
            issues.add(issue("UF_AUSENTE", new Severity.BLOQUEANTE(),
                    "UF não informada. Sem a UF não é possível determinar o tribunal competente.",
                    "uf", "Informe a UF de competência (ex: 'SP', 'MG', 'RJ')."));
        }
        return Optional.empty();
    }

    private JudicialSystem verifyConnector(NationalCompetenceMatrix tribunal,
                                            GateResult gateResult,
                                            List<PreflightIssue> issues) {
        if (tribunal == null) return null;
        if (!tribunal.supportsProtocolo()) {
            issues.add(issue("CONECTOR_SEM_PROTOCOLO", new Severity.CRITICO(),
                    "O tribunal '" + tribunal.codigo() + "' não possui integração de protocolo eletrônico " +
                    "homologada nesta versão do sistema.",
                    "tribunalCodigo",
                    "Homologue capability válida na matriz nacional antes de liberar protocolo automático."));
            return null;
        }
        if (gateResult != null && gateResult.hasBlockingIssues()) {
            issues.add(issue("SANITY_GATE_BLOQUEANTE", new Severity.CRITICO(),
                    "Gate canônico detectou inconsistências estruturais bloqueantes: " + String.join(", ", gateResult.statusCodes()) + '.',
                    "sanityGate",
                    "Corrija o contexto canônico, a competência e o blueprint antes de protocolar."));
        }
        return tribunal.connectorPreferido();
    }

    private List<String> checkParties(PreflightContext ctx,
                                       List<PartyRoleSpec> schema,
                                       List<PreflightIssue> issues) {
        List<String> missing = new ArrayList<>();
        java.util.Set<String> presenteNorm = ctx.partesPresentes().stream()
                .map(this::normalizeToken)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());

        for (PartyRoleSpec role : schema) {
            if (!role.required()) continue;
            boolean found = java.util.stream.Stream.concat(
                            java.util.stream.Stream.of(role.code()),
                            role.aliases().stream())
                    .filter(token -> token != null && !token.isBlank())
                    .map(this::normalizeToken)
                    .anyMatch(presenteNorm::contains);
            if (!found) {
                missing.add(role.code());
                String humanRole = humanize(role.code());
                issues.add(issue("PARTE_OBRIGATORIA_AUSENTE_" + role.code(),
                        new Severity.BLOQUEANTE(),
                        "Parte obrigatória ausente: '" + humanRole + "'. " +
                        "Esta peça não será aceita sem a qualificação correta desta parte no polo respectivo.",
                        "partes",
                        "Inclua a seção '" + humanRole + "' com qualificação completa (nome, CPF/CNPJ, endereço, " +
                        "advogado responsável). Sinônimos aceitos: " + String.join(", ", role.aliases()) + "."));
            }
        }
        return List.copyOf(missing);
    }

    private List<String> checkDocuments(PreflightContext ctx,
                                         List<DocumentSpec> schema,
                                         List<PreflightIssue> issues) {
        List<String> missing = new ArrayList<>();
        List<String> presenteNorm = ctx.documentosPresentes().stream()
                .map(this::normalizeToken).toList();

        for (DocumentSpec doc : schema) {
            if (!doc.required()) continue;
            if (!presenteNorm.contains(normalizeToken(doc.code()))) {
                missing.add(doc.code());
                issues.add(issue("DOCUMENTO_OBRIGATORIO_AUSENTE_" + doc.code(),
                        new Severity.BLOQUEANTE(),
                        "Documento obrigatório ausente: '" + humanize(doc.code()) + "'. " +
                        "Motivo: " + doc.rationale() + ".",
                        "documentos",
                        "Anexe o documento '" + humanize(doc.code()) + "' antes de prosseguir. " +
                        "Formatos aceitos: PDF/A, PDF asssinado digitalmente."));
            }
        }
        return List.copyOf(missing);
    }

    private void checkValorCausa(PreflightContext ctx,
                                  String rito,
                                  TpuClasseCnj classe,
                                  List<PreflightIssue> issues) {
        if (ctx.valorCausa() == null || ctx.valorCausa().compareTo(BigDecimal.ZERO) <= 0) {
            if (!ProceduralRitoNames.isMilitar(rito) && !ProceduralRitoNames.isEleitoral(rito)) {
                issues.add(issue("VALOR_CAUSA_AUSENTE", new Severity.AVISO(),
                        "Valor da causa não informado ou zero. " +
                        "O sistema não pode verificar compatibilidade com a alçada do juizado ou tribunal.",
                        "valorCausa", "Informe o valor atribuído à causa conforme os critérios do art. 291 CPC."));
            }
            return;
        }
        switch (rito) {
                case "JUIZADO_ESPECIAL_CIVEL" -> {
                    if (ctx.valorCausa().compareTo(LIMITE_JEC_ESTADUAL) > 0) {
                        issues.add(issue("VALOR_ACIMA_JEC_ESTADUAL", new Severity.CRITICO(),
                                "Valor da causa R$ " + ctx.valorCausa() + " supera o limite do JEC estadual (40 SM). " +
                                "A ação não pode tramitar pelo rito sumaríssimo nessa alçada.",
                                "valorCausa", "Ajuste o rito para 'Procedimento Comum' ou revise o valor da causa."));
                    }
                }
                case "JUIZADO_ESPECIAL_FEDERAL" -> {
                    if (ctx.valorCausa().compareTo(LIMITE_JEC_FEDERAL) > 0) {
                        issues.add(issue("VALOR_ACIMA_JEF", new Severity.CRITICO(),
                                "Valor da causa R$ " + ctx.valorCausa() + " supera o limite do JEF (60 SM). " +
                                "Redistribua para vara federal comum.",
                                "valorCausa", "Ajuste o rito para vara federal comum ou revise o valor."));
                    }
                }
                case "JUIZADO_ESPECIAL_FAZENDA_PUBLICA" -> {
                    if (ctx.valorCausa().compareTo(LIMITE_JEC_FAZENDA) > 0) {
                        issues.add(issue("VALOR_ACIMA_JEFAZ", new Severity.CRITICO(),
                                "Valor da causa R$ " + ctx.valorCausa() + " supera o limite do JEFaz (60 SM). " +
                                "Redistribua para vara da Fazenda Pública comum.",
                                "valorCausa", "Ajuste o rito ou revise o valor."));
                    }
                }
                default -> {
                    if (ctx.valorCausa().compareTo(BigDecimal.ZERO) == 0) {
                        issues.add(issue("VALOR_CAUSA_ZERO", new Severity.AVISO(),
                                "Valor da causa igual a zero. Se a ação tiver conteúdo econômico, " +
                                "o tribunal pode exigir a atribuição de valor estimado.",
                                "valorCausa", "Atribua valor estimado à causa nos termos do art. 292 CPC."));
                    }
                }
            }
        }

    private void checkAssinatura(PreflightContext ctx,
                                  String rito,
                                  TpuClasseCnj classe,
                                  List<PreflightIssue> issues) {
        boolean exigeAssinatura = (
                ProceduralRitoNames.isMilitar(rito)
                || ProceduralRitoNames.isEleitoral(rito)
                || (classe != null && classe.ramoJustica() == TpuClasseCnj.RamoJustica.SUPERIOR)
        );
        if (exigeAssinatura && !ctx.assinadoDigitalmente()) {
            issues.add(issue("ASSINATURA_DIGITAL_AUSENTE", new Severity.BLOQUEANTE(),
                    "A peça não está assinada digitalmente. Para ritos " +
                    (ProceduralRitoNames.isMilitar(rito) ? "militares" : ProceduralRitoNames.isEleitoral(rito) ? "eleitorais" : "em Tribunais Superiores") +
                    ", a assinatura ICP-Brasil é obrigatória (PAdES ou XAdES conforme o conector).",
                    "assinatura", "Assine a peça com certificado ICP-Brasil válido antes de submeter."));
        }
        if (!ctx.possuiProcuracao() && !ctx.assinadoDigitalmente()) {
            issues.add(issue("PROCURACAO_OU_ASSINATURA_AUSENTES", new Severity.CRITICO(),
                    "Nenhuma procuração e nenhuma assinatura digital detectadas. " +
                    "O sistema não pode verificar a representação processual.",
                    "procuracao", "Annexe a procuração ou assine a peça digitalmente com certificado vigente."));
        }
    }

    private void checkPrazoFatal(PreflightContext ctx,
                                  String rito,
                                  TpuClasseCnj classe,
                                  List<PreflightIssue> issues) {
        if (rito == null) return;
        switch (rito) {
            case "ELEITORAL_AIRC" -> issues.add(issue("PRAZO_FATAL_AIRC", new Severity.INFO(),
                    "AIRC tem prazo fatal de 3 dias após a publicação da candidatura. " +
                    "Verifique se a submissão está dentro do prazo legal.",
                    "prazo", "Confirme a data da publicação do pedido de registro antes de protocolar."));
            case "ELEITORAL_AIME" -> issues.add(issue("PRAZO_FATAL_AIME", new Severity.INFO(),
                    "AIME deve ser proposta no prazo de 15 dias após a diplomação. " +
                    "Verifique o calendário eleitoral.",
                    "prazo", "Confirme a data da diplomação e calcule o prazo decadencial."));
            case "ELEITORAL_AIJE", "ELEITORAL_CAPTACAO_ILICITA_SUFRAGIO" ->
                    issues.add(issue("PRAZO_FATAL_AIJE", new Severity.INFO(),
                    "AIJE e captação ilícita de sufrágio têm prazo de propositura vinculado ao calendário eleitoral. " +
                    "Fatos anteriores à eleição prescrevem em 1 ano após o pleito.",
                    "prazo", "Verifique a data dos fatos imputados e a data do pleito."));
            case "ELEITORAL_REPRESENTACAO_INELEGIBILIDADE" ->
                    issues.add(issue("PRAZO_INELEGIBILIDADE_LCP64", new Severity.INFO(),
                    "Ações de inelegibilidade devem ser propostas nos prazos da LC 64/90. " +
                    "Verifique o prazo para cada causa de inelegibilidade aplicável.",
                    "prazo", "Consulte os prazos dos arts. 1º a 3º da LC 64/90 conforme o fundamento."));
            case "ESPECIAL_HABEAS_CORPUS", "PENAL_HABEAS_CORPUS_PREVENTIVO", "MILITAR_HABEAS_CORPUS_MILITAR" ->
                    issues.add(issue("HC_URGENCIA", new Severity.INFO(),
                    "Habeas corpus é remédio de urgência. Verifique se há risco de prescrição da coação " +
                    "ou se já cessou o constrangimento antes de protocolar.",
                    "prazo", "Avalie se a coação persiste e se o HC ainda é o instrumento adequado."));
            default -> {}
        }
    }

    private void checkFormatos(PreflightContext ctx,
                                NationalCompetenceMatrix tribunal,
                                List<PreflightIssue> issues) {
        List<String> formatos = ctx.formatosArquivo();
        if (formatos.isEmpty()) {
            issues.add(issue("FORMATO_ARQUIVO_NAO_INFORMADO", new Severity.AVISO(),
                    "Formatos dos arquivos a anexar não foram informados. " +
                    "Alguns tribunais aceitam apenas PDF/A ou exigem tamanho máximo por peça.",
                    "anexos", "Informe os formatos e confirme que atendem às políticas do tribunal destino."));
            return;
        }
        boolean temNaoPdf = formatos.stream()
                .anyMatch(f -> f != null && !f.toLowerCase(Locale.ROOT).contains("pdf"));
        if (temNaoPdf) {
            String tribunalCodigo = tribunal != null ? tribunal.codigo() : "NAO_RESOLVIDO";
            issues.add(issue("FORMATO_NAO_PDF_DETECTADO", new Severity.AVISO(),
                    "Alguns anexos não estão em formato PDF. O tribunal '" + tribunalCodigo +
                    "' pode rejeitar formatos diferentes de PDF/A ou PDF assinado.",
                    "anexos", "Converta todos os documentos para PDF antes de submeter."));
        }
    }

    private void checkStepUp(PreflightContext ctx,
                              String rito,
                              TpuClasseCnj classe,
                              NationalCompetenceMatrix tribunal,
                              List<PreflightIssue> issues) {
        if (tribunal == null) return;
        boolean exigeStepUp = (tribunal.isMilitar() || tribunal.isEleitoral())
                && !ctx.assinadoDigitalmente();
        if (exigeStepUp) {
            issues.add(issue("STEP_UP_REQUERIDO", new Severity.CRITICO(),
                    "O tribunal '" + tribunal.codigo() +
                    "' exige autenticação reforçada (gov.br nível Prata/Ouro ou certificado ICP-Brasil) " +
                    "para atos processuais eletrônicos.",
                    "autenticacao", "Complete a autenticação step-up antes de submeter a peça."));
        }
    }

    private void checkProcuracao(PreflightContext ctx,
                                  String rito,
                                  List<PreflightIssue> issues) {
        if (!ctx.possuiProcuracao()) {
            boolean exige = !ProceduralRitoNames.isMilitar(rito);
            if (exige) {
                issues.add(issue("PROCURACAO_AUSENTE", new Severity.CRITICO(),
                        "Procuração não detectada no bundle. O sistema não pode confirmar a representação processual. " +
                        "A maioria dos tribunais rejeita a petição sem o instrumento de procuração vigente.",
                        "procuracao", "Anexe a procuração com poderes adequados para o ato processual pretendido. " +
                        "Verifique substabelecimentos e poderes especiais exigidos pela classe."));
            }
        }
    }

    private void checkEleitoralContexto(PreflightContext ctx,
                                         String rito,
                                         TpuClasseCnj classe,
                                         List<PreflightIssue> issues) {
        List<String> docsNorm = ctx.documentosPresentes().stream().map(this::normalizeToken).toList();
        switch (rito) {
            case "ELEITORAL_REGISTRO_CANDIDATURA" -> {
                checkDocObrigatorio(docsNorm, "DRAP", "DRAP (Demonstrativo de Regularidade do Partido ou Federação)",
                        "Documento exigido para registro de candidatura no sistema eleitoral.", issues);
                checkDocObrigatorio(docsNorm, "ATA_CONVENCAO", "Ata de convenção partidária",
                        "Deliberação sobre a escolha do candidato na convenção.", issues);
                checkDocObrigatorio(docsNorm, "FILIACAO_PARTIDARIA", "Comprovante de filiação partidária",
                        "Condição de elegibilidade: filiação no prazo legal.", issues);
            }
            case "ELEITORAL_PROPAGANDA", "ELEITORAL_DIREITO_RESPOSTA" -> {
                checkDocObrigatorio(docsNorm, "MIDIA_PROPAGANDA", "Mídia da propaganda impugnada",
                        "Prova pré-constituída da propaganda irregular.", issues);
                checkDocObrigatorio(docsNorm, "ATA_NOTARIAL_OU_HASH", "Ata notarial ou hash da prova digital",
                        "Integridade e autenticidade da prova eletrônica.", issues);
            }
            case "ELEITORAL_PRESTACAO_CONTAS" -> {
                checkDocObrigatorio(docsNorm, "EXTRATOS_BANCARIOS", "Extratos bancários da campanha",
                        "Comprovação da movimentação financeira eleitoral.", issues);
                checkDocObrigatorio(docsNorm, "RECIBOS_ELEITORAIS", "Recibos eleitorais",
                        "Documentação financeira da campanha conforme lei eleitoral.", issues);
            }
            default -> {}
        }
    }

    private void checkMilitarContexto(PreflightContext ctx,
                                       String rito,
                                       TpuClasseCnj classe,
                                       List<PreflightIssue> issues) {
        List<String> docsNorm = ctx.documentosPresentes().stream().map(this::normalizeToken).toList();
        switch (rito) {
            case "MILITAR_IPM" -> {
                checkDocObrigatorio(docsNorm, "PORTARIA_INSTAURACAO", "Portaria de instauração do IPM",
                        "Formalização do IPM conforme CPPM art. 9º.", issues);
                checkDocObrigatorio(docsNorm, "DESIGNACAO_AUTORIDADE", "Designação da autoridade instauradora",
                        "Competência da autoridade que instaurou o IPM.", issues);
            }
            case "MILITAR_PROCESSO_PENAL_MILITAR", "MILITAR", "MILITAR_CONSELHO_JUSTICA" -> {
                checkDocObrigatorio(docsNorm, "PECAS_IPM", "Peças do IPM",
                        "Elementos informativos do procedimento militar.", issues);
            }
            case "MILITAR_PAD" -> {
                checkDocObrigatorio(docsNorm, "PORTARIA_INSTAURACAO", "Portaria de instauração do PAD",
                        "Ato formal de instauração do processo disciplinar.", issues);
                checkDocObrigatorio(docsNorm, "DESIGNACAO_COMISSAO", "Designação da comissão disciplinar",
                        "Comissão competente para o PAD militar.", issues);
                checkDocObrigatorio(docsNorm, "ASSENTAMENTOS_FUNCIONAIS", "Assentamentos funcionais do militar",
                        "Dados funcionais obrigatórios em PADs militares.", issues);
            }
            default -> {}
        }
    }

    private void checkWorkflowCompleteness(String rito,
                                            DefinitionSnapshot snapshot,
                                            List<PreflightIssue> issues) {
        boolean hasExternalActor = snapshot.parties().stream().anyMatch(PartyRoleSpec::external);
        boolean hasExternalWorkItem = snapshot.stages().stream()
                .flatMap(s -> s.getWork() == null ? java.util.stream.Stream.empty() : s.getWork().stream())
                .anyMatch(w -> {
                    String role = w.getActorRole();
                    return role != null && (
                            role.contains("ADVOGADO") || role.contains("DEFENSOR") ||
                            role.contains("PROCURADOR") || role.contains("MINISTERIO_PUBLICO"));
                });
        if (hasExternalActor && !hasExternalWorkItem) {
            issues.add(issue("WORKFLOW_SEM_ATOR_EXTERNO", new Severity.AVISO(),
                    "O rito '" + rito + "' tem partes externas, " +
                    "mas nenhum work item está atribuído a ADVOGADO, DEFENSOR ou MP. " +
                    "O advogado não receberá tarefas no painel e o processo ficará bloqueado.",
                    "workflow", "Verifique o rito_pack_2026.json e o ProceduralCatalogSupport para " +
                    "adicionar work items com actorRole=ADVOGADO nas fases de protocolo e resposta."));
        }
        if (snapshot.stages().isEmpty()) {
            issues.add(issue("WORKFLOW_BPMN_VAZIO", new Severity.CRITICO(),
                    "Nenhuma fase/stage definida para o rito '" + rito + "'. " +
                    "O processo será criado mas não terá fluxo: nenhuma tarefa será gerada automaticamente.",
                    "workflow", "Defina os stages no ProceduralCatalogSupport ou no rito_pack_2026.json."));
        }
    }

    private void checkDocObrigatorio(List<String> presentes, String code, String nome,
                                      String motivo, List<PreflightIssue> issues) {
        if (!presentes.contains(normalizeToken(code))) {
            issues.add(issue("DOC_ESPECIFICO_AUSENTE_" + normalizeToken(code),
                    new Severity.BLOQUEANTE(),
                    "Documento específico obrigatório ausente: '" + nome + "'. Motivo: " + motivo,
                    "documentos",
                    "Anexe '" + nome + "' antes de protocolar. Formato esperado: PDF."));
        }
    }

    private NationalCompetenceMatrix.RamoJusticaNacional inferirRamoJustica(String rito) {
        if (ProceduralRitoNames.isEleitoral(rito)) return NationalCompetenceMatrix.RamoJusticaNacional.ELEITORAL;
        if (ProceduralRitoNames.isMilitar(rito))   return NationalCompetenceMatrix.RamoJusticaNacional.MILITAR_SUPERIOR;
        if (ProceduralRitoNames.isTrabalhista(rito)) return NationalCompetenceMatrix.RamoJusticaNacional.TRABALHO;
        String name = rito;
        if (name.startsWith("PREVIDENCIARIO") || name.startsWith("ESPECIAL_HABEAS") ||
            name.contains("FEDERAL") || name.contains("EXECUCAO_FISCAL")) {
            return NationalCompetenceMatrix.RamoJusticaNacional.FEDERAL;
        }
        return NationalCompetenceMatrix.RamoJusticaNacional.ESTADUAL;
    }

    private Map<String, Object> buildCanonicalPayload(PreflightContext ctx) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        putCanonicalPayload(payload, "rito", ctx.ritoRaw());
        putCanonicalPayload(payload, "classeTpu", ctx.classeTpuRaw());
        putCanonicalPayload(payload, "ramoDireito", ctx.ramoDireitoRaw());
        putCanonicalPayload(payload, "tribunalCodigo", ctx.tribunalCodigoRaw());
        putCanonicalPayload(payload, "uf", ctx.uf());
        putCanonicalPayload(payload, "comarca", ctx.comarca());
        putCanonicalPayload(payload, "vara", ctx.vara());
        if (ctx.partesPresentes() != null && !ctx.partesPresentes().isEmpty()) {
            payload.put("requiredPartyRoles", List.copyOf(ctx.partesPresentes()));
        }
        if (ctx.documentosPresentes() != null && !ctx.documentosPresentes().isEmpty()) {
            payload.put("requiredDocuments", List.copyOf(ctx.documentosPresentes()));
        }
        return safeCopy(payload);
    }


    private static void putCanonicalPayload(Map<String, Object> payload, String key, String value) {
        if (payload == null || key == null || key.isBlank() || value == null) {
            return;
        }
        String normalized = value.trim();
        if (!normalized.isBlank()) {
            payload.put(key, normalized);
        }
    }

    private void appendGateIssues(GateResult gateResult, List<PreflightIssue> issues) {
        if (gateResult == null) {
            return;
        }
        for (GateIssue gateIssue : gateResult.issues()) {
            Severity severity = gateIssue.blocking() ? new Severity.BLOQUEANTE() : new Severity.AVISO();
            issues.add(issue(gateIssue.status().name(), severity, gateIssue.message(), gateIssue.field(),
                    "Corrija o núcleo canônico antes de prosseguir."));
        }
    }

    private Optional<TpuClasseCnj> resolveClasseByNameOrCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        Optional<TpuClasseCnj> byName = TpuClasseCnj.porNome(raw);
        if (byName.isPresent()) {
            return byName;
        }
        try {
            return TpuClasseCnj.porCodigo(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private PreflightIssue issue(String code, Severity severity, String message,
                                  String field, String fix) {
        return new PreflightIssue(code, severity, message, field, fix);
    }

    private String normalizeToken(String v) {
        if (v == null) return "";
        return Normalizer.normalize(v, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT)
                .strip();
    }

    private String humanize(String token) {
        return token == null ? "" : Arrays.stream(token.split("_"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1).toLowerCase(Locale.ROOT))
                .reduce((a, b) -> a + " " + b).orElse(token);
    }

    private String coalesce(String... values) {
        for (String v : values) { if (v != null && !v.isBlank()) return v; }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> safeCopy(Map<String, Object> input) {
        if (input == null || input.isEmpty()) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : input.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private static List<String> asList(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof List<?> l) return l.stream().filter(e -> e != null).map(Object::toString).toList();
        if (v instanceof String s && !s.isBlank()) {
            return Arrays.stream(s.split("[,;\n]"))
                    .map(String::trim)
                    .filter(token -> !token.isBlank())
                    .toList();
        }
        return List.of();
    }

    private static String asText(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : v.toString();
    }

    private static boolean asBool(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(v)) || "1".equals(String.valueOf(v)) || "sim".equalsIgnoreCase(String.valueOf(v));
    }

    private static BigDecimal asBigDecimal(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return null;
        try { return new BigDecimal(v.toString()); } catch (Exception e) { return null; }
    }

    private static String firstText(Map<String, Object> m, String... keys) {
        for (String key : keys) {
            String value = asText(m, key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static BigDecimal firstBigDecimal(Map<String, Object> m, String... keys) {
        for (String key : keys) {
            BigDecimal value = asBigDecimal(m, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static boolean firstBool(Map<String, Object> m, String... keys) {
        for (String key : keys) {
            if (m.containsKey(key) && asBool(m, key)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> firstNonEmptyList(Map<String, Object> m, String... keys) {
        for (String key : keys) {
            List<String> values = asList(m, key);
            if (!values.isEmpty()) {
                return values;
            }
        }
        return List.of();
    }

    private static String normalizeTokenStatic(String v) {
        if (v == null) return "";
        return Normalizer.normalize(v, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9_]", "_")
                .replaceAll("_+", "_")
                .toUpperCase(Locale.ROOT)
                .strip();
    }
}
