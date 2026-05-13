package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;


@Service
public class SecretariatOperationalActLineService {

    public ActLineSnapshot planejar(Processo processo,
                                    SecretariatOperationalRoutingProfile routing,
                                    SecretariatOperationalChecklistEngine.ChecklistSnapshot checklist) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(routing, "routing");
        Objects.requireNonNull(checklist, "checklist");
        List<ActLineItem> items = new ArrayList<>();
        items.add(item("RECEBIMENTO", "AUTUAR_E_CONFERIR", "Autuar, conferir integridade mínima e consolidar inbox da secretaria", 1, true,
                routing.receiptQueueCode(), routing.receiptInboxKey(), rationaleBase(processo, routing)));
        items.add(item("SANEAMENTO", "ORGANIZAR_PENDENCIAS", "Identificar pendências saneáveis, documentos faltantes e célula competente", 2, true,
                routing.saneamentoQueueCode(), routing.saneamentoInboxKey(), "Saneamento orientado por checklist institucional e isolamento de rito."));
        if (routing.conciliationPreferred()) {
            items.add(item("AUDIENCIA", "CONFIRMAR_CONCILIACAO", "Confirmar partes, conciliador, recurso virtual e pauta", 3, false,
                    routing.audienceQueueCode(), routing.audienceInboxKey(), "Fluxo conciliatório preferencial do ramo/regime."));
        }
        if (shouldPrepareHearing(processo)) {
            items.add(item("AUDIENCIA", "PREPARAR_SALA_E_INTIMACOES", "Preparar sala, recurso, lista de presença e janelas de audiência", 4, false,
                    routing.audienceQueueCode(), routing.audienceInboxKey(), "A pauta precisa nascer com recurso, confirmação e fila própria da secretaria."));
        }
        items.add(item("EXECUCAO", "EXPEDIR_ATOS_SERIADOS", "Expedir atos cartorários seriados em lote controlado", 5, false,
                routing.executionQueueCode(), routing.executionInboxKey(), "Linha industrial de atos evita planilha paralela e preserva ordem causal."));
        if (routing.secrecyAware()) {
            items.add(item("SIGILO", "CONTROLAR_ACESSO_E_RECURSO", "Aplicar restrição de célula, sala e visibilidade institucional", 1, true,
                    routing.saneamentoQueueCode(), routing.saneamentoInboxKey(), "Processos sigilosos exigem trilha segregada."));
        }
        if (!checklist.blockers().isEmpty()) {
            items.add(item("ESCALONAMENTO", "RESOLVER_BLOQUEIOS", "Escalonar bloqueios operacionais antes de avançar a linha", 1, true,
                    routing.saneamentoQueueCode(), routing.saneamentoInboxKey(), "O checklist apontou bloqueios que impedem avanço automático."));
        }
        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Linha industrial de atos construída por fase, checklist, secretaria e fila real do processo.");
        fundamentos.add("A sequência evita mistura entre recebimento, saneamento, pauta, sigilo e execução.");
        fundamentos.add("Secretaria alvo: " + routing.secretariatCode() + '.');
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalActs", items.size());
        metrics.put("blockingActs", items.stream().filter(ActLineItem::blocking).count());
        metrics.put("secretariatCode", routing.secretariatCode());
        metrics.put("routeKey", routing.routeKey());
        metrics.put("processPhase", processo.getFaseAtual() == null ? null : processo.getFaseAtual().name());
        metrics.entrySet().removeIf(entry -> entry.getValue() == null);
        return new ActLineSnapshot(List.copyOf(items), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    private ActLineItem item(String stage,
                             String code,
                             String label,
                             int sequence,
                             boolean blocking,
                             String queueCode,
                             String inboxKey,
                             String rationale) {
        return new ActLineItem(normalize(stage), normalize(code), label, sequence, blocking, queueCode, inboxKey, rationale);
    }

    private boolean shouldPrepareHearing(Processo processo) {
        FaseProcessual fase = processo.getFaseAtual();
        if (fase == null) {
            return true;
        }
        String token = fase.name().toUpperCase(Locale.ROOT);
        return token.contains("AUD") || token.contains("INSTRU") || token.contains("CONCILI") || token.contains("SENTENC");
    }

    private String rationaleBase(Processo processo, SecretariatOperationalRoutingProfile routing) {
        return "Recebimento territorial e material alinhado à secretaria " + routing.secretariatCode() + " para o processo " + processo.getId() + '.';
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "ITEM";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    public record ActLineSnapshot(
            List<ActLineItem> acts,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record ActLineItem(
            String stage,
            String code,
            String label,
            int sequence,
            boolean blocking,
            String queueCode,
            String inboxKey,
            String rationale
    ) {
    }
}
