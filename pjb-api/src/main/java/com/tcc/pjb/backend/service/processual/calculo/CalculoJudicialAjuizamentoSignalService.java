package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAjuizamentoSignalRequest;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialAjuizamentoSignalResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialAjuizamentoSignalService {

    private final CalculoJudicialProfileResolverService profileResolverService;
    private final CalculoJudicialFrontendContractService frontendContractService;
    private final CalculoJudicialEconomicReferenceService economicReferenceService;
    private final CalculoJudicialAgentMeshService agentMeshService;

    public CalculoJudicialAjuizamentoSignalService(CalculoJudicialProfileResolverService profileResolverService,
                                                   CalculoJudicialFrontendContractService frontendContractService,
                                                   CalculoJudicialEconomicReferenceService economicReferenceService,
                                                   CalculoJudicialAgentMeshService agentMeshService) {
        this.profileResolverService = Objects.requireNonNull(profileResolverService);
        this.frontendContractService = Objects.requireNonNull(frontendContractService);
        this.economicReferenceService = Objects.requireNonNull(economicReferenceService);
        this.agentMeshService = Objects.requireNonNull(agentMeshService);
    }

    public CalculoJudicialAjuizamentoSignalResponse analisar(CalculoJudicialAjuizamentoSignalRequest request, Authentication authentication) {
        CalculoJudicialSolicitantePerfil perfil = profileResolverService.resolve(authentication, null);
        CompletableFuture<Map<String, Object>> necessidade = agentMeshService.submitMap(() -> necessityAgent(request));
        CompletableFuture<Map<String, Object>> dominio = agentMeshService.submitMap(() -> domainAgent(request));
        CompletableFuture<Map<String, Object>> monetario = agentMeshService.submitMap(() -> monetaryAgent(request));
        CompletableFuture<Map<String, Object>> riscos = agentMeshService.submitMap(() -> feesAndPenaltiesAgent(request));

        agentMeshService.awaitAll(necessidade, dominio, monetario, riscos);

        Map<String, Object> necessidadeResult = necessidade.getNow(Map.of());
        Map<String, Object> dominioResult = dominio.getNow(Map.of());
        Map<String, Object> monetarioResult = monetario.getNow(Map.of());
        Map<String, Object> riscosResult = riscos.getNow(Map.of());

        boolean requerCalculo = Boolean.TRUE.equals(necessidadeResult.get("requerCalculo"));
        String dominioSugerido = stringValue(dominioResult.get("dominioSugerido"));
        List<String> bloqueios = mergeLists((List<String>) dominioResult.get("bloqueios"), (List<String>) monetarioResult.get("bloqueios"));
        List<String> recomendacoes = mergeLists((List<String>) necessidadeResult.get("recomendacoes"), (List<String>) monetarioResult.get("recomendacoes"), (List<String>) riscosResult.get("recomendacoes"));
        List<Map<String, Object>> mensagensTemporarias = temporaryMessages(requerCalculo, dominioSugerido, necessidadeResult, monetarioResult, riscosResult, bloqueios);
        String status = !bloqueios.isEmpty() ? "BLOCKED" : requerCalculo ? "READY" : "IDLE";

        Map<String, Object> routes = new LinkedHashMap<>();
        routes.put("workspace", CalculoJudicialDomainSupport.workspaceRoute());
        routes.put("financialAiExecute", CalculoJudicialDomainSupport.financialAiExecuteRoute());
        routes.put("officialTables", CalculoJudicialDomainSupport.officialTablesRoute());
        routes.put("economicReferences", CalculoJudicialDomainSupport.economicReferencesRoute());
        routes.put("liveAjuizamentoAssist", CalculoJudicialDomainSupport.financialAiLiveFilingRoute());
        if (dominioSugerido != null && !dominioSugerido.isBlank()) {
            routes.put("bootstrap", CalculoJudicialDomainSupport.bootstrapRoute(dominioSugerido));
            routes.put("calculatorJson", CalculoJudicialDomainSupport.jsonRoute(dominioSugerido));
            routes.put("calculatorPdf", CalculoJudicialDomainSupport.pdfRoute(dominioSugerido));
            routes.put("financialAiPreset", Map.of(
                    "dominio", dominioSugerido,
                    "executionProfile", "live_petitioning_2026",
                    "payload", request == null || request.payloadCalculo() == null ? Map.of() : request.payloadCalculo()
            ));
        }

        Map<String, Object> agentResults = new LinkedHashMap<>();
        agentResults.put("necessidadeCalculo", necessidadeResult);
        agentResults.put("roteamentoDominio", dominioResult);
        agentResults.put("monetario", monetarioResult);
        agentResults.put("multasHonorarios", riscosResult);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("executionModel", "concurrent_specialists_virtual_threads");
        metadata.put("agentsExecuted", List.of("necessidade_calculo", "roteamento_dominio", "valor_causa", "multas_honorarios"));
        metadata.put("financialAiVisibleForAllUsers", Boolean.TRUE);
        metadata.put("deliveryMode", "temporary_messages");
        metadata.put("messageTtlMs", 15000);
        metadata.put("panelCode", "PAINEL_IA_FINANCEIRA");
        metadata.put("profileCapabilities", frontendContractService.profileCapabilities(perfil));
        metadata.put("knowledgeBase", frontendContractService.financialKnowledgeBase());
        metadata.put("agentMesh", agentMeshService.meshDescriptor());
        metadata.put("frontendMeta", Map.of(
                "financialAiExecuteRoute", CalculoJudicialDomainSupport.financialAiExecuteRoute(),
                "liveAjuizamentoAssistRoute", CalculoJudicialDomainSupport.financialAiLiveFilingRoute(),
                "financialAiPanelVisible", Boolean.TRUE
        ));
        metadata.put("requestHasPayloadCalculo", request != null && request.payloadCalculo() != null && !request.payloadCalculo().isEmpty());

        return new CalculoJudicialAjuizamentoSignalResponse(
                status,
                requerCalculo,
                dominioSugerido,
                List.copyOf(mensagensTemporarias),
                List.copyOf(recomendacoes),
                List.copyOf(bloqueios),
                Map.copyOf(routes),
                economicReferenceService.panelSnapshot(),
                Map.copyOf(agentResults),
                safeMetadata(metadata),
                Instant.now()
        );
    }

    private Map<String, Object> necessityAgent(CalculoJudicialAjuizamentoSignalRequest request) {
        String text = normalizedText(request == null ? null : request.textoPeticao());
        boolean hasMoney = positive(request == null ? null : request.valorPedidosSomados()) || positive(request == null ? null : request.valorLiquidoPretendido()) || positive(request == null ? null : request.honorariosInformados()) || positive(request == null ? null : request.multasInformadas());
        boolean requires = hasMoney || containsAny(text, "calculo", "cálculo", "liquidacao", "liquidação", "atrasados", "fgts", "horas extras", "selic", "custas", "preparo", "guia", "honorarios", "honorários", "multa", "valor da causa", "rmi", "beneficio", "benefício", "renda mensal");
        List<String> recomendacoes = new ArrayList<>();
        if (requires) {
            recomendacoes.add("A petição contém elementos econômicos relevantes e já pode usar a calculadora ou a IA financeira do PJB.");
        }
        if (request != null && Boolean.TRUE.equals(request.possuiPedidosVincendos())) {
            recomendacoes.add("Há indício de parcelas vincendas; revisar se a memória deve separar vencidas e vincendas.");
        }
        return Map.of(
                "requerCalculo", requires,
                "sinais", List.of(
                        "tem_valores_monetarios=" + hasMoney,
                        "tem_texto_economico=" + requires
                ),
                "recomendacoes", List.copyOf(recomendacoes)
        );
    }

    private Map<String, Object> domainAgent(CalculoJudicialAjuizamentoSignalRequest request) {
        String preferred = request == null ? null : request.dominioPreferencial();
        if (preferred != null && !preferred.isBlank() && CalculoJudicialDomainSupport.isSupported(preferred)) {
            return Map.of(
                    "dominioSugerido", CalculoJudicialDomainSupport.requireSupported(preferred),
                    "candidatos", List.of(CalculoJudicialDomainSupport.requireSupported(preferred)),
                    "bloqueios", List.of()
            );
        }
        String text = normalizedText(request == null ? null : request.textoPeticao());
        String dominio = null;
        if (containsAny(text, "clt", "rescis", "fgts", "hora extra", "insalubridade", "periculosidade")) {
            dominio = "TRABALHISTA_CLT";
        } else if (containsAny(text, "beneficio", "benefício", "inss", "rmi", "dib", "dip", "rpv", "precat")) {
            dominio = "FEDERAL_PREVIDENCIARIO_CJF";
        } else if (containsAny(text, "custas", "preparo", "guia", "porte", "diligencia", "diligência")) {
            dominio = "CUSTAS_PROCESSUAIS";
        } else if (containsAny(text, "tribut", "selic", "mora", "debito fiscal", "débito fiscal", "encargo legal")) {
            dominio = "FAZENDA_TRIBUTARIO";
        }
        List<String> bloqueios = new ArrayList<>();
        if (dominio == null && (positive(request == null ? null : request.valorPedidosSomados()) || positive(request == null ? null : request.multasInformadas()) || positive(request == null ? null : request.honorariosInformados()))) {
            bloqueios.add("Há material econômico na petição, mas o domínio do cálculo ainda não ficou suficientemente claro.");
        }
        return Map.of(
                "dominioSugerido", dominio == null ? "" : dominio,
                "candidatos", dominio == null ? List.of() : List.of(dominio),
                "bloqueios", List.copyOf(bloqueios)
        );
    }

    private Map<String, Object> monetaryAgent(CalculoJudicialAjuizamentoSignalRequest request) {
        List<String> recomendacoes = new ArrayList<>();
        List<String> bloqueios = new ArrayList<>();
        BigDecimal valorDaCausa = request == null ? null : request.valorDaCausaInformado();
        BigDecimal valorPedidos = request == null ? null : request.valorPedidosSomados();
        if (valorDaCausa == null && positive(valorPedidos)) {
            recomendacoes.add("Existe soma econômica do pedido sem valor da causa informado; sugerir uso da calculadora para sustentar o valor da causa.");
        }
        if (positive(valorDaCausa) && positive(valorPedidos)) {
            BigDecimal diff = valorDaCausa.subtract(valorPedidos).abs();
            BigDecimal ratio = valorPedidos.signum() == 0 ? BigDecimal.ZERO : diff.divide(valorPedidos, 4, java.math.RoundingMode.HALF_UP);
            if (ratio.compareTo(new BigDecimal("0.10")) > 0) {
                recomendacoes.add("O valor da causa informado se distancia mais de 10% da soma econômica indicada; conferir a base do pedido.");
            }
        }
        if (request != null && request.quantidadePedidos() != null && request.quantidadePedidos() > 0 && !positive(valorPedidos) && !positive(valorDaCausa)) {
            recomendacoes.add("Há múltiplos pedidos sem massa econômica informada; sugerir cálculo orientado antes do protocolo.");
        }
        if (request != null && Boolean.TRUE.equals(request.possuiCalculosAnexos()) && request.payloadCalculo() == null) {
            recomendacoes.add("Há indicação de cálculo anexo; permitir sincronização com a IA financeira para validar base, multas, honorários e valor da causa.");
        }
        return Map.of(
                "recomendacoes", List.copyOf(recomendacoes),
                "bloqueios", List.copyOf(bloqueios)
        );
    }

    private Map<String, Object> feesAndPenaltiesAgent(CalculoJudicialAjuizamentoSignalRequest request) {
        String text = normalizedText(request == null ? null : request.textoPeticao());
        List<String> recomendacoes = new ArrayList<>();
        if (positive(request == null ? null : request.honorariosInformados()) || containsAny(text, "honorarios", "honorários", "sucumb")) {
            recomendacoes.add("Há sinal de honorários na petição; a IA financeira pode revisar base de incidência e coerência do percentual.");
        }
        if (positive(request == null ? null : request.multasInformadas()) || containsAny(text, "multa", "467", "477", "mora", "clausula penal", "cláusula penal")) {
            recomendacoes.add("Há sinal de multas na petição; validar se a memória separa multa principal, multa acessória e eventual limitação legal.");
        }
        return Map.of("recomendacoes", List.copyOf(recomendacoes));
    }

    private List<Map<String, Object>> temporaryMessages(boolean requerCalculo,
                                                        String dominioSugerido,
                                                        Map<String, Object> necessidadeResult,
                                                        Map<String, Object> monetarioResult,
                                                        Map<String, Object> riscosResult,
                                                        List<String> bloqueios) {
        List<Map<String, Object>> messages = new ArrayList<>();
        if (requerCalculo) {
            messages.add(message("info", "Ação econômica detectada", "O PJB detectou que a petição contém material econômico e já pode abrir a calculadora ou a IA financeira.", 12000));
        }
        if (dominioSugerido != null && !dominioSugerido.isBlank()) {
            messages.add(message("success", "Domínio sugerido", "A análise viva sugere " + CalculoJudicialDomainSupport.aba(dominioSugerido) + " como domínio principal do cálculo.", 12000));
        }
        for (String item : listOfStrings(monetarioResult.get("recomendacoes"))) {
            messages.add(message("warning", "Valor da causa e base econômica", item, 15000));
        }
        for (String item : listOfStrings(riscosResult.get("recomendacoes"))) {
            messages.add(message("info", "Multas e honorários", item, 15000));
        }
        for (String item : bloqueios) {
            messages.add(message("danger", "Bloqueio para automação", item, 15000));
        }
        return messages;
    }

    private Map<String, Object> message(String severity, String title, String content, int ttlMs) {
        return Map.of(
                "severity", severity,
                "title", title,
                "message", content,
                "ttlMs", ttlMs,
                "ephemeral", Boolean.TRUE
        );
    }

    @SafeVarargs
    private List<String> mergeLists(List<String>... lists) {
        List<String> merged = new ArrayList<>();
        if (lists == null) {
            return merged;
        }
        for (List<String> list : lists) {
            if (list == null) {
                continue;
            }
            for (String item : list) {
                if (item != null && !item.isBlank() && !merged.contains(item)) {
                    merged.add(item);
                }
            }
        }
        return merged;
    }

    private List<String> listOfStrings(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(s -> !s.isBlank()).toList();
        }
        return List.of();
    }

    private String normalizedText(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String text, String... keys) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String key : keys) {
            if (text.contains(key.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? "" : text;
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        Map<String, Object> safe = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safe.entrySet().removeIf(entry -> entry.getValue() == null);
        return Map.copyOf(safe);
    }
}
