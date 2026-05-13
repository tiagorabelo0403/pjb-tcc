package com.tcc.pjb.backend.service.secretariat.oficial;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.oficial_justica.OficialJusticaContextEnvelopeService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInboxAccessService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class SecretariatOfficialActsDrawerService {

    private static final String TEMPLATE_PREFIX = "SECRETARIA:GAVETA_OFICIO_OFICIAL:";
    private static final String DRAWER_KEY_MARKER = "PJB_GAVETA_CHAVE[[";
    private static final String DRAWER_LABEL_MARKER = "PJB_GAVETA_ROTULO[[";
    private static final String DRAWER_FACE_MARKER = "PJB_GAVETA_FACE[[";
    private static final String DRAWER_LOTACAO_MARKER = "PJB_GAVETA_LOTACAO[[";
    private static final String DRAWER_DETAIL_MARKER = "PJB_GAVETA_DETALHE[[";
    private static final String MARKER_END = "]]";
    private static final List<WorkItemStatus> DRAWER_STATUSES = List.of(WorkItemStatus.CONCLUIDO, WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);

    private final WorkItemRepository workItemRepository;
    private final SecretariatInboxAccessService inboxAccessService;
    private final OficialJusticaContextEnvelopeService contextEnvelopeService;
    private final CurrentUserService currentUserService;
    private final PjbTimeService timeService;

    public SecretariatOfficialActsDrawerService(WorkItemRepository workItemRepository,
                                                SecretariatInboxAccessService inboxAccessService,
                                                OficialJusticaContextEnvelopeService contextEnvelopeService,
                                                CurrentUserService currentUserService,
                                                PjbTimeService timeService) {
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.inboxAccessService = Objects.requireNonNull(inboxAccessService);
        this.contextEnvelopeService = Objects.requireNonNull(contextEnvelopeService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> previewReservation(WorkItem desk, Usuario official, String actName) {
        DrawerReservation reservation = resolveReservation(desk, official, actName);
        return reservationMap(reservation, null, null);
    }

    @Transactional
    public Map<String, Object> registerMaterializedAct(WorkItem desk,
                                                       Usuario official,
                                                       String actName,
                                                       Map<String, Object> operation,
                                                       String observation) {
        WorkItem safeDesk = Objects.requireNonNull(desk, "desk_requerido");
        Processo processo = Objects.requireNonNull(safeDesk.getProcesso(), "processo_requerido");
        DrawerReservation reservation = resolveReservation(safeDesk, official, actName);
        String templateCode = TEMPLATE_PREFIX + sourceToken(safeDesk) + ':' + normalizeToken(actName, "ATO");
        WorkItem archive = workItemRepository.findLatestByProcessoIdAndTemplateCode(processo.getId(), templateCode)
                .orElseGet(() -> WorkItem.builder().processo(processo).templateCode(templateCode).build());
        archive.setFaseOrigem(processo.getFaseAtual());
        archive.setType(WorkItemType.CERTIDAO);
        archive.setTitulo(buildArchiveTitle(processo, reservation, actName));
        archive.setDescricao(buildArchiveDescription(processo, safeDesk, official, reservation, actName, operation, observation));
        archive.setQueueCode(firstNonBlank(safeDesk.getQueueCode(), reservation.queueCode()));
        archive.setInboxKey(firstNonBlank(safeDesk.getInboxKey(), reservation.inboxKey()));
        archive.setAssignedRole(TipoUsuario.SERVIDOR_FORUM);
        archive.setAssignedUser(null);
        archive.setStatus(WorkItemStatus.CONCLUIDO);
        archive.setPrioridade(3);
        archive.setBlocking(false);
        archive.setDueAt(null);
        archive.setUf(firstNonBlank(processo.getUf(), safeDesk.getUf()));
        archive.setComarca(firstNonBlank(processo.getComarca(), safeDesk.getComarca()));
        archive.setBaseLegal(buildArchiveBaseLegal(reservation, actName, operation, observation));
        archive.setSemInteresse(false);
        WorkItem saved = workItemRepository.save(archive);
        return reservationMap(reservation, saved, operation);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> drawers(String inboxKey, int limit) {
        String normalizedInbox = inboxAccessService.requireAccess(inboxKey);
        int safeLimit = Math.max(1, Math.min(limit, 80));
        List<WorkItem> items = workItemRepository.findSecretariatOfficialDrawerItemsByInbox(normalizedInbox, DRAWER_STATUSES, PageRequest.of(0, safeLimit * 4));
        LinkedHashMap<String, DrawerAggregate> grouped = new LinkedHashMap<>();
        for (WorkItem item : items) {
            String drawerKey = extractMarker(item, DRAWER_KEY_MARKER);
            if (drawerKey == null) {
                continue;
            }
            DrawerAggregate aggregate = grouped.computeIfAbsent(drawerKey, key -> new DrawerAggregate(
                    key,
                    firstNonBlank(extractMarker(item, DRAWER_LABEL_MARKER), "Gaveta operacional"),
                    firstNonBlank(extractMarker(item, DRAWER_FACE_MARKER), "SECRETARIA_DO_FORUM"),
                    firstNonBlank(extractMarker(item, DRAWER_LOTACAO_MARKER), "COMARCA"),
                    firstNonBlank(extractMarker(item, DRAWER_DETAIL_MARKER), "Sem detalhamento")));
            aggregate.accept(item);
        }
        List<Map<String, Object>> rows = grouped.values().stream().limit(safeLimit).map(aggregate -> {
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("drawerKey", aggregate.drawerKey());
            row.put("rotulo", aggregate.label());
            row.put("faceSecretaria", aggregate.face());
            row.put("lotacaoTipo", aggregate.lotacaoTipo());
            row.put("detalhamento", aggregate.detail());
            row.put("itens", aggregate.count());
            row.put("processosDistintos", aggregate.processos().size());
            row.put("ultimaAtualizacao", aggregate.latestUpdate());
            row.put("ultimoProcessoNumero", aggregate.latestProcessNumber());
            row.put("gavetaDetalhePath", OperationalApiRoutes.secretariatOperationalOfficialClosureDrawerDetail(normalizedInbox, aggregate.drawerKey()));
            return safeCopy(row);
        }).toList();
        LinkedHashMap<String, Object> operador = new LinkedHashMap<>();
        Usuario current = currentUserService.getRequired();
        operador.put("usuarioId", current.getId());
        operador.put("usuarioNome", firstNonBlank(current.getNome(), "USUARIO_NAO_IDENTIFICADO"));
        operador.put("perfil", current.getTipoUsuario() != null ? current.getTipoUsuario().name() : "SERVIDOR_FORUM");
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "GAVETAS_SECRETARIA_OFICIAL_CARREGADAS");
        out.put("generatedAt", timeService.nowUtc());
        out.put("inboxKey", normalizedInbox);
        out.put("usuarioOperador", safeCopy(operador));
        out.put("totalGavetas", rows.size());
        out.put("gavetas", rows);
        return safeCopy(out);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> drawerDetail(String inboxKey, String drawerKey, int limit) {
        String normalizedInbox = inboxAccessService.requireAccess(inboxKey);
        String normalizedDrawerKey = validateDrawerKey(drawerKey);

        int safeLimit = Math.max(1, Math.min(limit, 120));
        List<WorkItem> items = workItemRepository.findSecretariatOfficialDrawerItemsByInbox(normalizedInbox, DRAWER_STATUSES, PageRequest.of(0, safeLimit * 4));
        List<Map<String, Object>> rows = new ArrayList<>();
        String label = null;
        String face = null;
        String lotacaoTipo = null;
        String detail = null;
        for (WorkItem item : items) {
            if (!normalizedDrawerKey.equals(extractMarker(item, DRAWER_KEY_MARKER))) {
                continue;
            }
            label = firstNonBlank(label, extractMarker(item, DRAWER_LABEL_MARKER));
            face = firstNonBlank(face, extractMarker(item, DRAWER_FACE_MARKER));
            lotacaoTipo = firstNonBlank(lotacaoTipo, extractMarker(item, DRAWER_LOTACAO_MARKER));
            detail = firstNonBlank(detail, extractMarker(item, DRAWER_DETAIL_MARKER));
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("workItemId", item.getId());
            row.put("processoId", item.getProcessoId());
            row.put("processoNumero", contextEnvelopeService.processNumber(item.getProcesso()));
            row.put("titulo", item.getTitulo());
            row.put("descricao", item.getDescricao());
            row.put("status", item.getStatus() != null ? item.getStatus().name() : null);
            row.put("createdAt", item.getCreatedAt());
            row.put("updatedAt", item.getUpdatedAt());
            row.put("auditHash", Hashes.sha256HexPrefix(normalizedDrawerKey + '|' + item.getId(), 32));
            rows.add(safeCopy(row));
            if (rows.size() >= safeLimit) {
                break;
            }
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("status", "GAVETA_SECRETARIA_OFICIAL_DETALHE");
        out.put("generatedAt", timeService.nowUtc());
        out.put("inboxKey", normalizedInbox);
        out.put("drawerKey", normalizedDrawerKey);
        out.put("rotulo", firstNonBlank(label, "Gaveta operacional"));
        out.put("faceSecretaria", firstNonBlank(face, "SECRETARIA_DO_FORUM"));
        out.put("lotacaoTipo", firstNonBlank(lotacaoTipo, "COMARCA"));
        out.put("detalhamento", firstNonBlank(detail, "Sem detalhamento"));
        out.put("itens", rows);
        out.put("total", rows.size());
        return safeCopy(out);
    }

    private DrawerReservation resolveReservation(WorkItem desk, Usuario official, String actName) {
        Processo processo = Objects.requireNonNull(desk.getProcesso(), "processo_requerido");
        String tribunal = firstNonBlank(processo.getTribunalCodigoRoteado(), processo.getTribunal(), extractTribunalFromDrawerSeed(processo));
        String unidade = normalizeUnit(processo.getUnidadeJudiciariaCodigo());
        String vara = normalizeLabel(firstNonBlank(processo.getVara(), extractToken(desk.getDescricao(), "Vara: ", '.')));
        String comarca = normalizeLabel(firstNonBlank(processo.getComarca(), desk.getComarca(), official != null ? official.getComarca() : null));
        String uf = normalizeLabel(firstNonBlank(processo.getUf(), desk.getUf(), official != null ? official.getUf() : null));
        String lotacaoTipo;
        String drawerKey;
        String label;
        String detail;
        String face;
        if (unidade != null) {
            lotacaoTipo = "UNIDADE_JUDICIARIA";
            drawerKey = "GAVETA:UNIDADE:" + normalizeToken(unidade, "UNIDADE");
            label = "Gaveta da unidade judiciária " + unidade;
            detail = joinNonBlank(" • ", firstNonBlank(tribunal, "TRIBUNAL"), firstNonBlank(comarca, uf), unidade);
            face = "SECRETARIA_DA_VARA";
        } else if (vara != null) {
            lotacaoTipo = "VARA";
            drawerKey = "GAVETA:VARA:" + normalizeToken(firstNonBlank(tribunal, "TRIBUNAL"), "TRIBUNAL") + ':'
                    + normalizeToken(firstNonBlank(comarca, uf, "COMARCA"), "COMARCA") + ':' + normalizeToken(vara, "VARA");
            label = "Gaveta da vara " + vara;
            detail = joinNonBlank(" • ", firstNonBlank(tribunal, "TRIBUNAL"), firstNonBlank(comarca, uf), vara);
            face = "SECRETARIA_DA_VARA";
        } else if (comarca != null) {
            lotacaoTipo = "COMARCA";
            drawerKey = "GAVETA:COMARCA:" + normalizeToken(firstNonBlank(uf, "BR"), "BR") + ':' + normalizeToken(comarca, "COMARCA");
            label = "Gaveta do fórum de " + comarca;
            detail = joinNonBlank(" • ", firstNonBlank(tribunal, "TRIBUNAL"), comarca, uf);
            face = hasTribunalFace(tribunal) ? "SECRETARIA_DO_TRIBUNAL" : "SECRETARIA_DO_FORUM";
        } else {
            lotacaoTipo = "TRIBUNAL";
            drawerKey = "GAVETA:TRIBUNAL:" + normalizeToken(firstNonBlank(tribunal, "TRIBUNAL"), "TRIBUNAL");
            label = "Gaveta do tribunal " + firstNonBlank(tribunal, "TRIBUNAL");
            detail = firstNonBlank(tribunal, "TRIBUNAL");
            face = "SECRETARIA_DO_TRIBUNAL";
        }
        return new DrawerReservation(
                firstNonBlank(desk.getQueueCode(), "SECRETARIA_GAVETA_OFICIAL"),
                firstNonBlank(desk.getInboxKey(), "SECRETARIA_GAVETA_OFICIAL"),
                drawerKey,
                label,
                face,
                lotacaoTipo,
                detail,
                firstNonBlank(tribunal, "TRIBUNAL"),
                comarca,
                uf,
                vara,
                unidade,
                normalizeLabel(actName)
        );
    }

    private Map<String, Object> reservationMap(DrawerReservation reservation, WorkItem item, Map<String, Object> operation) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("queueCode", reservation.queueCode());
        out.put("inboxKey", reservation.inboxKey());
        out.put("drawerKey", reservation.drawerKey());
        out.put("rotulo", reservation.label());
        out.put("faceSecretaria", reservation.face());
        out.put("lotacaoTipo", reservation.lotacaoTipo());
        out.put("detalhamento", reservation.detail());
        putIfNotBlank(out, "tribunal", reservation.tribunal());
        putIfNotBlank(out, "comarca", reservation.comarca());
        putIfNotBlank(out, "uf", reservation.uf());
        putIfNotBlank(out, "vara", reservation.vara());
        putIfNotBlank(out, "unidadeJudiciaria", reservation.unidade());
        putIfNotBlank(out, "ato", reservation.actName());
        out.put("gavetasPath", OperationalApiRoutes.secretariatOperationalOfficialClosureDrawers(reservation.inboxKey()));
        out.put("gavetaDetalhePath", OperationalApiRoutes.secretariatOperationalOfficialClosureDrawerDetail(reservation.inboxKey(), reservation.drawerKey()));
        if (item != null) {
            out.put("workItemId", item.getId());
            out.put("createdAt", item.getCreatedAt());
        }
        if (operation != null && !operation.isEmpty()) {
            out.put("operationStatus", operation.get("atoExecutado"));
        }
        out.put("auditHash", Hashes.sha256HexPrefix(reservation.drawerKey() + '|' + firstNonBlank(reservation.actName(), "ATO"), 32));
        return safeCopy(out);
    }

    private String buildArchiveTitle(Processo processo, DrawerReservation reservation, String actName) {
        return trim("Gaveta da secretaria — " + reservation.label() + " — " + firstNonBlank(normalizeLabel(actName), "ATO")
                + " — " + contextEnvelopeService.processNumber(processo), 220);
    }

    private String buildArchiveDescription(Processo processo,
                                           WorkItem desk,
                                           Usuario official,
                                           DrawerReservation reservation,
                                           String actName,
                                           Map<String, Object> operation,
                                           String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Arquivo materializado da secretaria para atos subsequentes do Oficial de Justiça.");
        parts.add("Processo: " + contextEnvelopeService.processNumber(processo) + '.');
        parts.add("Gaveta: " + reservation.label() + '.');
        parts.add("Detalhamento: " + reservation.detail() + '.');
        parts.add("Face da secretaria: " + reservation.face() + '.');
        if (official != null && official.getNome() != null) {
            parts.add("Oficial vinculado: " + official.getNome() + '.');
        }
        if (actName != null) {
            parts.add("Ato materializado: " + actName + '.');
        }
        if (operation != null && operation.get("resultado") != null) {
            parts.add("Resultado operacional: " + trim(String.valueOf(operation.get("resultado")), 800) + '.');
        }
        if (desk.getDescricao() != null && !desk.getDescricao().isBlank()) {
            parts.add("Retorno base: " + trim(desk.getDescricao(), 900) + '.');
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação: " + observation + '.');
        }
        return trim(String.join(" ", parts), 3500);
    }

    private String buildArchiveBaseLegal(DrawerReservation reservation,
                                         String actName,
                                         Map<String, Object> operation,
                                         String observation) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Arquivo de gaveta institucional da secretaria para atos do Oficial de Justiça.");
        parts.add(DRAWER_KEY_MARKER + reservation.drawerKey() + MARKER_END);
        parts.add(DRAWER_LABEL_MARKER + reservation.label() + MARKER_END);
        parts.add(DRAWER_FACE_MARKER + reservation.face() + MARKER_END);
        parts.add(DRAWER_LOTACAO_MARKER + reservation.lotacaoTipo() + MARKER_END);
        parts.add(DRAWER_DETAIL_MARKER + reservation.detail() + MARKER_END);
        if (actName != null) {
            parts.add("Ato arquivado: " + actName + '.');
        }
        if (operation != null && operation.get("atoExecutado") != null) {
            parts.add("Operação materializada: " + operation.get("atoExecutado") + '.');
        }
        if (observation != null && !observation.isBlank()) {
            parts.add("Observação da secretaria: " + observation + '.');
        }
        return trim(String.join(" ", parts), 2200);
    }

    private String sourceToken(WorkItem desk) {
        String template = desk.getTemplateCode();
        if (template == null || template.isBlank()) {
            return String.valueOf(desk.getId());
        }
        int start = template.indexOf(':');
        int last = template.lastIndexOf(':');
        if (start < 0 || last <= start) {
            return String.valueOf(desk.getId());
        }
        String candidate = template.substring(start + 1, last).replace(':', '_').trim();
        return candidate.isBlank() ? String.valueOf(desk.getId()) : normalizeToken(candidate, String.valueOf(desk.getId()));
    }

    private String extractTribunalFromDrawerSeed(Processo processo) {
        if (processo == null) {
            return null;
        }
        return firstNonBlank(processo.getTribunal(), processo.getTribunalCodigoRoteado(), processo.getUf());
    }

    private boolean hasTribunalFace(String tribunal) {
        String normalized = normalizeToken(tribunal, "");
        return normalized.startsWith("TJ") || normalized.startsWith("TRF") || normalized.startsWith("TRE")
                || normalized.startsWith("TRT") || normalized.startsWith("STM") || normalized.startsWith("TJM")
                || "STJ".equals(normalized) || "STF".equals(normalized);
    }

    private String normalizeUnit(String value) {
        return normalizeLabel(value);
    }

    private String extractMarker(WorkItem item, String marker) {
        return firstNonBlank(extractBetween(item != null ? item.getBaseLegal() : null, marker, MARKER_END), extractBetween(item != null ? item.getDescricao() : null, marker, MARKER_END));
    }

    private String validateDrawerKey(String drawerKey) {
        if (drawerKey == null || drawerKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gaveta da secretaria não informada");
        }
        String normalized = drawerKey.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > 180 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gaveta da secretaria inválida");
        }
        if (!normalized.startsWith("GAVETA:") || !normalized.matches("[A-Z0-9:_-]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "gaveta da secretaria inválida");
        }
        return normalized;
    }

    private String extractBetween(String source, String startMarker, String endMarker) {
        if (source == null || source.isBlank() || startMarker == null || startMarker.isBlank() || endMarker == null || endMarker.isBlank()) {
            return null;
        }
        int start = source.indexOf(startMarker);
        if (start < 0) {
            return null;
        }
        int valueStart = start + startMarker.length();
        int end = source.indexOf(endMarker, valueStart);
        if (end < 0) {
            return null;
        }
        String candidate = source.substring(valueStart, end).trim();
        return candidate.isEmpty() ? null : candidate;
    }

    private String extractToken(String source, String prefix, char endChar) {
        if (source == null || source.isBlank() || prefix == null || prefix.isBlank()) {
            return null;
        }
        int start = source.indexOf(prefix);
        if (start < 0) {
            return null;
        }
        int valueStart = start + prefix.length();
        int end = source.indexOf(endChar, valueStart);
        if (end < 0) {
            end = source.length();
        }
        String candidate = source.substring(valueStart, end).trim();
        return candidate.isEmpty() ? null : candidate;
    }


    private String joinNonBlank(String delimiter, String... values) {
        ArrayList<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                if (value != null) {
                    String normalized = value.trim();
                    if (!normalized.isBlank()) {
                        parts.add(normalized);
                    }
                }
            }
        }
        return parts.isEmpty() ? null : String.join(delimiter, parts);
    }

    private String normalizeToken(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+|_+$", "");
        return normalized.isBlank() ? fallback : normalized;
    }

    private String normalizeLabel(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String trim(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 1)).trim() + '…';
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null) {
                String normalized = value.trim();
                if (!normalized.isBlank()) {
                    return normalized;
                }
            }
        }
        return null;
    }

    private void putIfNotBlank(Map<String, Object> target, String key, String value) {
        if (target != null && key != null && value != null && !value.isBlank()) {
            target.put(key, value);
        }
    }

    private Map<String, Object> safeCopy(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((key, value) -> {
                if (key == null || key.isBlank() || value == null) {
                    return;
                }
                if (value instanceof Map<?, ?> nested) {
                    LinkedHashMap<String, Object> nestedCopy = new LinkedHashMap<>();
                    nested.forEach((nestedKey, nestedValue) -> {
                        if (nestedKey != null && nestedValue != null) {
                            nestedCopy.put(String.valueOf(nestedKey), nestedValue);
                        }
                    });
                    out.put(key, nestedCopy.isEmpty() ? Map.of() : Map.copyOf(nestedCopy));
                } else if (value instanceof List<?> list) {
                    out.put(key, List.copyOf(list));
                } else {
                    out.put(key, value);
                }
            });
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private record DrawerReservation(String queueCode,
                                     String inboxKey,
                                     String drawerKey,
                                     String label,
                                     String face,
                                     String lotacaoTipo,
                                     String detail,
                                     String tribunal,
                                     String comarca,
                                     String uf,
                                     String vara,
                                     String unidade,
                                     String actName) {
    }

    private static final class DrawerAggregate {
        private final String drawerKey;
        private final String label;
        private final String face;
        private final String lotacaoTipo;
        private final String detail;
        private final List<Long> processos = new ArrayList<>();
        private long count;
        private Instant latestUpdate;
        private String latestProcessNumber;

        private DrawerAggregate(String drawerKey, String label, String face, String lotacaoTipo, String detail) {
            this.drawerKey = drawerKey;
            this.label = label;
            this.face = face;
            this.lotacaoTipo = lotacaoTipo;
            this.detail = detail;
        }

        private void accept(WorkItem item) {
            if (item.getProcessoId() != null && processos.stream().noneMatch(existing -> existing.equals(item.getProcessoId()))) {
                processos.add(item.getProcessoId());
            }
            Instant eventTime = item.getUpdatedAt() != null ? item.getUpdatedAt() : item.getCreatedAt();
            if (eventTime != null && (latestUpdate == null || eventTime.isAfter(latestUpdate))) {
                latestUpdate = eventTime;
                latestProcessNumber = item.getProcesso() != null ? item.getProcesso().getNumeroProcesso() : null;
            }
            count++;
        }

        private String drawerKey() {
            return drawerKey;
        }

        private String label() {
            return label;
        }

        private String face() {
            return face;
        }

        private String lotacaoTipo() {
            return lotacaoTipo;
        }

        private String detail() {
            return detail;
        }

        private List<Long> processos() {
            return processos;
        }

        private long count() {
            return count;
        }

        private Instant latestUpdate() {
            return latestUpdate;
        }

        private String latestProcessNumber() {
            return latestProcessNumber;
        }
    }
}
