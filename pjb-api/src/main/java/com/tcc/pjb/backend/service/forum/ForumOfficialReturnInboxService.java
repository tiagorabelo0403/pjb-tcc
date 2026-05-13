package com.tcc.pjb.backend.service.forum;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.institutional.topology.InstitutionalActorRoutingService;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ForumOfficialReturnInboxService {

    private final OficialJusticaContextEnvelopeService contextEnvelopeService;

    public ForumOfficialReturnInboxService(OficialJusticaContextEnvelopeService contextEnvelopeService) {
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
    }

    public ReturnEnvelope classify(Processo processo,
                                   Usuario oficial,
                                   WorkItem source,
                                   String reasonCode,
                                   String reasonLabel,
                                   InstitutionalActorRoutingService.InstitutionalRoute route) {
        String esfera = contextEnvelopeService.resolveEsfera(oficial, processo, oficial != null ? oficial.getTipoUsuario() : null);
        String tribunal = contextEnvelopeService.resolveTribunalPrincipal(oficial, processo);
        String cidade = firstNonBlank(processo != null ? processo.getComarca() : null, oficial != null ? oficial.getComarca() : null, "CIDADE_NAO_IDENTIFICADA");
        String vara = firstNonBlank(processo != null ? processo.getVara() : null, "VARA_NAO_IDENTIFICADA");
        String forum = contextEnvelopeService.resolveForum(processo, esfera, cidade);
        String regiao = contextEnvelopeService.resolveRegiaoJudicial(tribunal, firstNonBlank(processo != null ? processo.getUf() : null, oficial != null ? oficial.getUf() : null), esfera);
        Compartment compartment = resolveCompartment(reasonCode, processo, tribunal, esfera);
        String queueCode = firstNonBlank(route != null ? route.queueCode() : null, compartment.queueCode(), "FORUM_OFFICIAL_RETURN_QUEUE");
        String inboxKey = firstNonBlank(route != null ? route.inboxKey() : null, compartment.inboxKey(), "FORUM_OFFICIAL_RETURN_INBOX");
        String unitPartitionKey = normalize(tribunal) + ':' + normalize(forum) + ':' + normalize(vara) + ':' + normalize(compartment.code());
        LinkedHashMap<String, Object> unitContext = new LinkedHashMap<>();
        unitContext.put("processoId", processo != null ? processo.getId() : null);
        unitContext.put("processoNumero", contextEnvelopeService.processNumber(processo));
        unitContext.put("oficialNome", oficial != null ? oficial.getNome() : null);
        unitContext.put("oficialTipo", oficial != null && oficial.getTipoUsuario() != null ? oficial.getTipoUsuario().name() : null);
        unitContext.put("vara", vara);
        unitContext.put("forum", forum);
        unitContext.put("cidade", cidade);
        unitContext.put("uf", firstNonBlank(processo != null ? processo.getUf() : null, oficial != null ? oficial.getUf() : null));
        unitContext.put("tribunal", tribunal);
        unitContext.put("esfera", esfera);
        unitContext.put("regiaoJudicial", regiao);
        unitContext.put("compartimento", compartment.code());
        unitContext.put("compartimentoLabel", compartment.label());
        unitContext.put("filaUnidade", compartment.queueCode());
        unitContext.put("unitPartitionKey", unitPartitionKey);
        unitContext.put("routeAxis", route != null ? route.routeAxis() : null);
        unitContext.put("routeKey", route != null ? route.topologyKey() : null);
        unitContext.put("generatedAt", Instant.now());
        return new ReturnEnvelope(
                compartment.code(),
                compartment.label(),
                queueCode,
                inboxKey,
                compartment.folderCode(),
                unitPartitionKey,
                forum,
                vara,
                cidade,
                tribunal,
                esfera,
                regiao,
                trim(compartment.title() + " — " + contextEnvelopeService.processNumber(processo), 220),
                unitContext.entrySet().stream().filter(entry -> entry.getValue() != null).collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new))
        );
    }

    public String renderSummary(ReturnEnvelope envelope, String baseSummary) {
        StringBuilder sb = new StringBuilder(2400);
        if (envelope != null) {
            sb.append("Compartimento automático do fórum/tribunal: ").append(envelope.compartmentLabel()).append('.').append(' ');
            sb.append("Fila da unidade: ").append(firstNonBlank(envelope.queueCode(), envelope.inboxKey(), "FILA_NAO_IDENTIFICADA")).append('.').append(' ');
            sb.append("Fórum/unidade: ").append(firstNonBlank(envelope.forum(), envelope.tribunal(), "UNIDADE_NAO_IDENTIFICADA")).append('.').append(' ');
            sb.append("Vara: ").append(firstNonBlank(envelope.vara(), "VARA_NAO_IDENTIFICADA")).append('.').append(' ');
            sb.append("Cidade/UF: ").append(firstNonBlank(envelope.cidade(), "CIDADE_NAO_IDENTIFICADA")).append('/').append(firstNonBlank(stringValue(envelope.unitContext().get("uf")), "UF")).append('.').append(' ');
            sb.append("Tribunal: ").append(firstNonBlank(envelope.tribunal(), "TRIBUNAL_NAO_IDENTIFICADO")).append('.').append(' ');
            sb.append("Região judicial: ").append(firstNonBlank(envelope.regiaoJudicial(), "REGIAO_NAO_IDENTIFICADA")).append('.').append(' ');
        }
        if (baseSummary != null && !baseSummary.isBlank()) {
            sb.append(baseSummary.trim());
        }
        return trim(sb.toString(), 1800);
    }

    private Compartment resolveCompartment(String reasonCode, Processo processo, String tribunal, String esfera) {
        String reason = normalize(reasonCode);
        boolean tribunalLane = isTribunalTransition(processo, tribunal, esfera);
        if ("ARQUIVAMENTO_OU_BAIXA".equals(reason)) {
            return new Compartment(
                    tribunalLane ? "TRIBUNAL_ARQUIVAMENTO_CONTROLADO" : "FORUM_ARQUIVAMENTO_CONTROLADO",
                    tribunalLane ? "Tribunal / arquivamento controlado" : "Fórum / arquivamento controlado",
                    tribunalLane ? "TRIBUNAL_ARQUIVAMENTO" : "FORUM_ARQUIVAMENTO",
                    tribunalLane ? "TRIBUNAL:ARQUIVAMENTO" : "FORUM:ARQUIVAMENTO",
                    "ARQUIVAMENTO_CONTROLADO",
                    "Guardar retorno do Oficial e consolidar arquivamento controlado"
            );
        }
        if ("DEMANDA_REDIRECIONADA".equals(reason)) {
            return new Compartment(
                    tribunalLane ? "TRIBUNAL_REDISTRIBUICAO" : "FORUM_REDISTRIBUICAO_UNIDADE",
                    tribunalLane ? "Tribunal / redistribuição de unidade" : "Fórum / redistribuição de unidade",
                    tribunalLane ? "TRIBUNAL_REDISTRIBUICAO" : "FORUM_REDISTRIBUICAO",
                    tribunalLane ? "TRIBUNAL:REDISTRIBUICAO" : "FORUM:REDISTRIBUICAO",
                    "REDISTRIBUICAO_UNIDADE",
                    "Receber retorno do Oficial e redistribuir para a unidade competente"
            );
        }
        return new Compartment(
                tribunalLane ? "TRIBUNAL_PROXIMA_DEMANDA" : "CARTORIO_PROXIMA_DEMANDA",
                tribunalLane ? "Tribunal / próxima demanda" : "Cartório / próxima demanda",
                tribunalLane ? "TRIBUNAL_PROXIMA_DEMANDA" : "CARTORIO_PROXIMA_DEMANDA",
                tribunalLane ? "TRIBUNAL:PROXIMA_DEMANDA" : "CARTORIO:PROXIMA_DEMANDA",
                "PROXIMA_DEMANDA",
                "Receber retorno do Oficial e preparar a próxima demanda"
        );
    }

    private boolean isTribunalTransition(Processo processo, String tribunal, String esfera) {
        String normalizedTribunal = normalize(firstNonBlank(tribunal, processo != null ? processo.getTribunal() : null));
        if (normalizedTribunal.startsWith("TRF") || normalizedTribunal.startsWith("TJ") || normalizedTribunal.startsWith("TRE") || normalizedTribunal.startsWith("TRT") || normalizedTribunal.startsWith("STJ") || normalizedTribunal.startsWith("STF")) {
            return true;
        }
        return normalize(esfera).contains("FEDERAL") && processo != null && processo.getVara() != null && processo.getVara().toUpperCase(Locale.ROOT).contains("TURMA");
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static String trim(String value, int limit) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= Math.max(12, limit)) {
            return normalized;
        }
        return normalized.substring(0, Math.max(12, limit) - 1) + '…';
    }

    public record ReturnEnvelope(
            String compartmentCode,
            String compartmentLabel,
            String queueCode,
            String inboxKey,
            String folderCode,
            String unitPartitionKey,
            String forum,
            String vara,
            String cidade,
            String tribunal,
            String esfera,
            String regiaoJudicial,
            String title,
            Map<String, Object> unitContext
    ) {
    }

    private record Compartment(
            String code,
            String label,
            String queueCode,
            String inboxKey,
            String folderCode,
            String title
    ) {
    }
}
