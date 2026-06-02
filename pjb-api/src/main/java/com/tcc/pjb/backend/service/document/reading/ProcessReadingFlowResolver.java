package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProcessEntryResponse;
import com.tcc.pjb.backend.model.dto.shared.reading.ProcessReadingFlowMetadataDto;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProcessReadingFlowResolver {

    private static final int MAX_PREVIEW = 320;
    private static final int MAX_ENTRIES = 60;

    public ProcessReadingFlowResponse resolve(Processo processo,
                                              Usuario usuario,
                                              List<MovimentacaoProcessual> movimentacoes,
                                              List<EventoProcessual> eventos,
                                              ProcessReadingModeProfile modeProfile,
                                              ProcessReadingPresetProfile presetProfile) {
        ArrayList<ProcessReadingProcessEntryResponse> entries = new ArrayList<>();
        appendProcessInlineEntries(entries, processo, modeProfile, presetProfile);
        appendMovements(entries, processo, movimentacoes, modeProfile, presetProfile);
        appendEvents(entries, processo, eventos, modeProfile, presetProfile);
        List<ProcessReadingProcessEntryResponse> ordered = entries.stream()
                .sorted(Comparator.comparing(ProcessReadingProcessEntryResponse::occurredAt, Comparator.nullsLast(String::compareTo)).reversed()
                        .thenComparing(ProcessReadingProcessEntryResponse::entryId, Comparator.nullsLast(String::compareTo)))
                .limit(MAX_ENTRIES)
                .toList();
        long inlineCount = ordered.stream().filter(entry -> "PROCESS_INLINE_TEXT".equals(entry.sourceType())).count();
        long movementCount = ordered.stream().filter(entry -> "MOVIMENTACAO_PROCESSUAL".equals(entry.sourceType())).count();
        long eventCount = ordered.stream().filter(entry -> "EVENTO_PROCESSUAL".equals(entry.sourceType())).count();
        ProcessReadingFlowMetadataDto metadata = new ProcessReadingFlowMetadataDto(
                resolveCluster(usuario),
                true,
                true,
                true,
                true,
                true,
                true,
                modeProfile.recursal() ? "RECURSAL" : "ATOS",
                presetProfile.focusBandMode(),
                presetProfile.chronologyMode(),
                processo != null && processo.getId() != null ? "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo" : null
        );
        return new ProcessReadingFlowResponse(
                ordered.size(),
                inlineCount,
                movementCount,
                eventCount,
                presetProfile.chronologyMode(),
                modeProfile.recursal() ? "ATOS_E_RECURSOS_EM_LINHA_DO_TEMPO" : "ATOS_PROCESSUAIS_EM_LINHA_DO_TEMPO",
                ordered,
                metadata
        );
    }

    private void appendProcessInlineEntries(List<ProcessReadingProcessEntryResponse> entries,
                                            Processo processo,
                                            ProcessReadingModeProfile modeProfile,
                                            ProcessReadingPresetProfile presetProfile) {
        if (processo == null || processo.getId() == null) {
            return;
        }
        addInline(entries, processo, "RESUMO_PROCESSUAL", "Resumo processual consolidado", processo.getResumoIA(), processo.getDataUltimaMovimentacao(), modeProfile, presetProfile, "ATOS", "medium");
        addInline(entries, processo, "PEDIDOS_CONSOLIDADOS", "Pedidos consolidados", processo.getPedidosConsolidados(), processo.getDataCriacao(), modeProfile, presetProfile, "TEXTO", "medium");
        addInline(entries, processo, "MATERIAL_PROBATORIO", "Material probatório consolidado", processo.getMaterialProbatorioResumo(), processo.getDataUltimaMovimentacao(), modeProfile, presetProfile, "PROVA", "high");
        addInline(entries, processo, "RESULTADO_FINAL", "Resultado final consolidado", processo.getResultadoFinal(), processo.getDataUltimaMovimentacao(), modeProfile, presetProfile, modeProfile.recursal() ? "RECURSAL" : "ATOS", "high");
    }

    private void addInline(List<ProcessReadingProcessEntryResponse> entries,
                           Processo processo,
                           String originMode,
                           String title,
                           String body,
                           LocalDateTime occurredAt,
                           ProcessReadingModeProfile modeProfile,
                           ProcessReadingPresetProfile presetProfile,
                           String lane,
                           String severity) {
        if (blank(body)) {
            return;
        }
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo.getId());
        metadata.put("chronologyMode", presetProfile.chronologyMode());
        metadata.put("anchorMode", presetProfile.anchorMode());
        metadata.put("supportDeskMode", modeProfile.supportDeskMode());
        metadata.put("originMode", originMode);
        String entryId = "INLINE-" + originMode;
        metadata.put("contentEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo?entryId=" + entryId);
        entries.add(new ProcessReadingProcessEntryResponse(
                entryId,
                "PROCESS_INLINE_TEXT",
                originMode,
                title,
                preview(body),
                "SISTEMA",
                iso(occurredAt),
                lane,
                severity,
                false,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo#" + entryId,
                null,
                List.of(originMode, lane, modeProfile.profileCode()),
                metadata
        ));
    }

    private void appendMovements(List<ProcessReadingProcessEntryResponse> entries,
                                 Processo processo,
                                 List<MovimentacaoProcessual> movimentacoes,
                                 ProcessReadingModeProfile modeProfile,
                                 ProcessReadingPresetProfile presetProfile) {
        if (processo == null || processo.getId() == null || movimentacoes == null) {
            return;
        }
        for (MovimentacaoProcessual movimentacao : movimentacoes.stream().limit(36).toList()) {
            if (movimentacao == null || movimentacao.getId() == null) {
                continue;
            }
            String text = firstNonBlank(movimentacao.getDescricao(), phaseBridge(movimentacao));
            if (blank(text)) {
                continue;
            }
            String lane = resolveLane(text, modeProfile);
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("processoId", processo.getId());
            metadata.put("movimentacaoId", movimentacao.getId());
            metadata.put("faseDe", movimentacao.getFaseDe() != null ? movimentacao.getFaseDe().name() : null);
            metadata.put("fasePara", movimentacao.getFasePara() != null ? movimentacao.getFasePara().name() : null);
            metadata.put("supportDeskMode", modeProfile.supportDeskMode());
            metadata.put("operationalOverlayMode", presetProfile.operationalOverlayMode());
            String entryId = "MOV-" + movimentacao.getId();
            metadata.put("contentEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo?entryId=" + entryId);
            entries.add(new ProcessReadingProcessEntryResponse(
                    entryId,
                    "MOVIMENTACAO_PROCESSUAL",
                    inferMovementMode(text, movimentacao.getFasePara()),
                    resolveMovementTitle(text, movimentacao.getFaseDe(), movimentacao.getFasePara()),
                    preview(text),
                    movimentacao.getAtor() != null ? safeName(movimentacao.getAtor().getNome()) : "ATO PROCESSUAL",
                    iso(movimentacao.getDataMovimentacao()),
                    lane,
                    resolveSeverity(text, modeProfile),
                    false,
                    "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo#" + entryId,
                    null,
                    List.of("MOVIMENTACAO", lane, modeProfile.profileCode()),
                    metadata
            ));
        }
    }

    private void appendEvents(List<ProcessReadingProcessEntryResponse> entries,
                              Processo processo,
                              List<EventoProcessual> eventos,
                              ProcessReadingModeProfile modeProfile,
                              ProcessReadingPresetProfile presetProfile) {
        if (processo == null || processo.getId() == null || eventos == null) {
            return;
        }
        for (EventoProcessual evento : eventos.stream().limit(24).toList()) {
            if (evento == null || evento.getId() == null) {
                continue;
            }
            String text = firstNonBlank(evento.getDescricao(), evento.getTitulo());
            if (blank(text)) {
                continue;
            }
            String lane = resolveLane(text, modeProfile);
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("processoId", processo.getId());
            metadata.put("eventoId", evento.getId());
            metadata.put("tipo", evento.getTipo() != null ? evento.getTipo().name() : null);
            metadata.put("status", evento.getStatus() != null ? evento.getStatus().name() : null);
            metadata.put("overlayMode", presetProfile.operationalOverlayMode());
            String entryId = "EVT-" + evento.getId();
            metadata.put("contentEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo?entryId=" + entryId);
            entries.add(new ProcessReadingProcessEntryResponse(
                    entryId,
                    "EVENTO_PROCESSUAL",
                    inferEventMode(evento),
                    firstNonBlank(evento.getTitulo(), "Evento processual"),
                    preview(text),
                    evento.getResponsavel() != null ? safeName(evento.getResponsavel().getNome()) : "RESPONSÁVEL NÃO IDENTIFICADO",
                    iso(evento.getDataInicio()),
                    lane,
                    evento.isAtrasado() ? "high" : "medium",
                    false,
                    "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo#" + entryId,
                    null,
                    tagsForEvent(evento, lane, modeProfile),
                    metadata
            ));
        }
    }

    private static List<String> tagsForEvent(EventoProcessual evento, String lane, ProcessReadingModeProfile modeProfile) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tags.add("EVENTO");
        tags.add(lane);
        tags.add(modeProfile.profileCode());
        if (evento.getTipo() != null) {
            tags.add(evento.getTipo().name());
        }
        if (evento.getStatus() != null) {
            tags.add(evento.getStatus().name());
        }
        return List.copyOf(tags);
    }

    private static String inferMovementMode(String text, FaseProcessual fasePara) {
        String lower = normalize(text);
        if (containsAny(lower, "peticao inicial", "petição inicial", "ajuizamento", "protocolada a inicial")) {
            return "PETICAO_INICIAL_HTML";
        }
        if (containsAny(lower, "contestacao", "contestação", "defesa", "resposta do reu", "resposta do réu")) {
            return "CONTESTACAO_HTML";
        }
        if (containsAny(lower, "despacho", "intime-se", "cumpra-se", "diga a parte")) {
            return "DESPACHO_HTML_NATIVO_ASSINAVEL";
        }
        if (containsAny(lower, "sentenca", "sentença", "julgo procedente", "julgo improcedente", "extingo")) {
            return "SENTENCA_HTML_NATIVA_ASSINAVEL";
        }
        if (containsAny(lower, "decis", "julgo", "defiro", "indefiro", "liminar", "tutela", "saneador")) {
            return "DECISAO_INTERLOCUTORIA_HTML_NATIVA";
        }
        if (containsAny(lower, "acordao", "acórdão", "turma", "câmara", "camara", "sessao de julgamento", "sessão de julgamento")) {
            return "ACORDAO_HTML_NATIVO";
        }
        if (containsAny(lower, "embargos de declara", "embargos declar")) {
            return "EMBARGOS_DECLARACAO_HTML";
        }
        if (containsAny(lower, "agravo", "apela", "contrarrazo", "contrarrazõ", "recurso ordin", "recurso especial", "recurso extraordin")) {
            return "ATO_RECURSAL_HTML";
        }
        if (containsAny(lower, "parecer ministerial", "parecer do mp", "manifestacao ministerial", "manifestação ministerial")) {
            return "PARECER_MINISTERIAL_HTML";
        }
        if (containsAny(lower, "laudo", "pericia", "perícia", "assistente técnico", "assistente tecnico")) {
            return "LAUDO_PERICIAL_HTML";
        }
        if (containsAny(lower, "calculo", "cálculo", "conta de liquida", "memoria de calculo", "memória de cálculo")) {
            return "CALCULO_PROCESSUAL_HTML";
        }
        if (fasePara == FaseProcessual.RECURSAL || containsAny(lower, "recurso", "embargos", "agravo", "apelacao", "apelação")) {
            return "ATO_RECURSAL_HTML";
        }
        return "MOVIMENTACAO_HTML_NATIVA";
    }

    private static String inferEventMode(EventoProcessual evento) {
        String titulo = normalize(firstNonBlank(evento.getTitulo(), ""));
        if (containsAny(titulo, "audiencia", "sessao", "sessão", "julgamento")) {
            return "AGENDA_PROCESSUAL_HTML";
        }
        if (containsAny(titulo, "prazo", "manifestacao", "manifestação")) {
            return "PRAZO_OPERACIONAL_HTML";
        }
        return "EVENTO_INLINE_HTML";
    }

    private static String resolveMovementTitle(String text, FaseProcessual faseDe, FaseProcessual fasePara) {
        String trimmed = preview(text);
        if (!blank(trimmed)) {
            return trimmed.length() <= 96 ? trimmed : trimmed.substring(0, 96).trim();
        }
        if (faseDe != null || fasePara != null) {
            return phaseBridge(faseDe, fasePara);
        }
        return "Movimentação processual";
    }

    private static String phaseBridge(MovimentacaoProcessual movimentacao) {
        return phaseBridge(movimentacao != null ? movimentacao.getFaseDe() : null, movimentacao != null ? movimentacao.getFasePara() : null);
    }

    private static String phaseBridge(FaseProcessual faseDe, FaseProcessual fasePara) {
        if (faseDe == null && fasePara == null) {
            return null;
        }
        return (faseDe != null ? faseDe.name() : "FASE_ATUAL") + " → " + (fasePara != null ? fasePara.name() : "FASE_SEGUINTE");
    }

    private static String resolveLane(String text, ProcessReadingModeProfile modeProfile) {
        String lower = normalize(text);
        if (containsAny(lower, "recurso", "agravo", "apela", "contrarrazo", "embargos", "acord")) {
            return "RECURSAL";
        }
        if (containsAny(lower, "despacho", "decis", "senten", "julgo", "defiro", "indefiro", "peticao inicial", "contestacao", "contestação")) {
            return modeProfile.recursal() ? "RECURSAL" : "ATOS";
        }
        if (containsAny(lower, "laudo", "pericia", "perícia", "prova", "testemunh", "contrato", "extrato", "parecer", "assistente tecnico", "assistente técnico")) {
            return "PROVA";
        }
        if (containsAny(lower, "art.", "tema", "sumula", "súmula", "precedente", "cf/88", "cpc", "cpp", "clt", "lei complementar", "constitui")) {
            return "CITACOES";
        }
        if (containsAny(lower, "prazo", "intime-se", "cumpra-se", "certifico", "conclusos", "junte-se", "manifest", "calculo", "cálculo")) {
            return "EQUIPE";
        }
        return modeProfile.recursal() ? "RECURSAL" : "ATOS";
    }

    private static String resolveSeverity(String text, ProcessReadingModeProfile modeProfile) {
        String lower = normalize(text);
        if (containsAny(lower, "urgente", "liminar", "prazo", "tutela", "hc", "habeas")) {
            return "high";
        }
        if (modeProfile.recursal() || containsAny(lower, "recurso", "acord", "embargos", "agravo", "apelacao", "apelação")) {
            return "medium";
        }
        return "low";
    }

    private static String preview(String value) {
        if (blank(value)) {
            return "";
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= MAX_PREVIEW ? normalized : normalized.substring(0, MAX_PREVIEW) + "...";
    }

    private static String iso(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private static String iso(Instant value) {
        return value != null ? value.atZone(ZoneOffset.UTC).toLocalDateTime().toString() : null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String safeName(String value) {
        return blank(value) ? "USUÁRIO NÃO IDENTIFICADO" : value.trim();
    }

    private static String resolveCluster(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "ANONIMO";
        }
        if (usuario.getTipoUsuario().isMagistratura()) {
            return "MAGISTRATURA";
        }
        if (usuario.getTipoUsuario().isAssessor()) {
            return "ASSESSORIA";
        }
        if (usuario.getTipoUsuario().isServidorJudiciario()) {
            return "SERVIDOR_JUDICIARIO";
        }
        if (usuario.getTipoUsuario().isMinisterioPublico()) {
            return "MINISTERIO_PUBLICO";
        }
        if (usuario.getTipoUsuario().isDefensoriaPublica()) {
            return "DEFENSORIA_PUBLICA";
        }
        if (usuario.getTipoUsuario().isProcuradoria()) {
            return "PROCURADORIA";
        }
        if (usuario.getTipoUsuario().isAdvocacia()) {
            return "ADVOCACIA";
        }
        return usuario.getTipoUsuario().name();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String base, String... needles) {
        if (blank(base) || needles == null) {
            return false;
        }
        for (String needle : needles) {
            if (!blank(needle) && base.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
