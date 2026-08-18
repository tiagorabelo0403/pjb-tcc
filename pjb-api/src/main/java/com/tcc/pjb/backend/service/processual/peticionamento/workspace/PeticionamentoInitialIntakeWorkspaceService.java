package com.tcc.pjb.backend.service.processual.peticionamento.workspace;

import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntentEngine;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoVisualIdentityRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoEditorBlueprintCatalogService;

@Service
public class PeticionamentoInitialIntakeWorkspaceService {

    private static final Pattern MONEY_PATTERN = Pattern.compile("(?i)valor\\s+da\\s+causa[^\\d]*(?:R\\$\\s*)?([\\d\\.,]+)");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(?:[IVXLCDM]+|\\d{1,3})(?:[\\).\\-]|\\s+[-–])?\\s*(.+)$");
    private static final Set<String> FACT_HEADINGS = Set.of("FATOS", "DOS FATOS", "DA EXPOSICAO DOS FATOS", "BREVE RESUMO DOS FATOS");
    private static final Set<String> LEGAL_HEADINGS = Set.of("DO DIREITO", "DOS FUNDAMENTOS JURIDICOS", "FUNDAMENTOS", "DO DIREITO APLICAVEL", "DOS FUNDAMENTOS");
    private static final Set<String> REQUEST_HEADINGS = Set.of("DOS PEDIDOS", "PEDIDOS", "REQUERIMENTOS", "DO PEDIDO", "DOS REQUERIMENTOS");
    private static final Set<String> EVIDENCE_HEADINGS = Set.of("DAS PROVAS", "PROVAS", "PROTESTA POR PROVAS", "DOS MEIOS DE PROVA");
    private static final Set<String> URGENCY_HEADINGS = Set.of("DA TUTELA DE URGENCIA", "DA LIMINAR", "DA TUTELA ANTECIPADA", "DO PEDIDO LIMINAR");

    private final AjuizamentoIntentEngine ajuizamentoIntentEngine;
    private final PeticionamentoEditorBlueprintCatalogService editorBlueprintCatalogService;

    public PeticionamentoInitialIntakeWorkspaceService(AjuizamentoIntentEngine ajuizamentoIntentEngine,
                                                       PeticionamentoEditorBlueprintCatalogService editorBlueprintCatalogService) {
        this.ajuizamentoIntentEngine = Objects.requireNonNull(ajuizamentoIntentEngine, "ajuizamentoIntentEngine");
        this.editorBlueprintCatalogService = Objects.requireNonNull(editorBlueprintCatalogService, "editorBlueprintCatalogService");
    }

    public IntakeResult analyze(PeticionamentoSessaoRequest request, Usuario usuario) {
        PeticionamentoSessaoRequest safe = request == null ? PeticionamentoSessaoRequest.builder().tituloCaso("PETIÇÃO INICIAL").build() : request;
        String petitionText = resolvePetitionText(safe);
        LinkedHashMap<String, Object> intentPayload = buildIntentPayload(safe, usuario, petitionText);
        AjuizamentoIntent intent = ajuizamentoIntentEngine.inferir(intentPayload);
        ProceduralRoutingReport routing = intent == null ? null : intent.proceduralRouting();
        SectionExtraction extraction = extractSections(petitionText);

        String resolvedTitle = firstNonBlank(safe.getTituloCaso(), extraction.title(), intent == null ? null : intent.tipoAcao(), routing == null ? null : routing.actionNature(), "PETIÇÃO INICIAL");
        String resolvedRamo = firstNonBlank(safe.getRamoDireito(), intent == null ? null : intent.ramoDireito());
        String resolvedRito = firstNonBlank(safe.getRitoProcessual(), intent == null ? null : intent.rito(), routing == null ? null : routing.ritoSugerido());
        String resolvedClasse = firstNonBlank(safe.getClasseProcessual(), routing == null ? null : routing.actionFamily(), intent == null ? null : intent.tipoAcao());
        String resolvedNatureza = firstNonBlank(safe.getNaturezaJuridica(), metadataAsString(routing, "naturezaJuridicaCanonical"), routing == null ? null : routing.actionNature(), intent == null ? null : intent.tipoAcao());
        String resolvedCidadeProtocolo = firstNonBlank(safe.getCidadeProtocolo(), routing == null ? null : routing.cidadeSugerida(), safe.getEnderecoAutor() == null ? null : safe.getEnderecoAutor().getCidade(), usuario == null ? null : usuario.getComarca());
        String resolvedUfProtocolo = firstNonBlank(safe.getUfProtocolo(), routing == null ? null : routing.ufSugerida(), safe.getEnderecoAutor() == null ? null : safe.getEnderecoAutor().getUf(), usuario == null ? null : usuario.getUf());
        String resolvedCidadeFato = firstNonBlank(safe.getCidadeFato(), extraction.cityHint(), safe.getEnderecoAutor() == null ? null : safe.getEnderecoAutor().getCidade());
        String resolvedUfFato = firstNonBlank(safe.getUfFato(), extraction.ufHint(), safe.getEnderecoAutor() == null ? null : safe.getEnderecoAutor().getUf());
        BigDecimal resolvedValorCausa = safe.getValorCausa() != null ? safe.getValorCausa() : extraction.valorCausa();

        List<String> fatos = chooseList(safe.getFatos(), extraction.facts(), fallbackNarrative(safe.getTextoFatosResumido()));
        List<String> fundamentos = chooseList(safe.getFundamentosJuridicos(), extraction.legalGrounds(), intent == null ? List.of() : intent.alertas());
        List<String> pedidos = chooseList(safe.getPedidos(), extraction.requests(), intent == null ? List.of() : intent.proximosPassos());
        List<String> provas = chooseList(safe.getProvasIndicadas(), extraction.evidence(), inferEvidenceChecklist(intent, petitionText));
        boolean tutela = safe.tutelaUrgenciaResolvida() || extraction.urgencyDetected();

        PeticionamentoVisualIdentityRequest identity = resolveIdentity(safe.getIdentidadeVisual(), usuario, resolvedUfProtocolo);
        LaianePeticaoInicialDraftService.EstruturarRequest draftRequest = new LaianePeticaoInicialDraftService.EstruturarRequest(
                safe.getProcessoId(),
                resolvedTitle,
                safe.getParteAutora(),
                safe.getParteRe(),
                resolvedRamo,
                resolvedRito,
                resolvedClasse,
                fatos,
                fundamentos,
                pedidos,
                provas,
                resolvedValorCausa,
                tutela,
                petitionText,
                resolvedCidadeFato,
                resolvedUfFato,
                resolvedCidadeProtocolo,
                resolvedUfProtocolo,
                resolvedNatureza,
                EnderecosProcessuaisRequest.vazio()
        );

        ArrayList<String> automations = new ArrayList<>();
        ArrayList<String> pendencias = new ArrayList<>();
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        if (!petitionText.isBlank()) {
            automations.add("Peça pré-existente lida e convertida em trilha de autopreenchimento assistido.");
            envelope.put("capturaPeticao", "UPLOAD_INTELIGENTE_LEITURA_ASSISTIDA");
        } else {
            automations.add("Editor nativo por blocos preparado para construção guiada da inicial.");
            envelope.put("capturaPeticao", "EDITOR_NATIVO_BLOCOS");
        }
        if (resolvedRamo != null) {
            automations.add("Ramo e procedimento preliminares inferidos para reduzir preenchimento repetitivo.");
        }
        if (resolvedCidadeProtocolo != null && resolvedUfProtocolo != null) {
            automations.add("Unidade de ingresso sugerida a partir do contexto territorial e da malha procedural, sem exigir escolha técnica do usuário.");
        }
        if (safe.getEnderecoAutor() != null && safe.getEnderecoAutor().getCep() != null && !safe.getEnderecoAutor().getCep().isBlank()) {
            automations.add("CEP do autor integrado à trilha de qualificação para apoiar rota territorial, citações e distribuição.");
        }
        if (identity.getExibirBrasaoOuLogomarca() != null && identity.getExibirBrasaoOuLogomarca() && identity.getBrasaoOuLogomarcaUri() != null && !identity.getBrasaoOuLogomarcaUri().isBlank()) {
            automations.add("Identidade visual habilitada para editor nativo com brasão ou logomarca do peticionante.");
        }

        collectMissing(pendencias, resolvedCidadeFato, "Informar a cidade em que o fato ocorreu para o PJB entender melhor onde o caso começou.");
        collectMissing(pendencias, resolvedUfFato, "Informar a UF do fato para o PJB fechar a rota territorial com mais segurança.");
        if (resolvedCidadeFato == null && resolvedCidadeProtocolo == null) {
            pendencias.add("Informar pelo menos a cidade ou município mais ligado ao conflito, ao trabalho, ao pleito, ao fato criminal ou ao atendimento negado.");
        }
        collectMissing(pendencias, resolvedNatureza, "Confirmar a natureza jurídica predominante da demanda.");
        if (resolvedValorCausa == null && ramoExigeValor(resolvedRamo)) {
            pendencias.add("Informar o valor da causa ou justificar sua inaplicabilidade para o rito escolhido.");
        }
        if (safe.getParteAutora() == null || safe.getParteAutora().isBlank()) {
            pendencias.add("Preencher o bloco da parte autora com nome completo e qualificação mínima.");
        }
        if (safe.getParteRe() == null || safe.getParteRe().isBlank()) {
            pendencias.add("Preencher o bloco da parte ré ou indicar a ausência de polo passivo definido.");
        }
        if (fatos.isEmpty()) {
            pendencias.add("Consolidar a narrativa dos fatos em bloco próprio antes do protocolo.");
        }
        if (pedidos.isEmpty()) {
            pendencias.add("Consolidar os pedidos em bloco próprio antes do protocolo.");
        }
        if (routing != null && routing.exigeRevisaoHumana()) {
            pendencias.add("O sistema ainda precisa de revisão humana para fechar a rota correta do caso antes do protocolo final.");
        }

        envelope.put("ramoInferido", resolvedRamo);
        envelope.put("ritoInferido", resolvedRito);
        envelope.put("classeInferida", resolvedClasse);
        envelope.put("naturezaJuridicaInferida", resolvedNatureza);
        envelope.put("cidadeFato", resolvedCidadeFato);
        envelope.put("ufFato", resolvedUfFato);
        envelope.put("cidadeProtocolo", resolvedCidadeProtocolo);
        envelope.put("ufProtocolo", resolvedUfProtocolo);
        envelope.put("valorCausaInferido", resolvedValorCausa == null ? null : resolvedValorCausa.toPlainString());
        envelope.put("confiancaIntent", intent == null ? null : intent.confianca());
        envelope.put("questionBlocksPending", pendencias.size());
        if (routing != null) {
            envelope.put("proceduralRouting", routing.toMap());
        }
        envelope.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);

        PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint resolvedBlueprint = editorBlueprintCatalogService.resolve(
                new PeticionamentoEditorBlueprintCatalogService.ResolveRequest(
                        resolvedRamo,
                        resolvedRito,
                        firstNonBlank(safe.getTipoJustica(), routing == null ? null : routing.tipoJusticaSugerida()),
                        safe.getClasseProcessual(),
                        safe.getAssuntoTpu(),
                        safe.getMateriaPrincipal(),
                        resolvedNatureza,
                        usuario == null ? null : usuario.getTipoUsuario(),
                        !petitionText.isBlank(),
                        tutela,
                        Boolean.TRUE.equals(safe.getContextoConsensual()),
                        identidadeParaMapa(identity)
                )
        );

        return new IntakeResult(
                draftRequest,
                buildWorkspace(safe, usuario, identity, intent, routing, extraction, resolvedNatureza, resolvedCidadeFato, resolvedUfFato, resolvedCidadeProtocolo, resolvedUfProtocolo, resolvedValorCausa, pendencias, resolvedBlueprint),
                List.copyOf(new LinkedHashSet<>(automations)),
                List.copyOf(new LinkedHashSet<>(pendencias)),
                Map.copyOf(envelope),
                resolvedBlueprint
        );
    }

    private Map<String, Object> buildWorkspace(PeticionamentoSessaoRequest request,
                                               Usuario usuario,
                                               PeticionamentoVisualIdentityRequest identity,
                                               AjuizamentoIntent intent,
                                               ProceduralRoutingReport routing,
                                               SectionExtraction extraction,
                                               String natureza,
                                               String cidadeFato,
                                               String ufFato,
                                               String cidadeProtocolo,
                                               String ufProtocolo,
                                               BigDecimal valorCausa,
                                               List<String> pendencias,
                                               PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint resolvedBlueprint) {
        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("captureModes", List.of("EDITOR_NATIVO_BLOCOS", "UPLOAD_INTELIGENTE_LEITURA_ASSISTIDA"));
        workspace.put("selectedCaptureMode", extraction.sourceMode());
        workspace.put("questionBlocks", buildQuestionBlocks(request, intent, routing, identidadeParaMapa(identity), cidadeFato, ufFato, cidadeProtocolo, ufProtocolo, valorCausa, pendencias, resolvedBlueprint));
        workspace.put("editorBlueprint", buildEditorBlueprint(identity, intent, routing, usuario, resolvedBlueprint));
        workspace.put("jurisdictionIntake", metadataAsMap(routing, "jurisdictionIntake"));
        workspace.put("assistantQuestions", jurisdictionQuestions(routing, "guidedQuestions"));
        workspace.put("assistantAmbiguityQuestions", jurisdictionQuestions(routing, "ambiguityQuestions"));
        workspace.put("assistantResolutionPolicy", jurisdictionResolutionPolicy(routing));
        workspace.put("specializedQuestionBlocks", resolvedBlueprint == null ? List.of() : resolvedBlueprint.specializedQuestionBlocks());
        workspace.put("petitionModels", resolvedBlueprint == null ? List.of() : resolvedBlueprint.petitionModels());
        workspace.put("selectedPetitionModel", resolvedBlueprint == null ? null : stringOf(resolvedBlueprint.editorBlueprint().get("recommendedModelCode")));
        workspace.put("resolvedProcedureFamily", resolvedBlueprint == null ? null : stringOf(resolvedBlueprint.editorBlueprint().get("resolvedProcedureFamily")));
        workspace.put("requiredDocumentsByTrack", resolvedBlueprint == null ? List.of() : resolvedBlueprint.requiredDocuments());
        workspace.put("petitionReading", buildReadingSummary(intent, routing, extraction, natureza));
        workspace.put("autopreenchimento", buildAutofillMap(intent, routing, natureza, cidadeFato, ufFato, cidadeProtocolo, ufProtocolo, valorCausa));
        return Map.copyOf(workspace);
    }

    private List<Map<String, Object>> buildQuestionBlocks(PeticionamentoSessaoRequest request,
                                                          AjuizamentoIntent intent,
                                                          ProceduralRoutingReport routing,
                                                          Map<String, Object> identidadeVisual,
                                                          String cidadeFato,
                                                          String ufFato,
                                                          String cidadeProtocolo,
                                                          String ufProtocolo,
                                                          BigDecimal valorCausa,
                                                          List<String> pendencias,
                                                          PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint resolvedBlueprint) {
        ArrayList<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(block("PARTES_E_REPRESENTACAO", "Quem participa do caso", List.of(
                field("parteAutora", request.getParteAutora(), true),
                field("parteRe", request.getParteRe(), true),
                field("tipoInstrumentoRepresentacao", request.getTipoInstrumentoRepresentacao(), false)
        )));
        blocks.add(block("LOCAL_DO_FATO", "Onde o caso aconteceu", List.of(
                field("cidadeFato", cidadeFato, true),
                field("ufFato", ufFato, true)
        )));
        blocks.add(block("LEITURA_AUTOMATICA_COMPETENCIA", "O que o sistema já entendeu até aqui", List.of(
                field("ramoDireitoInferido", firstNonBlank(request.getRamoDireito(), intent == null ? null : intent.ramoDireito()), false),
                field("ritoProcessualInferido", firstNonBlank(request.getRitoProcessual(), intent == null ? null : intent.rito(), routing == null ? null : routing.ritoSugerido()), false),
                field("classeProcessualInferida", firstNonBlank(request.getClasseProcessual(), routing == null ? null : routing.actionFamily()), false),
                field("naturezaJuridicaInferida", firstNonBlank(request.getNaturezaJuridica(), metadataAsString(routing, "naturezaJuridicaCanonical")), false),
                field("localDeIngressoSugerido", localDeIngressoSugerido(cidadeProtocolo, ufProtocolo), false)
        )));
        blocks.add(block("NARRATIVA_E_PEDIDOS", "Resumo do caso, pedidos e valor", List.of(
                field("textoFatosResumido", request.getTextoFatosResumido(), true),
                field("valorCausa", valorCausa == null ? null : valorCausa.toPlainString(), ramoExigeValor(firstNonBlank(request.getRamoDireito(), intent == null ? null : intent.ramoDireito()))),
                field("tutelaUrgencia", request.tutelaUrgenciaResolvida() ? "SIM" : null, false)
        )));
        blocks.add(block("IDENTIDADE_VISUAL", "Apresentação visual da peça", List.of(
                field("nomeExibicao", stringOf(identidadeVisual.get("nomeExibicao")), false),
                field("nomeInstituicao", stringOf(identidadeVisual.get("nomeInstituicao")), false),
                field("brasaoOuLogomarcaUri", stringOf(identidadeVisual.get("brasaoOuLogomarcaUri")), false),
                field("paletaPrimaria", stringOf(identidadeVisual.get("paletaPrimaria")), false),
                field("rodapeLivre", stringOf(identidadeVisual.get("rodapeLivre")), false)
        )));
        if (resolvedBlueprint != null && resolvedBlueprint.specializedQuestionBlocks() != null && !resolvedBlueprint.specializedQuestionBlocks().isEmpty()) {
            blocks.addAll(resolvedBlueprint.specializedQuestionBlocks());
        }
        if (!pendencias.isEmpty()) {
            LinkedHashMap<String, Object> attention = new LinkedHashMap<>();
            attention.put("code", "PENDENCIAS_CRITICAS");
            attention.put("label", "Pendências antes do protocolo");
            attention.put("completion", 0);
            attention.put("requiredMissing", List.copyOf(pendencias));
            blocks.add(Map.copyOf(attention));
        }
        return List.copyOf(blocks);
    }

    private Map<String, Object> block(String code, String label, List<Map<String, Object>> fields) {
        LinkedHashMap<String, Object> block = new LinkedHashMap<>();
        int totalRequired = 0;
        int filledRequired = 0;
        ArrayList<String> missing = new ArrayList<>();
        for (Map<String, Object> field : fields) {
            if (!Boolean.TRUE.equals(field.get("required"))) {
                continue;
            }
            totalRequired++;
            if (Boolean.TRUE.equals(field.get("filled"))) {
                filledRequired++;
            } else {
                missing.add(stringOf(field.get("name")));
            }
        }
        int completion = totalRequired == 0 ? 100 : (int) Math.round((filledRequired * 100.0d) / totalRequired);
        block.put("code", code);
        block.put("label", label);
        block.put("fields", List.copyOf(fields));
        block.put("completion", completion);
        block.put("requiredMissing", List.copyOf(missing));
        return Map.copyOf(block);
    }

    private Map<String, Object> field(String name, String value, boolean required) {
        LinkedHashMap<String, Object> field = new LinkedHashMap<>();
        String normalized = trimToNull(value);
        field.put("name", name);
        field.put("label", fieldLabel(name));
        field.put("helperText", fieldHelperText(name));
        field.put("value", normalized);
        field.put("required", required);
        field.put("filled", normalized != null);
        field.put("plainLanguage", true);
        return field;
    }

    private String fieldLabel(String name) {
        if (name == null || name.isBlank()) {
            return "Campo guiado";
        }
        return switch (name) {
            case "parteAutora" -> "Quem está ajuizando?";
            case "parteRe" -> "Contra quem é o pedido?";
            case "tipoInstrumentoRepresentacao" -> "Como a parte está sendo representada?";
            case "cidadeFato" -> "Cidade principal do caso";
            case "ufFato" -> "UF principal do caso";
            case "ramoDireitoInferido" -> "Área jurídica identificada pelo sistema";
            case "ritoProcessualInferido" -> "Caminho processual provável";
            case "classeProcessualInferida" -> "Tipo de ação mais provável";
            case "naturezaJuridicaInferida" -> "Natureza principal do problema";
            case "localDeIngressoSugerido" -> "Local de ingresso sugerido pelo sistema";
            case "textoFatosResumido" -> "Resumo dos fatos";
            case "valorCausa" -> "Valor da causa";
            case "tutelaUrgencia" -> "Há urgência?";
            case "nomeExibicao" -> "Nome exibido na peça";
            case "nomeInstituicao" -> "Instituição ou escritório";
            case "brasaoOuLogomarcaUri" -> "Brasão ou logomarca";
            case "paletaPrimaria" -> "Cor principal";
            case "rodapeLivre" -> "Rodapé livre";
            default -> humanizeFieldName(name);
        };
    }

    private String fieldHelperText(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        return switch (name) {
            case "localDeIngressoSugerido" -> "Esse local é sugerido pelo PJB a partir dos fatos e pode mudar se aparecer uma informação melhor.";
            case "ramoDireitoInferido", "ritoProcessualInferido", "classeProcessualInferida", "naturezaJuridicaInferida" -> "Leitura preliminar feita pelo sistema para te ajudar a protocolar com mais segurança.";
            case "cidadeFato", "ufFato" -> "Se o caso envolveu mais de um lugar, informe o local principal ou o mais ligado ao conflito.";
            case "tipoInstrumentoRepresentacao" -> "Procuração, mandato legal, atuação institucional ou outra forma de representação.";
            default -> "";
        };
    }

    private String humanizeFieldName(String name) {
        String normalized = name == null ? "" : name.replaceAll("([a-z])([A-Z])", "$1 $2").replace('_', ' ').trim();
        if (normalized.isBlank()) {
            return "Campo guiado";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String localDeIngressoSugerido(String cidadeProtocolo, String ufProtocolo) {
        String cidade = trimToNull(cidadeProtocolo);
        String uf = trimToNull(ufProtocolo);
        if (cidade == null && uf == null) {
            return null;
        }
        if (cidade == null) {
            return uf;
        }
        return uf == null ? cidade : cidade + "/" + uf;
    }

    private Map<String, Object> buildEditorBlueprint(PeticionamentoVisualIdentityRequest identity,
                                                     AjuizamentoIntent intent,
                                                     ProceduralRoutingReport routing,
                                                     Usuario usuario,
                                                     PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint resolvedBlueprint) {
        LinkedHashMap<String, Object> editor = new LinkedHashMap<>(resolvedBlueprint == null ? Map.of() : resolvedBlueprint.editorBlueprint());
        if (!editor.containsKey("surface")) {
            editor.put("surface", "EDITOR_NATIVO_PJB");
        }
        editor.put("visualIdentity", identidadeParaMapa(identity));
        put(editor, "resolvedProfile", usuario == null || usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().papelArquitetural());
        put(editor, "suggestedTrack", firstNonBlank(intent == null ? null : intent.rito(), routing == null ? null : routing.ritoSugerido()));
        put(editor, "suggestedJustice", routing == null ? null : routing.tipoJusticaSugerida());
        put(editor, "recommendedModelCode", resolvedBlueprint == null ? null : stringOf(resolvedBlueprint.editorBlueprint().get("recommendedModelCode")));
        put(editor, "resolvedProcedureFamily", resolvedBlueprint == null ? null : stringOf(resolvedBlueprint.editorBlueprint().get("resolvedProcedureFamily")));
        return Map.copyOf(editor);
    }

    private Map<String, Object> buildReadingSummary(AjuizamentoIntent intent,
                                                    ProceduralRoutingReport routing,
                                                    SectionExtraction extraction,
                                                    String natureza) {
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("petitionDetected", !extraction.rawText().isBlank());
        put(summary, "titleDetected", extraction.title());
        summary.put("factsDetected", extraction.facts());
        summary.put("requestsDetected", extraction.requests());
        summary.put("legalGroundsDetected", extraction.legalGrounds());
        summary.put("evidenceDetected", extraction.evidence());
        summary.put("urgencyDetected", extraction.urgencyDetected());
        put(summary, "naturezaJuridica", natureza);
        LinkedHashMap<String, Object> intentMap = new LinkedHashMap<>();
        if (intent != null) {
            put(intentMap, "ramoDireito", intent.ramoDireito());
            put(intentMap, "rito", intent.rito());
            put(intentMap, "tipoAcao", intent.tipoAcao());
            put(intentMap, "competencia", intent.competencia());
            put(intentMap, "esfera", intent.esfera());
            intentMap.put("confianca", intent.confianca());
            intentMap.put("camposObrigatorios", intent.camposObrigatorios());
            intentMap.put("documentosEssenciais", intent.documentosEssenciais());
            intentMap.put("alertas", intent.alertas());
            intentMap.put("proximosPassos", intent.proximosPassos());
        }
        summary.put("intent", intentMap);
        summary.put("proceduralRouting", routing == null ? Map.of() : routing.toMap());
        return summary;
    }

    private Map<String, Object> buildAutofillMap(AjuizamentoIntent intent,
                                                 ProceduralRoutingReport routing,
                                                 String natureza,
                                                 String cidadeFato,
                                                 String ufFato,
                                                 String cidadeProtocolo,
                                                 String ufProtocolo,
                                                 BigDecimal valorCausa) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        put(map, "ramoDireito", intent == null ? null : intent.ramoDireito());
        put(map, "ritoProcessual", intent == null ? null : intent.rito());
        put(map, "tipoAcao", intent == null ? null : intent.tipoAcao());
        put(map, "competencia", intent == null ? null : intent.competencia());
        put(map, "tipoJustica", routing == null ? null : routing.tipoJusticaSugerida());
        put(map, "tribunalCodigo", routing == null ? null : routing.tribunalCodigo());
        put(map, "foroSugerido", routing == null ? null : routing.foroSugerido());
        put(map, "cidadeFato", cidadeFato);
        put(map, "ufFato", ufFato);
        put(map, "cidadeProtocolo", cidadeProtocolo);
        put(map, "ufProtocolo", ufProtocolo);
        put(map, "naturezaJuridica", natureza);
        put(map, "valorCausa", valorCausa == null ? null : valorCausa.toPlainString());
        return map;
    }

    private PeticionamentoVisualIdentityRequest resolveIdentity(PeticionamentoVisualIdentityRequest supplied, Usuario usuario, String ufProtocolo) {
        PeticionamentoVisualIdentityRequest identity = supplied == null ? new PeticionamentoVisualIdentityRequest() : supplied;
        if (trimToNull(identity.getNomeExibicao()) == null) {
            identity.setNomeExibicao(defaultVisualName(usuario));
        }
        if (trimToNull(identity.getNomeInstituicao()) == null) {
            identity.setNomeInstituicao(defaultInstitutionName(usuario, ufProtocolo));
        }
        if (trimToNull(identity.getPaletaPrimaria()) == null) {
            identity.setPaletaPrimaria(defaultPrimaryColor(usuario));
        }
        if (trimToNull(identity.getPaletaSecundaria()) == null) {
            identity.setPaletaSecundaria(defaultSecondaryColor(usuario));
        }
        if (identity.getExibirRegistroProfissional() == null) {
            identity.setExibirRegistroProfissional(usuario != null && usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isAdvocacia() || usuario.getTipoUsuario().isDefensoriaPublica() || usuario.getTipoUsuario().isProcuradoria() || usuario.getTipoUsuario().isMinisterioPublico()));
        }
        if (identity.getExibirBrasaoOuLogomarca() == null) {
            identity.setExibirBrasaoOuLogomarca(Boolean.TRUE);
        }
        if (trimToNull(identity.getCabecalhoLivre()) == null) {
            identity.setCabecalhoLivre(defaultHeader(usuario, ufProtocolo));
        }
        if (trimToNull(identity.getRodapeLivre()) == null) {
            identity.setRodapeLivre(defaultFooter(usuario));
        }
        return identity;
    }

    private LinkedHashMap<String, Object> identidadeParaMapa(PeticionamentoVisualIdentityRequest identity) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (identity == null) {
            return map;
        }
        put(map, "nomeExibicao", identity.getNomeExibicao());
        put(map, "nomeInstituicao", identity.getNomeInstituicao());
        put(map, "brasaoOuLogomarcaUri", identity.getBrasaoOuLogomarcaUri());
        put(map, "cabecalhoLivre", identity.getCabecalhoLivre());
        put(map, "rodapeLivre", identity.getRodapeLivre());
        put(map, "paletaPrimaria", identity.getPaletaPrimaria());
        put(map, "paletaSecundaria", identity.getPaletaSecundaria());
        if (identity.getExibirRegistroProfissional() != null) {
            map.put("exibirRegistroProfissional", identity.getExibirRegistroProfissional());
        }
        if (identity.getExibirBrasaoOuLogomarca() != null) {
            map.put("exibirBrasaoOuLogomarca", identity.getExibirBrasaoOuLogomarca());
        }
        return map;
    }

    private LinkedHashMap<String, Object> buildIntentPayload(PeticionamentoSessaoRequest request, Usuario usuario, String petitionText) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        put(payload, "tituloCaso", request.getTituloCaso());
        put(payload, "parteAutora", request.getParteAutora());
        put(payload, "parteRe", request.getParteRe());
        put(payload, "ramoDireito", request.getRamoDireito());
        put(payload, "ritoProcessual", request.getRitoProcessual());
        put(payload, "classeProcessual", request.getClasseProcessual());
        put(payload, "tipoJustica", request.getTipoJustica());
        put(payload, "textoFatosResumido", request.getTextoFatosResumido());
        put(payload, "fatos", request.getFatos());
        put(payload, "fundamentos", request.getFundamentosJuridicos());
        put(payload, "pedidos", request.getPedidos());
        put(payload, "provas", request.getProvasIndicadas());
        put(payload, "cidadeFato", request.getCidadeFato());
        put(payload, "ufFato", request.getUfFato());
        put(payload, "cidadeProtocolo", request.getCidadeProtocolo());
        put(payload, "ufProtocolo", request.getUfProtocolo());
        put(payload, "natureza_acao", request.getNaturezaJuridica());
        put(payload, "peticaoInicial", petitionText);
        if (request.getValorCausa() != null) {
            payload.put("valorCausa", request.getValorCausa().toPlainString());
        }
        if (request.tutelaUrgenciaResolvida()) {
            payload.put("liminar", Boolean.TRUE);
        }
        if (usuario != null) {
            put(payload, "perfilPeticionante", usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name());
            put(payload, "comarcaUsuario", usuario.getComarca());
            put(payload, "ufUsuario", usuario.getUf());
        }
        if (request.getCtx() != null && !request.getCtx().isEmpty()) {
            request.getCtx().forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null) {
                    payload.putIfAbsent(key, value);
                }
            });
        }
        return payload;
    }

    private List<String> fallbackNarrative(String summary) {
        String normalized = trimToNull(summary);
        if (normalized == null) {
            return List.of();
        }
        return List.of(normalized);
    }

    private List<String> inferEvidenceChecklist(AjuizamentoIntent intent, String petitionText) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (petitionText != null && !petitionText.isBlank()) {
            if (containsNormalized(petitionText, "contrato", "aditivo", "termo contratual")) {
                out.add("Juntar contrato ou instrumento principal mencionado na narrativa.");
            }
            if (containsNormalized(petitionText, "boletim de ocorrencia", "b.o.", "inquerito")) {
                out.add("Confirmar juntada de boletim de ocorrência, inquérito ou peça policial correlata.");
            }
            if (containsNormalized(petitionText, "laudo", "pericia", "exame", "prontuario")) {
                out.add("Confirmar laudo, exame ou documento técnico mencionado na petição.");
            }
        }
        if (intent != null && intent.documentosEssenciais() != null) {
            out.addAll(intent.documentosEssenciais());
        }
        return List.copyOf(out);
    }

    private List<String> chooseList(List<String> explicit, List<String> extracted, List<String> fallback) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        addAllNormalized(out, explicit);
        if (out.isEmpty()) {
            addAllNormalized(out, extracted);
        }
        if (out.isEmpty()) {
            addAllNormalized(out, fallback);
        }
        return List.copyOf(out);
    }

    private void addAllNormalized(Set<String> target, List<String> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (String value : source) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                target.add(normalized);
            }
        }
    }

    private String resolvePetitionText(PeticionamentoSessaoRequest request) {
        String resolved = firstNonBlank(request.getTextoPeticaoLivre(), request.getDraftMarkdown(), mapString(request.getCtx(), "peticaoInicial"), mapString(request.getCtx(), "peticaoInicialText"), mapString(request.getCtx(), "documentoPrincipalTexto"));
        return resolved == null ? "" : resolved;
    }

    private SectionExtraction extractSections(String rawText) {
        String text = trimToNull(rawText);
        if (text == null) {
            return new SectionExtraction("EDITOR_NATIVO_BLOCOS", "", null, List.of(), List.of(), List.of(), List.of(), false, null, null, null);
        }
        ArrayList<String> facts = new ArrayList<>();
        ArrayList<String> legal = new ArrayList<>();
        ArrayList<String> requests = new ArrayList<>();
        ArrayList<String> evidence = new ArrayList<>();
        String title = null;
        String cityHint = null;
        String ufHint = null;
        boolean urgencyDetected = false;
        String current = null;
        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            String line = trimToNull(rawLine);
            if (line == null) {
                continue;
            }
            String heading = normalizeHeading(line);
            if (title == null && line.length() > 8 && line.length() < 180 && !heading.isBlank() && !isAllCapsShort(heading)) {
                title = line;
            }
            if (matchesHeading(heading, FACT_HEADINGS)) {
                current = "FACTS";
                continue;
            }
            if (matchesHeading(heading, LEGAL_HEADINGS)) {
                current = "LEGAL";
                continue;
            }
            if (matchesHeading(heading, REQUEST_HEADINGS)) {
                current = "REQUESTS";
                continue;
            }
            if (matchesHeading(heading, EVIDENCE_HEADINGS)) {
                current = "EVIDENCE";
                continue;
            }
            if (matchesHeading(heading, URGENCY_HEADINGS)) {
                urgencyDetected = true;
                current = "LEGAL";
                continue;
            }
            if (containsNormalized(line, "cidade de", "comarca de", "foro de")) {
                cityHint = cityHint == null ? extractCity(line) : cityHint;
            }
            if (ufHint == null) {
                ufHint = extractUf(line);
            }
            appendToCurrent(current, line, facts, legal, requests, evidence);
            if (containsNormalized(line, "tutela de urgencia", "liminar", "perigo de dano", "probabilidade do direito")) {
                urgencyDetected = true;
            }
        }
        if (title == null && lines.length > 0) {
            title = trimToNull(lines[0]);
        }
        BigDecimal valorCausa = parseMoney(text);
        return new SectionExtraction("UPLOAD_INTELIGENTE_LEITURA_ASSISTIDA", text, title, sanitizeItems(facts), sanitizeItems(legal), sanitizeItems(requests), sanitizeItems(evidence), urgencyDetected, valorCausa, cityHint, ufHint);
    }

    private void appendToCurrent(String current, String line, List<String> facts, List<String> legal, List<String> requests, List<String> evidence) {
        String cleaned = cleanLine(line);
        if (cleaned == null) {
            return;
        }
        switch (current == null ? "" : current) {
            case "FACTS" -> facts.add(cleaned);
            case "LEGAL" -> legal.add(cleaned);
            case "REQUESTS" -> requests.add(cleaned);
            case "EVIDENCE" -> evidence.add(cleaned);
            default -> {
                if (facts.size() < 4 && cleaned.length() > 30) {
                    facts.add(cleaned);
                }
            }
        }
    }

    private boolean matchesHeading(String heading, Set<String> candidates) {
        if (heading.isBlank()) {
            return false;
        }
        String normalized = normalizeToken(heading);
        if (candidates.contains(normalized)) {
            return true;
        }
        for (String candidate : candidates) {
            if (normalized.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeHeading(String line) {
        Matcher matcher = HEADING_PATTERN.matcher(line.trim().toUpperCase(Locale.ROOT));
        String candidate = matcher.matches() ? matcher.group(1) : line.toUpperCase(Locale.ROOT);
        return normalizeToken(candidate);
    }

    private String normalizeToken(String value) {
        String normalized = value == null ? "" : value.toUpperCase(Locale.ROOT);
        normalized = normalized
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
        normalized = normalized.replaceAll("[^A-Z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private boolean isAllCapsShort(String value) {
        return value != null && value.equals(value.toUpperCase(Locale.ROOT)) && value.length() < 80;
    }

    private List<String> sanitizeItems(List<String> source) {
        LinkedHashSet<String> items = new LinkedHashSet<>();
        for (String value : source) {
            String normalized = cleanLine(value);
            if (normalized != null) {
                items.add(normalized);
            }
        }
        return List.copyOf(items);
    }

    private String cleanLine(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.replaceFirst("^[\\-•*\\d\\.)\\s]+", "").trim();
        return normalized.length() < 3 ? null : normalized;
    }

    private BigDecimal parseMoney(String text) {
        Matcher matcher = MONEY_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String raw = matcher.group(1);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.replace(".", "").replace(',', '.').replaceAll("[^0-9.]", "");
        try {
            return normalized.isBlank() ? null : new BigDecimal(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean containsNormalized(String text, String... needles) {
        String haystack = normalizeToken(text);
        for (String needle : needles) {
            if (haystack.contains(normalizeToken(needle))) {
                return true;
            }
        }
        return false;
    }

    private String extractCity(String line) {
        String normalized = trimToNull(line);
        if (normalized == null) {
            return null;
        }
        String[] markers = {"cidade de ", "comarca de ", "foro de "};
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx >= 0) {
                String city = normalized.substring(idx + marker.length()).replaceAll("[\\.,;:].*$", "").trim();
                return city.isBlank() ? null : city;
            }
        }
        return null;
    }

    private String extractUf(String line) {
        String normalized = trimToNull(line);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\b([A-Z]{2})\\b").matcher(normalized.toUpperCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!"DO".equals(token) && !"DA".equals(token) && !"DE".equals(token)) {
                return token;
            }
        }
        return null;
    }

    private String defaultVisualName(Usuario usuario) {
        if (usuario == null) {
            return "Peticionante PJB";
        }
        if (usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAdvocacia() && usuario.getOab() != null && !usuario.getOab().isBlank()) {
            return usuario.getNome() + " - OAB " + usuario.getOab();
        }
        return firstNonBlank(usuario.getNome(), "Peticionante PJB");
    }

    private String defaultInstitutionName(Usuario usuario, String ufProtocolo) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return firstNonBlank(ufProtocolo == null ? null : "Poder Judiciário - " + ufProtocolo, "PJB");
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo.isAdvocacia()) {
            return "Escritório / Advocacia";
        }
        if (tipo.isDefensoriaPublica()) {
            return "Defensoria Pública";
        }
        if (tipo.isProcuradoria()) {
            return "Procuradoria";
        }
        if (tipo.isMinisterioPublico()) {
            return "Ministério Público";
        }
        return "Peticionante institucional";
    }

    private String defaultPrimaryColor(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "#163A63";
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo.isAdvocacia()) {
            return "#1F3C88";
        }
        if (tipo.isDefensoriaPublica()) {
            return "#145A32";
        }
        if (tipo.isProcuradoria()) {
            return "#7D6608";
        }
        if (tipo.isMinisterioPublico()) {
            return "#6C3483";
        }
        return "#163A63";
    }

    private String defaultSecondaryColor(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAdvocacia() ? "#D4AF37" : "#AAB7C4";
    }

    private String defaultHeader(Usuario usuario, String ufProtocolo) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return firstNonBlank(ufProtocolo == null ? null : "Cabeçalho institucional " + ufProtocolo, "Cabeçalho peticionante");
        }
        if (usuario.getTipoUsuario().isAdvocacia()) {
            return "Cabeçalho do escritório / advogado";
        }
        if (usuario.getTipoUsuario().isDefensoriaPublica()) {
            return "Cabeçalho institucional da Defensoria Pública";
        }
        if (usuario.getTipoUsuario().isMinisterioPublico()) {
            return "Cabeçalho institucional do Ministério Público";
        }
        if (usuario.getTipoUsuario().isProcuradoria()) {
            return "Cabeçalho institucional da Procuradoria";
        }
        return "Cabeçalho institucional do peticionante";
    }

    private String defaultFooter(Usuario usuario) {
        if (usuario == null) {
            return "Documento preparado no PJB";
        }
        return usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAdvocacia()
                ? "Rodapé com identificação profissional e canais do escritório"
                : "Rodapé institucional com identificação do órgão peticionante";
    }

    private void collectMissing(List<String> target, String value, String message) {
        if (trimToNull(value) == null) {
            target.add(message);
        }
    }

    private boolean ramoExigeValor(String ramo) {
        String normalized = trimToNull(ramo);
        if (normalized == null) {
            return true;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return !normalized.contains("PENAL") && !normalized.contains("ELEITORAL") && !normalized.contains("MILITAR");
    }

    private String metadataAsString(ProceduralRoutingReport routing, String key) {
        if (routing == null || routing.metadata() == null || key == null) {
            return null;
        }
        Object value = routing.metadata().get(key);
        return value == null ? null : trimToNull(value.toString());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> metadataAsMap(ProceduralRoutingReport routing, String key) {
        if (routing == null || routing.metadata() == null || key == null) {
            return Map.of();
        }
        Object value = routing.metadata().get(key);
        if (value instanceof Map<?, ?> raw) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            raw.forEach((mapKey, mapValue) -> {
                if (mapKey != null && mapValue != null) {
                    out.put(String.valueOf(mapKey), mapValue);
                }
            });
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> jurisdictionQuestions(ProceduralRoutingReport routing, String key) {
        Map<String, Object> intake = metadataAsMap(routing, "jurisdictionIntake");
        Object value = intake.get(key);
        if (!(value instanceof List<?> raw) || raw.isEmpty()) {
            return List.of();
        }
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> safe = new LinkedHashMap<>();
                map.forEach((questionKey, questionValue) -> {
                    if (questionKey != null && questionValue != null) {
                        safe.put(String.valueOf(questionKey), questionValue);
                    }
                });
                if (!safe.isEmpty()) {
                    out.add(Map.copyOf(safe));
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private Map<String, Object> jurisdictionResolutionPolicy(ProceduralRoutingReport routing) {
        Map<String, Object> intake = metadataAsMap(routing, "jurisdictionIntake");
        Object value = intake.get("resolutionPolicy");
        if (value instanceof Map<?, ?> raw) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            raw.forEach((policyKey, policyValue) -> {
                if (policyKey != null && policyValue != null) {
                    out.put(String.valueOf(policyKey), policyValue);
                }
            });
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
        }
        return Map.of();
    }

    private String mapString(Map<String, Object> source, String key) {
        if (source == null || source.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        Object value = source.get(key);
        return value == null ? null : trimToNull(value.toString());
    }

    private String stringOf(Object value) {
        return value == null ? null : trimToNull(value.toString());
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record IntakeResult(
            LaianePeticaoInicialDraftService.EstruturarRequest resolvedDraftRequest,
            Map<String, Object> workspace,
            List<String> automacoes,
            List<String> pendencias,
            Map<String, Object> envelope,
            PeticionamentoEditorBlueprintCatalogService.ResolvedEditorBlueprint editorBlueprint
    ) {
    }

    private record SectionExtraction(
            String sourceMode,
            String rawText,
            String title,
            List<String> facts,
            List<String> legalGrounds,
            List<String> requests,
            List<String> evidence,
            boolean urgencyDetected,
            BigDecimal valorCausa,
            String cityHint,
            String ufHint
    ) {
    }
}
