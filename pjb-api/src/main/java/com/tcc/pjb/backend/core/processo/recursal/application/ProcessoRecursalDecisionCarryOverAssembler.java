package com.tcc.pjb.backend.core.processo.recursal.application;

import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalDecisionCarryOver;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalDecisionLayer;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalDecisionSourceDocument;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ProcessoRecursalDecisionCarryOverAssembler {

    private ProcessoRecursalDecisionCarryOverAssembler() {
    }

    public static ProcessoRecursalDecisionCarryOver assemble(Processo processo,
                                                             String scope,
                                                             String sourceTimelineMode,
                                                             String targetTimelineMode) {
        return assemble(processo, scope, sourceTimelineMode, targetTimelineMode, null, null);
    }

    public static ProcessoRecursalDecisionCarryOver assemble(Processo processo,
                                                             String scope,
                                                             String sourceTimelineMode,
                                                             String targetTimelineMode,
                                                             DocumentoProcessualRepository documentoRepository,
                                                             DocumentoPaginaRepository paginaRepository) {
        if (processo == null) {
            return new ProcessoRecursalDecisionCarryOver(
                    scope,
                    "DECISAO_JUDICIAL",
                    inferStage(scope, null),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    sourceTimelineMode,
                    targetTimelineMode,
                    List.of(),
                    List.of(),
                    List.of()
            );
        }
        String resultadoFinal = normalized(processo.getResultadoFinal());
        String resumoIa = normalized(processo.getResumoIA());
        String analiseTriagem = normalized(processo.getAnaliseTriagemV1());
        String objeto = normalized(processo.getObjetoProcessual());
        String pedido = normalized(processo.getPedidoPrincipal());
        String materialProbatorio = normalized(processo.getMaterialProbatorioResumo());
        String peticaoInicial = normalized(processo.getPeticaoInicialText());

        List<DocumentoProcessual> documentos = fetchProcessoDocumentos(processo, documentoRepository);
        List<DocumentoProcessual> documentosDecisorios = documentos.stream()
                .filter(ProcessoRecursalDecisionCarryOverAssembler::looksLikeDecisionDocument)
                .sorted(Comparator.comparing(DocumentoProcessual::getCriadoEm, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DocumentoProcessual::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<ProcessoRecursalDecisionLayer> trilhaDecisoriaIntegral = buildDecisionLayers(processo, documentosDecisorios, paginaRepository);
        ProcessoRecursalDecisionSourceDocument documentoOriginal = selectPrimaryDecisionDocument(scope, processo, trilhaDecisoriaIntegral);

        LinkedHashSet<String> sinais = new LinkedHashSet<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        if (documentoOriginal != null && documentoOriginal.available()) {
            sinais.add("DECISAO_ORIGINAL_INTEGRA_ACOPLADA");
            fundamentos.add("A decisão judicial imediatamente impugnada segue acoplada ao fluxo recursal ou integrativo com rota documental própria, sem substituição por síntese.");
            if (documentoOriginal.hasPdfView()) {
                sinais.add("PDF_ORIGINAL_DISPONIVEL_AO_JULGADOR");
                fundamentos.add("O documento original pode ser aberto diretamente no destino para leitura integral da sentença, decisão ou acórdão impugnado.");
            }
            if (documentoOriginal.hasIntegralText()) {
                sinais.add("TEXTO_INTEGRAL_EXTRAIDO_DISPONIVEL");
                fundamentos.add("O texto integral extraído do documento original acompanha a tramitação quando disponível, preservando leitura completa no recurso ou nos embargos.");
            }
        }
        if (trilhaDecisoriaIntegral.size() > 1) {
            sinais.add("CADEIA_DECISORIA_CUMULATIVA_ACOPLADA");
            fundamentos.add("A sentença de primeiro grau e as decisões recursais supervenientes acompanham cumulativamente o processo até os graus superiores, preservando a sequência decisória integral.");
        }
        if (resultadoFinal != null) {
            sinais.add("DECISAO_FINAL_MATERIALIZADA");
            fundamentos.add("O dispositivo final e o resultado do julgamento continuam visíveis como apoio operacional, sem substituir a íntegra das decisões originais acopladas.");
        }
        if (materialProbatorio != null) {
            sinais.add("PROVA_RESUMIDA_TRANSPORTADA");
            fundamentos.add("O resumo probatório relevante é transportado junto com a decisão anterior para evitar leitura fragmentada entre graus ou na integração de embargos.");
        }
        if (peticaoInicial != null || pedido != null || objeto != null) {
            sinais.add("TESE_ORIGINARIA_MATERIALIZADA");
            fundamentos.add("Pedido principal, objeto processual e petição inicial resumida permanecem acessíveis para comparação entre a tese originária e a decisão impugnada.");
        }
        if (analiseTriagem != null || resumoIa != null) {
            sinais.add("SINTETICO_OPERACIONAL_AUXILIAR");
            fundamentos.add("Síntese operacional e leitura prévia do caso acompanham o fluxo apenas como apoio secundário de leitura, nunca como substituto da decisão original.");
        }
        String resumoDecisorio = firstNonBlank(resultadoFinal, resumoIa, analiseTriagem, pedido, objeto);
        return new ProcessoRecursalDecisionCarryOver(
                normalized(scope),
                inferDecisionType(processo),
                inferStage(scope, processo.getStatusProcesso()),
                safeNumeroProcesso(processo),
                firstNonBlank(normalized(processo.getNumeroCNJ()), safeNumeroProcesso(processo)),
                firstNonBlank(normalized(processo.getTribunal()), normalized(processo.getTribunalCodigoRoteado())),
                firstNonBlank(normalized(processo.getVara()), normalized(processo.getUnidadeJudiciariaCodigo())),
                normalized(processo.getClasseProcessual()),
                normalized(processo.getAssunto()),
                objeto,
                pedido,
                resultadoFinal,
                resumoDecisorio,
                documentoOriginal,
                materialProbatorio,
                shorten(peticaoInicial, 1200),
                analiseTriagem,
                normalized(sourceTimelineMode),
                normalized(targetTimelineMode),
                trilhaDecisoriaIntegral,
                List.copyOf(sinais),
                List.copyOf(fundamentos)
        );
    }

    public static Map<String, Object> asMap(ProcessoRecursalDecisionCarryOver carryOver) {
        if (carryOver == null || !carryOver.available()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        put(out, "scope", carryOver.scope());
        put(out, "sourceDecisionType", carryOver.sourceDecisionType());
        put(out, "sourceDecisionStage", carryOver.sourceDecisionStage());
        put(out, "numeroProcessoOrigem", carryOver.numeroProcessoOrigem());
        put(out, "numeroCnjOrigem", carryOver.numeroCnjOrigem());
        put(out, "tribunalOrigem", carryOver.tribunalOrigem());
        put(out, "unidadeOrigem", carryOver.unidadeOrigem());
        put(out, "orgaoOrigem", carryOver.sourceOrganLabel());
        put(out, "classeProcessual", carryOver.classeProcessual());
        put(out, "assunto", carryOver.assunto());
        put(out, "objetoProcessual", carryOver.objetoProcessual());
        put(out, "pedidoPrincipal", carryOver.pedidoPrincipal());
        put(out, "resultadoFinal", carryOver.resultadoFinal());
        put(out, "resumoDecisorio", carryOver.resumoDecisorio());
        if (carryOver.documentoOriginalDecisao() != null && carryOver.documentoOriginalDecisao().available()) {
            out.put("documentoDecisaoOriginal", sourceDocumentAsMap(carryOver.documentoOriginalDecisao()));
        }
        if (!carryOver.trilhaDecisoriaIntegral().isEmpty()) {
            out.put("trilhaDecisoriaIntegral", carryOver.trilhaDecisoriaIntegral().stream().filter(ProcessoRecursalDecisionLayer::available).map(ProcessoRecursalDecisionCarryOverAssembler::layerAsMap).toList());
        }
        put(out, "materialProbatorioResumo", carryOver.materialProbatorioResumo());
        put(out, "peticaoInicialResumo", carryOver.peticaoInicialResumo());
        put(out, "triagemResumo", carryOver.triagemResumo());
        put(out, "sourceTimelineMode", carryOver.sourceTimelineMode());
        put(out, "targetTimelineMode", carryOver.targetTimelineMode());
        if (!carryOver.carryOverSignals().isEmpty()) {
            out.put("carryOverSignals", carryOver.carryOverSignals());
        }
        if (!carryOver.fundamentosExibicao().isEmpty()) {
            out.put("fundamentosExibicao", carryOver.fundamentosExibicao());
        }
        return out;
    }

    public static List<String> toSurfaceLines(ProcessoRecursalDecisionCarryOver carryOver) {
        if (carryOver == null || !carryOver.available()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        add(out, "CADERNO_DECISORIO_ESCOPO", carryOver.scope());
        add(out, "DECISAO_ANTERIOR_TIPO", carryOver.sourceDecisionType());
        add(out, "DECISAO_ANTERIOR_GRAU", carryOver.sourceDecisionStage());
        add(out, "ORGAO_ORIGEM", carryOver.sourceOrganLabel());
        add(out, "PROCESSO_ORIGEM", carryOver.numeroCnjOrigem());
        add(out, "CLASSE", carryOver.classeProcessual());
        add(out, "ASSUNTO", carryOver.assunto());
        if (carryOver.documentoOriginalDecisao() != null && carryOver.documentoOriginalDecisao().available()) {
            add(out, "DECISAO_ORIGINAL_DOCUMENTO", carryOver.documentoOriginalDecisao().displayTitle());
            add(out, "DECISAO_ORIGINAL_DOCUMENTO_ID", carryOver.documentoOriginalDecisao().documentoId() == null ? null : carryOver.documentoOriginalDecisao().documentoId().toString());
            add(out, "DECISAO_ORIGINAL_PDF", carryOver.documentoOriginalDecisao().pdfEndpoint());
            add(out, "DECISAO_ORIGINAL_MODO", carryOver.documentoOriginalDecisao().visualizationMode());
        }
        if (!carryOver.trilhaDecisoriaIntegral().isEmpty()) {
            out.add("TRILHA_DECISORIA_INTEGRAL_TOTAL=" + carryOver.trilhaDecisoriaIntegral().size());
            carryOver.trilhaDecisoriaIntegral().stream().filter(ProcessoRecursalDecisionLayer::available).forEach(layer -> {
                String prefixo = "TRILHA_DECISORIA_" + layer.ordemSequencial();
                add(out, prefixo + "_GRAU", layer.stageLabel());
                add(out, prefixo + "_TIPO", layer.decisionType());
                add(out, prefixo + "_ORGAO", layer.organLabel());
                if (layer.documentoOriginal() != null && layer.documentoOriginal().available()) {
                    add(out, prefixo + "_DOCUMENTO", layer.documentoOriginal().displayTitle());
                    add(out, prefixo + "_PDF", layer.documentoOriginal().pdfEndpoint());
                }
            });
        }
        add(out, "RESUMO_DECISORIO_AUXILIAR", carryOver.resumoDecisorio());
        add(out, "RESULTADO_FINAL", carryOver.resultadoFinal());
        add(out, "PEDIDO_PRINCIPAL", carryOver.pedidoPrincipal());
        add(out, "OBJETO_PROCESSUAL", carryOver.objetoProcessual());
        add(out, "PROVA_RESUMIDA", carryOver.materialProbatorioResumo());
        add(out, "PETICAO_INICIAL_RESUMIDA", carryOver.peticaoInicialResumo());
        add(out, "TRIAGEM_RESUMIDA", carryOver.triagemResumo());
        add(out, "MODO_TIMELINE_ORIGEM", carryOver.sourceTimelineMode());
        add(out, "MODO_TIMELINE_DESTINO", carryOver.targetTimelineMode());
        if (!carryOver.carryOverSignals().isEmpty()) {
            out.add("CARRY_OVER_SIGNALS=" + String.join(", ", carryOver.carryOverSignals()));
        }
        return List.copyOf(out);
    }

    private static Map<String, Object> sourceDocumentAsMap(ProcessoRecursalDecisionSourceDocument sourceDocument) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (sourceDocument.documentoId() != null) {
            out.put("documentoId", sourceDocument.documentoId().toString());
        }
        put(out, "documentType", sourceDocument.documentType());
        put(out, "titulo", sourceDocument.titulo());
        put(out, "nomeOriginal", sourceDocument.nomeOriginal());
        put(out, "contentType", sourceDocument.contentType());
        if (sourceDocument.tamanhoBytes() != null) {
            out.put("tamanhoBytes", sourceDocument.tamanhoBytes());
        }
        put(out, "sha256", sourceDocument.sha256());
        put(out, "sha384", sourceDocument.sha384());
        put(out, "origemSistema", sourceDocument.origemSistema());
        put(out, "storageBackend", sourceDocument.storageBackend());
        put(out, "storageUri", sourceDocument.storageUri());
        put(out, "pdfEndpoint", sourceDocument.pdfEndpoint());
        put(out, "visualizationMode", sourceDocument.visualizationMode());
        if (sourceDocument.totalPaginas() != null) {
            out.put("totalPaginas", sourceDocument.totalPaginas());
        }
        if (sourceDocument.paginasComTexto() != null) {
            out.put("paginasComTexto", sourceDocument.paginasComTexto());
        }
        put(out, "textoIntegralExtraido", sourceDocument.textoIntegralExtraido());
        if (sourceDocument.criadoEm() != null) {
            out.put("criadoEm", sourceDocument.criadoEm().toString());
        }
        return out;
    }

    private static Map<String, Object> layerAsMap(ProcessoRecursalDecisionLayer layer) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("ordemSequencial", layer.ordemSequencial());
        put(out, "stageLabel", layer.stageLabel());
        put(out, "decisionType", layer.decisionType());
        put(out, "tribunalOrigem", layer.tribunalOrigem());
        put(out, "unidadeOrigem", layer.unidadeOrigem());
        put(out, "numeroCnjOrigem", layer.numeroCnjOrigem());
        put(out, "orgaoOrigem", layer.organLabel());
        if (layer.documentoOriginal() != null && layer.documentoOriginal().available()) {
            out.put("documentoOriginal", sourceDocumentAsMap(layer.documentoOriginal()));
        }
        return out;
    }

    private static List<DocumentoProcessual> fetchProcessoDocumentos(Processo processo, DocumentoProcessualRepository documentoRepository) {
        if (processo == null || processo.getId() == null || documentoRepository == null) {
            return List.of();
        }
        List<DocumentoProcessual> documentos = documentoRepository.findByProcessoId(processo.getId());
        if (documentos == null || documentos.isEmpty()) {
            documentos = documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processo.getId());
        }
        return documentos == null ? List.of() : documentos;
    }

    private static List<ProcessoRecursalDecisionLayer> buildDecisionLayers(Processo processo,
                                                                           List<DocumentoProcessual> documentosDecisorios,
                                                                           DocumentoPaginaRepository paginaRepository) {
        if (documentosDecisorios == null || documentosDecisorios.isEmpty()) {
            return List.of();
        }
        ArrayList<ProcessoRecursalDecisionLayer> layers = new ArrayList<>();
        int grauCorrente = 0;
        for (DocumentoProcessual documento : documentosDecisorios) {
            int proximoGrau = nextDegreeForDocument(documento, grauCorrente);
            grauCorrente = Math.max(1, proximoGrau);
            ProcessoRecursalDecisionSourceDocument sourceDocument = sourceDocumentFromDocumento(documento, paginaRepository);
            layers.add(new ProcessoRecursalDecisionLayer(
                    layers.size() + 1,
                    stageLabelForDegree(grauCorrente),
                    classifyDocumentType(documento),
                    firstNonBlank(normalized(processo.getTribunal()), normalized(processo.getTribunalCodigoRoteado())),
                    firstNonBlank(normalized(processo.getVara()), normalized(processo.getUnidadeJudiciariaCodigo())),
                    firstNonBlank(normalized(processo.getNumeroCNJ()), safeNumeroProcesso(processo)),
                    sourceDocument
            ));
        }
        return List.copyOf(layers);
    }

    private static ProcessoRecursalDecisionSourceDocument selectPrimaryDecisionDocument(String scope,
                                                                                        Processo processo,
                                                                                        List<ProcessoRecursalDecisionLayer> trilha) {
        if (trilha == null || trilha.isEmpty()) {
            return null;
        }
        String normalizedScope = normalized(scope);
        if ("EMBARGOS_MESMO_GRAU".equals(normalizedScope)) {
            return trilha.stream()
                    .filter(ProcessoRecursalDecisionLayer::available)
                    .reduce((a, b) -> b)
                    .map(ProcessoRecursalDecisionLayer::documentoOriginal)
                    .orElse(null);
        }
        if (processo != null && processo.getStatusProcesso() == StatusProcesso.RECURSO_INTERPOSTO) {
            return trilha.stream()
                    .filter(ProcessoRecursalDecisionLayer::available)
                    .reduce((a, b) -> b)
                    .map(ProcessoRecursalDecisionLayer::documentoOriginal)
                    .orElse(null);
        }
        return trilha.stream()
                .filter(ProcessoRecursalDecisionLayer::available)
                .findFirst()
                .map(ProcessoRecursalDecisionLayer::documentoOriginal)
                .orElse(null);
    }

    private static ProcessoRecursalDecisionSourceDocument sourceDocumentFromDocumento(DocumentoProcessual candidato,
                                                                                       DocumentoPaginaRepository paginaRepository) {
        if (candidato == null) {
            return null;
        }
        List<DocumentoPagina> paginas = paginaRepository == null ? List.of() : paginaRepository.findByDocumentoId(candidato.getId());
        paginas = paginas == null ? List.of() : paginas;
        int totalPaginas = paginas.isEmpty() ? 0 : paginas.size();
        int paginasComTexto = (int) paginas.stream().map(DocumentoPagina::getTextoExtraido).filter(ProcessoRecursalDecisionCarryOverAssembler::hasText).count();
        String textoIntegral = paginasComTexto == 0 ? null : paginas.stream()
                .sorted(Comparator.comparing(DocumentoPagina::getPageNumber))
                .map(DocumentoPagina::getTextoExtraido)
                .filter(ProcessoRecursalDecisionCarryOverAssembler::hasText)
                .map(String::trim)
                .reduce((a, b) -> a + "\n\n" + b)
                .orElse(null);
        String visualizationMode = textoIntegral != null && (candidato.getPdf() != null || hasText(candidato.getStorageUri()))
                ? "PDF_E_TEXTO_INTEGRAL"
                : textoIntegral != null ? "TEXTO_INTEGRAL" : "PDF_ORIGINAL";
        return new ProcessoRecursalDecisionSourceDocument(
                candidato.getId(),
                classifyDocumentType(candidato),
                normalized(candidato.getDocumentoTitulo()),
                normalized(candidato.getNomeOriginal()),
                normalized(candidato.getContentType()),
                candidato.getTamanhoBytes(),
                normalized(candidato.getSha256()),
                normalized(candidato.getSha384()),
                normalized(candidato.getOrigemSistema()),
                normalized(candidato.getStorageBackend()),
                normalized(candidato.getStorageUri()),
                "/api/v1/documentos/" + candidato.getId() + "/pdf",
                visualizationMode,
                totalPaginas == 0 ? null : totalPaginas,
                paginasComTexto == 0 ? null : paginasComTexto,
                textoIntegral,
                candidato.getCriadoEm() == null ? null : candidato.getCriadoEm().toInstant(ZoneOffset.UTC)
        );
    }

    private static boolean looksLikeDecisionDocument(DocumentoProcessual documento) {
        if (documento == null) {
            return false;
        }
        String corpus = normalized(String.join(" ", safe(documento.getDocumentoTitulo()), safe(documento.getNomeOriginal())));
        if (corpus == null) {
            return false;
        }
        String lower = corpus.toLowerCase();
        boolean decisorio = containsAny(lower, "sentenca", "sentença", "acordao", "acórdão", "decisao", "decisão", "despacho", "voto", "ementa");
        if (!decisorio) {
            return false;
        }
        boolean startsDecision = startsWithAny(lower, "sentenca", "sentença", "acordao", "acórdão", "decisao", "decisão", "despacho", "voto", "ementa");
        boolean peticaoRecursal = containsAny(lower, "peticao", "petição", "recurso", "apela", "agravo", "contrarrazo", "contrarrazão", "contrarrazoes", "contrarrazões");
        return startsDecision || !peticaoRecursal;
    }

    private static int nextDegreeForDocument(DocumentoProcessual documento, int currentDegree) {
        String corpus = normalized(String.join(" ", safe(documento.getDocumentoTitulo()), safe(documento.getNomeOriginal())));
        String lower = corpus == null ? "" : corpus.toLowerCase();
        String documentType = classifyDocumentType(documento);
        boolean embargosMesmoGrau = containsAny(lower, "embargos de declaração", "embargos declaracao", "embargos declaratórios", "embargos declaratorios");
        if (currentDegree <= 0) {
            if (documentType.startsWith("SENTENCA")) {
                return 1;
            }
            if (documentType.startsWith("ACORDAO") || documentType.startsWith("DECISAO")) {
                return 2;
            }
            return 1;
        }
        if (embargosMesmoGrau || documentType.startsWith("DESPACHO")) {
            return currentDegree;
        }
        if (documentType.startsWith("SENTENCA")) {
            return Math.max(currentDegree, 1);
        }
        if (documentType.startsWith("ACORDAO") || documentType.startsWith("DECISAO")) {
            return currentDegree + 1;
        }
        return currentDegree;
    }

    private static String stageLabelForDegree(int degree) {
        if (degree <= 1) {
            return "PRIMEIRO_GRAU";
        }
        if (degree == 2) {
            return "SEGUNDO_GRAU";
        }
        return "TRIBUNAL_SUPERIOR_" + (degree - 2);
    }

    private static String classifyDocumentType(DocumentoProcessual documento) {
        String corpus = normalized(String.join(" ", safe(documento.getDocumentoTitulo()), safe(documento.getNomeOriginal())));
        if (corpus == null) {
            return "DECISAO_JUDICIAL_ORIGINAL";
        }
        String lower = corpus.toLowerCase();
        if (containsAny(lower, "sentenca", "sentença")) {
            return "SENTENCA_ORIGINAL";
        }
        if (containsAny(lower, "acordao", "acórdão")) {
            return containsAny(lower, "embargos") ? "ACORDAO_EMBARGOS_ORIGINAL" : "ACORDAO_ORIGINAL";
        }
        if (containsAny(lower, "despacho")) {
            return "DESPACHO_ORIGINAL";
        }
        if (containsAny(lower, "decisao", "decisão")) {
            return containsAny(lower, "embargos") ? "DECISAO_EMBARGADA_ORIGINAL" : "DECISAO_ORIGINAL";
        }
        return "DECISAO_JUDICIAL_ORIGINAL";
    }

    private static void put(Map<String, Object> out, String key, String value) {
        if (value != null && !value.isBlank()) {
            out.put(key, value);
        }
    }

    private static void add(List<String> out, String key, String value) {
        if (value != null && !value.isBlank()) {
            out.add(key + '=' + value);
        }
    }

    private static String inferDecisionType(Processo processo) {
        if (processo == null || processo.getStatusProcesso() == null) {
            return "DECISAO_JUDICIAL";
        }
        return switch (processo.getStatusProcesso()) {
            case EMBARGOS_DECLARACAO -> "DECISAO_EMBARGADA";
            case RECURSO_INTERPOSTO -> "DECISAO_RECORRIDA";
            case SENTENCA_PROFERIDA -> "SENTENCA";
            case CUMPRIMENTO_SENTENCA -> "DECISAO_EXECUTIVA";
            default -> "DECISAO_JUDICIAL";
        };
    }

    private static String inferStage(String scope, StatusProcesso status) {
        String normalizedScope = normalized(scope);
        if ("EMBARGOS_MESMO_GRAU".equals(normalizedScope)) {
            return "MESMO_GRAU";
        }
        if ("INCIDENTE_APARTADO_DEPENDENCIA".equals(normalizedScope)) {
            return "MESMO_GRAU_APARTADO";
        }
        if (status != null && status.isRecursalOuEmbargos()) {
            return status == StatusProcesso.EMBARGOS_DECLARACAO ? "MESMO_GRAU" : "GRAU_SUPERIOR";
        }
        return "GRAU_ORIGEM";
    }

    private static String safeNumeroProcesso(Processo processo) {
        return firstNonBlank(normalized(processo.getNumeroProcesso()), normalized(processo.getNumero()), normalized(processo.getNumeroUnificado()));
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String shorten(String value, int max) {
        String normalized = normalized(value);
        if (normalized == null || normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max);
    }

    private static String normalized(String value) {
        if (value == null) {
            return null;
        }
        String compact = value.trim().replaceAll("\\s+", " ");
        return compact.isBlank() ? null : compact;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean containsAny(String source, String... terms) {
        if (source == null || terms == null) {
            return false;
        }
        for (String term : terms) {
            if (term != null && !term.isBlank() && source.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithAny(String source, String... terms) {
        if (source == null || terms == null) {
            return false;
        }
        String trimmed = source.stripLeading();
        for (String term : terms) {
            if (term != null && !term.isBlank() && trimmed.startsWith(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
