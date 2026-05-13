package com.tcc.pjb.backend.service.secretariat.operational;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;


@Component
public class SecretariatOperationalChecklistEngine {

    public ChecklistSnapshot resolve(Processo processo,
                                     SecretariatOperationalRoutingProfile routing,
                                     SecretariatRulePack rulePack) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(routing, "routing");
        Objects.requireNonNull(rulePack, "rulePack");

        List<ChecklistItem> items = new ArrayList<>();
        items.add(item("RECEBIMENTO", "UNIDADE_COMPETENTE", "Confirmar unidade judiciária, competência territorial e secretaria calculada", true,
                isBlank(processo.getTribunal()) && isBlank(processo.getTribunalCodigoRoteado()),
                status(!(isBlank(processo.getUf()) || isBlank(processo.getComarca()) || isBlank(routing.secretariatCode()))),
                "A malha nacional precisa fechar justiça, cobertura municipal, unidade e secretaria antes de qualquer impulso."));
        items.add(item("RECEBIMENTO", "PARTES_MINIMAS", "Conferir identificação mínima das partes e polos processuais", true,
                isBlank(processo.getParteAutoraNome()) && isBlank(processo.getParteReuNome()),
                status(!(isBlank(processo.getParteAutoraNome()) && isBlank(processo.getParteReuNome()))),
                "Recebimento cartorário não deve seguir sem referência mínima de polo ativo ou passivo."));
        items.add(item("RECEBIMENTO", "CLASSE_RITO", "Validar coerência entre classe processual, rito e lane da secretaria", true,
                isBlank(processo.getClasseProcessual()),
                status(!isBlank(processo.getClasseProcessual())),
                "O PJB isola rito e lane para impedir mistura entre comum, juizado, penal, previdenciário, família e demais fluxos."));
        items.add(item("SANEAMENTO", "CHECKLIST_BASE", "Executar checklist estrutural do ramo, fase e regime", true,
                false,
                status(!routing.checklist().isEmpty()),
                "A secretaria trabalha por checklist institucional e não por planilha paralela."));
        items.add(item("SANEAMENTO", "TEMPLATES", "Confirmar minuta e templates compatíveis com o ramo da secretaria", false,
                false,
                status(rulePack.templatesDisponiveis() != null && !rulePack.templatesDisponiveis().isEmpty()),
                "O rule pack do ramo precisa sustentar expedição, juntada e despacho padronizados."));
        items.add(item("SANEAMENTO", "SIGILO", "Aplicar célula de sigilo e fila reforçada quando cabível", routing.secrecyAware(),
                routing.secrecyAware() && processo.getNivelSigilo() == null,
                status(!routing.secrecyAware() || processo.getNivelSigilo() != null),
                "Fluxos sigilosos devem ser segregados por célula, agenda, audiência e controle de acesso."));
        items.add(item("PAUTA", "SALA_E_RECURSO", "Reservar sala física/virtual e suporte operacional da audiência", shouldPrepareHearing(processo, routing),
                false,
                status(routing.supportsPhysicalRoom() || routing.supportsVirtualRoom()),
                "Pauta não deve depender de Excel para controle de sala, recurso e janela de audiência."));
        items.add(item("PAUTA", "CONCILIACAO", "Verificar trilha autocompositiva e confirmação de participantes", routing.conciliationPreferred(),
                false,
                status(!routing.conciliationPreferred() || rulePack.admiteFluxoConciliatorio()),
                "Conciliatório preferencial exige confirmação de partes, conciliador e recursos da secretaria."));
        items.add(item("EXECUCAO", "ATOS_SERIADOS", "Planejar atos cartorários seriados por fase e prioridade", true,
                false,
                status(processo.getFaseAtual() != null),
                "A secretaria precisa operar fila industrial de atos, com ordem causal e SLA."));
        items.add(item("EXECUCAO", "ATUACAO_MP", "Confirmar atuação obrigatória do Ministério Público quando exigida", rulePack.exigeAtuacaoMinisterioPublico(),
                false,
                status(!rulePack.exigeAtuacaoMinisterioPublico() || requiresPublicInstitutionMarker(processo)),
                "Matérias com atuação obrigatória do MP não devem avançar sem checkpoint institucional."));
        items.add(item("DISTRIBUICAO_INTERNA", "CELULA_E_SERVIDOR", "Distribuir item para célula e servidor compatíveis com lane, sigilo e carga", true,
                false,
                ChecklistStatus.PENDENTE,
                "Distribuição interna elimina planilhas de carga e garante mesa certa dentro da secretaria."));
        items.add(item("SLA", "MONITORAMENTO", "Monitorar atraso, risco de fila e necessidade de escalonamento", true,
                false,
                ChecklistStatus.PENDENTE,
                "Toda fila da secretaria deve nascer com relógio de SLA, banda de pressão e gatilho de escalonamento."));

        List<String> blockers = items.stream().filter(v -> v.blocking() && v.status() != ChecklistStatus.OK).map(ChecklistItem::label).toList();
        LinkedHashSet<String> categories = new LinkedHashSet<>();
        for (ChecklistItem item : items) {
            categories.add(item.category());
        }
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalItems", items.size());
        metrics.put("blockingItems", items.stream().filter(ChecklistItem::blocking).count());
        metrics.put("pendingItems", items.stream().filter(v -> v.status() == ChecklistStatus.PENDENTE).count());
        metrics.put("attentionItems", items.stream().filter(v -> v.status() == ChecklistStatus.ATENCAO).count());
        metrics.put("okItems", items.stream().filter(v -> v.status() == ChecklistStatus.OK).count());
        metrics.put("categories", List.copyOf(categories));
        metrics.put("routeKey", routing.routeKey());
        metrics.put("secretariatCode", routing.secretariatCode());
        metrics.put("processPhase", processo.getFaseAtual() == null ? null : processo.getFaseAtual().name());
        metrics.put("class", processo.getClasseProcessual());
        metrics.put("rito", processo.getRito() == null ? null : processo.getRito().name());
        metrics.put("sigilo", processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
        metrics.entrySet().removeIf(entry -> entry.getValue() == null);

        List<String> fundamentos = new ArrayList<>();
        fundamentos.add("Checklist operacional gerado por classe, rito, fase, sigilo e topologia real da secretaria.");
        fundamentos.add("A fila interna da secretaria deve refletir recebimento, saneamento, pauta, atos seriados e escalonamento sem uso de Excel.");
        if (routing.secrecyAware()) {
            fundamentos.add("Fluxo marcado para célula de sigilo reforçado.");
        }
        if (routing.conciliationPreferred()) {
            fundamentos.add("Rota com preferência conciliatória e preparação própria de audiência.");
        }
        if (processo.getFaseAtual() != null) {
            fundamentos.add("Fase atual do processo: " + processo.getFaseAtual().name() + '.');
        }
        return new ChecklistSnapshot(List.copyOf(items), List.copyOf(blockers), List.copyOf(fundamentos), Map.copyOf(metrics));
    }

    private ChecklistItem item(String category,
                               String code,
                               String label,
                               boolean required,
                               boolean blocking,
                               ChecklistStatus status,
                               String rationale) {
        return new ChecklistItem(category, normalizeToken(code), label, required, blocking, status == null ? ChecklistStatus.PENDENTE : status, rationale);
    }

    private ChecklistStatus status(boolean ready) {
        return ready ? ChecklistStatus.OK : ChecklistStatus.PENDENTE;
    }

    private boolean shouldPrepareHearing(Processo processo, SecretariatOperationalRoutingProfile routing) {
        FaseProcessual fase = processo.getFaseAtual();
        if (routing.conciliationPreferred()) {
            return true;
        }
        if (fase == null) {
            return true;
        }
        String token = fase.name().toUpperCase(Locale.ROOT);
        return token.contains("AUD") || token.contains("INSTRU") || token.contains("CONCILI") || token.contains("SENTENC");
    }

    private boolean requiresPublicInstitutionMarker(Processo processo) {
        if (processo.getRamoDireito() == null) {
            return false;
        }
        return processo.getRamoDireito().exigeAtuacaoMP() || processo.getNivelSigilo() == NivelSigilo.SEGREDO_JUSTICA;
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "ITEM";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_").replaceAll("(^_|_$)", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ChecklistSnapshot(
            List<ChecklistItem> items,
            List<String> blockers,
            List<String> fundamentos,
            Map<String, Object> metrics
    ) {
    }

    public record ChecklistItem(
            String category,
            String code,
            String label,
            boolean required,
            boolean blocking,
            ChecklistStatus status,
            String rationale
    ) {
    }

    public enum ChecklistStatus {
        OK,
        ATENCAO,
        PENDENTE
    }
}
