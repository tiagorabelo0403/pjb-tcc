package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioCartorioAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioChannelAckRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioConfirmationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioReconciliationRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRetryRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaTraceableCommunicationLedgerService {

    private static final int DEFAULT_RETRY_LIMIT = 5;
    private static final int MAX_STATES = 20_000;
    private static final long TERMINAL_RETENTION_MILLIS = Duration.ofDays(7).toMillis();
    private static final long ACTIVE_IDLE_RETENTION_MILLIS = Duration.ofDays(1).toMillis();
    private final AuditLedgerService auditLedgerService;
    private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();
    private final AtomicLong pruneSequence = new AtomicLong();

    public OficialJusticaTraceableCommunicationLedgerService(AuditLedgerService auditLedgerService) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService, "auditLedgerService");
    }

    public Map<String, Object> blueprint() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "OFICIAL_TRACEABLE_COMMUNICATION_LEDGER");
        out.put("actorLane", "OFICIAL_JUSTICA");
        out.put("statusModel", List.of(
                "AGUARDANDO_CONFIRMACAO_DESTINATARIO",
                "DISPONIBILIZADA_CAIXA_INSTITUCIONAL",
                "ENCAMINHADA_MALHA_EXTERNA",
                "ENTREGUE_EXTERNAMENTE",
                "CANAL_CONFIRMADO",
                "ACK_CARTORARIO",
                "JUNTADA_MATERIALIZADA",
                "CONFIRMADO",
                "ENTREGUE_E_JUNTADO",
                "ERRO_ENTREGA",
                "RETENTATIVA_PROGRAMADA",
                "RECONCILIADO"
        ));
        out.put("queues", List.of("OFICIAL_JUSTICA_DELIVERY_CONFIRMATION", "OFICIAL_JUSTICA_DELIVERY_ERROR", "OFICIAL_JUSTICA_DELIVERY_RETRY"));
        out.put("governance", List.of(
                "route_hash",
                "audit_hash",
                "status_trail",
                "retry_backoff",
                "hash_destinatario",
                "mesh_mirror",
                "external_dispatch",
                "cartorio_ack",
                "reconciliacao_materializada"
        ));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> registerExecution(TipoUsuario tipoUsuario,
                                                 Long processoId,
                                                 String operationCode,
                                                 String partnerSystem,
                                                 String templateCode,
                                                 Map<String, Object> oficioType,
                                                 Map<String, Object> destinatarioResolvido,
                                                 Map<String, Object> minutaGovernada,
                                                 boolean exigirConfirmacaoEntrega) {
        String actorLane = actorLane(tipoUsuario);
        String normalizedOperation = normalize(operationCode, "OFICIO_OFICIAL_JUSTICA");
        String normalizedPartner = normalize(partnerSystem, "CAIXA_INSTITUCIONAL_PJB");
        Map<String, Object> safeType = immutableMap(oficioType);
        Map<String, Object> safeRecipient = immutableMap(destinatarioResolvido);
        Map<String, Object> safeTemplate = immutableMap(minutaGovernada);
        Instant now = Instant.now();
        String canonicalBase = actorLane + '|' + processoId + '|' + normalizedOperation + '|' + normalizedPartner + '|' + safeType + '|' + safeRecipient + '|' + safeTemplate;
        String routeHash = Hashes.sha256Hex(canonicalBase);
        String executionId = "PJB-OFI-EXEC-" + Hashes.sha256HexPrefix(canonicalBase + ":execution", 18).toUpperCase(Locale.ROOT);
        String idempotencyKey = "oficial:oficio:" + Hashes.sha256HexPrefix(canonicalBase + ":idempotency", 24);
        String status = exigirConfirmacaoEntrega ? "AGUARDANDO_CONFIRMACAO_DESTINATARIO" : "ENTREGUE_E_JUNTADO";
        List<Map<String, Object>> trail = new ArrayList<>();
        trail.add(statusEvent("EXECUCAO_REGISTRADA", status, "Ofício do oficial registrado na malha rastreável institucional.", routeHash, now, 0));
        if (exigirConfirmacaoEntrega) {
            trail.add(statusEvent("CONFIRMACAO_PENDENTE", status, "Confirmação de entrega aguardando retorno do destinatário ou da caixa institucional.", routeHash, now, 0));
        }
        String auditHash = auditHash(executionId, status, 0, safeRecipient, safeTemplate, Map.of(), Map.of(), Map.of(), trail);
        State state = new State(
                executionId,
                actorLane,
                processoId,
                normalizedOperation,
                normalizedPartner,
                normalize(templateCode, "MINUTA_PADRAO"),
                exigirConfirmacaoEntrega,
                status,
                0,
                DEFAULT_RETRY_LIMIT,
                now,
                now,
                null,
                idempotencyKey,
                routeHash,
                auditHash,
                queue("CONFIRMATION"),
                queue("ERROR"),
                queue("RETRY"),
                safeType,
                safeRecipient,
                safeTemplate,
                List.of(),
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                List.of(),
                Map.of(),
                Map.of(),
                List.copyOf(trail)
        );
        storeState(executionId, state);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_TRACEABLE_REGISTERED", "OFICIAL_OFICIO", executionId, auditHash, normalizedOperation + "#" + normalizedPartner + "#" + status);
        return snapshot(state);
    }

    public Map<String, Object> attachDispatchTopology(TipoUsuario tipoUsuario, String executionId, Map<String, Object> dispatchContext) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        Map<String, Object> safeContext = immutableMap(dispatchContext);
        List<String> channelChain = immutableStringList(safeContext.get("channelChain"));
        String currentChannel = normalize(stringValue(safeContext.get("currentChannel")), current.currentChannel());
        Map<String, Object> caixaInstitucional = immutableMap(safeContext.get("caixaInstitucional"));
        Map<String, Object> meshMirror = immutableMap(safeContext.get("meshMirror"));
        Map<String, Object> externalDispatch = immutableMap(safeContext.get("externalDispatch"));
        Map<String, Object> reconciliationMaterialized = immutableMap(safeContext.get("reconciliationMaterialized"));
        String suggestedStatus = normalize(stringValue(safeContext.get("statusSuggested")), current.currentStatus());
        Instant now = Instant.now();
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("MALHA_EXTERNA_ACOPLADA", suggestedStatus, "Trilho institucional externo e caixa cartorária acoplados à execução do ofício.", current.routeHash(), now, current.attempts()));
        String auditHash = auditHash(executionId, suggestedStatus, current.attempts(), current.destinatarioResolvido(), current.minutaGovernada(), externalDispatch, cartorioAckDefault(current.cartorioAcknowledgement(), caixaInstitucional), reconciliationMaterialized, trail);
        State updated = new State(
                current.executionId(),
                current.actorLane(),
                current.processoId(),
                current.operationCode(),
                current.partnerSystem(),
                current.templateCode(),
                current.requiresConfirmation(),
                suggestedStatus,
                current.attempts(),
                current.retryLimit(),
                current.createdAt(),
                now,
                current.nextRetryAt(),
                current.idempotencyKey(),
                current.routeHash(),
                auditHash,
                current.confirmationQueue(),
                current.errorQueue(),
                current.retryQueue(),
                current.oficioType(),
                current.destinatarioResolvido(),
                current.minutaGovernada(),
                channelChain,
                currentChannel,
                caixaInstitucional,
                meshMirror,
                externalDispatch,
                current.channelConfirmations(),
                cartorioAckDefault(current.cartorioAcknowledgement(), caixaInstitucional),
                reconciliationMaterialized,
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_DISPATCH_TOPOLOGY_ATTACHED", "OFICIAL_OFICIO", executionId, auditHash, suggestedStatus);
        return snapshot(updated);
    }

    public Map<String, Object> recentExecutions(TipoUsuario tipoUsuario, int limit) {
        String actorLane = actorLane(tipoUsuario);
        int safeLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        List<Map<String, Object>> executions = recentStates(actorLane).stream()
                .filter(state -> actorLane.equals(state.actorLane()))
                .sorted(Comparator.comparing(State::updatedAt).reversed())
                .limit(safeLimit)
                .map(this::snapshot)
                .toList();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "OFICIAL_TRACEABLE_EXECUTIONS_RECENT");
        out.put("actorLane", actorLane);
        out.put("count", executions.size());
        out.put("executions", executions);
        out.put("ledgerBlueprint", blueprint());
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> executionStatus(TipoUsuario tipoUsuario, String executionId) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        return snapshot(current);
    }

    public Map<String, Object> externalMeshStatus(TipoUsuario tipoUsuario, String executionId) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("executionId", current.executionId());
        out.put("currentStatus", current.currentStatus());
        putIfNotNull(out, "currentChannel", current.currentChannel());
        out.put("channelChain", current.channelChain());
        out.put("caixaInstitucional", current.caixaInstitucional());
        out.put("meshMirror", current.meshMirror());
        out.put("externalDispatch", current.externalDispatch());
        out.put("channelConfirmations", current.channelConfirmations());
        out.put("cartorioAcknowledgement", current.cartorioAcknowledgement());
        out.put("reconciliationMaterialized", current.reconciliationMaterialized());
        out.put("statusTrail", current.statusTrail());
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> confirmDelivery(TipoUsuario tipoUsuario, String executionId, OficialJusticaOficioConfirmationRequest request) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        OficialJusticaOficioConfirmationRequest safe = request == null ? new OficialJusticaOficioConfirmationRequest(null, null, null, null, null, Boolean.FALSE) : request;
        Instant now = Instant.now();
        String deliveryStatus = normalize(safe.statusEntrega(), "CONFIRMADO");
        String targetStatus = switch (deliveryStatus) {
            case "ENTREGUE", "CONFIRMADO", "JUNTADO" -> current.requiresConfirmation() ? "CONFIRMADO" : "ENTREGUE_E_JUNTADO";
            case "ENTREGUE_E_JUNTADO" -> "ENTREGUE_E_JUNTADO";
            case "ERRO", "FALHA", "DEVOLVIDO" -> "ERRO_ENTREGA";
            default -> "CONFIRMADO";
        };
        int attempts = current.attempts();
        Instant nextRetryAt = null;
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("CONFIRMACAO_ENTREGA", targetStatus, safe.observacaoOperacional(), current.routeHash(), now, attempts));
        if ("ERRO_ENTREGA".equals(targetStatus) && safe.acionarRetentativaResolvido() && attempts < current.retryLimit()) {
            attempts++;
            targetStatus = "RETENTATIVA_PROGRAMADA";
            nextRetryAt = now.plus(attempts * 15L, ChronoUnit.MINUTES);
            trail.add(statusEvent("RETENTATIVA_PROGRAMADA", targetStatus, "Retentativa programada após falha de entrega institucional.", current.routeHash(), now, attempts));
        }
        if ("CONFIRMADO".equals(targetStatus) && !current.requiresConfirmation()) {
            targetStatus = "ENTREGUE_E_JUNTADO";
        }
        LinkedHashMap<String, Object> minuta = new LinkedHashMap<>(current.minutaGovernada());
        minuta.put("confirmationChannel", normalize(safe.canalConfirmacao(), firstNonBlank(current.currentChannel(), current.partnerSystem())));
        if (safe.protocoloEntrega() != null && !safe.protocoloEntrega().isBlank()) {
            minuta.put("deliveryProtocol", safe.protocoloEntrega().trim());
        }
        if (safe.referenciaExterna() != null && !safe.referenciaExterna().isBlank()) {
            minuta.put("externalReference", safe.referenciaExterna().trim());
        }
        Map<String, Object> externalDispatch = mergeMaps(current.externalDispatch(), mapOfNonNulls(
                "providerReference", firstNonBlank(stringValue(current.externalDispatch().get("providerReference")), safe.referenciaExterna()),
                "protocolReference", safe.protocoloEntrega(),
                "confirmationChannel", normalize(safe.canalConfirmacao(), firstNonBlank(current.currentChannel(), current.partnerSystem()))
        ));
        String auditHash = auditHash(executionId, targetStatus, attempts, current.destinatarioResolvido(), minuta, externalDispatch, current.cartorioAcknowledgement(), current.reconciliationMaterialized(), trail);
        State updated = current.withMutation(
                targetStatus,
                attempts,
                now,
                nextRetryAt,
                auditHash,
                current.channelChain(),
                current.currentChannel(),
                current.caixaInstitucional(),
                current.meshMirror(),
                externalDispatch,
                current.channelConfirmations(),
                current.cartorioAcknowledgement(),
                current.reconciliationMaterialized(),
                Map.copyOf(minuta),
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_DELIVERY_UPDATED", "OFICIAL_OFICIO", executionId, auditHash, targetStatus);
        return snapshot(updated);
    }

    public Map<String, Object> confirmChannelDelivery(TipoUsuario tipoUsuario, String executionId, OficialJusticaOficioChannelAckRequest request) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        OficialJusticaOficioChannelAckRequest safe = request == null ? new OficialJusticaOficioChannelAckRequest(null, null, null, null, null, Boolean.FALSE) : request;
        Instant now = Instant.now();
        String channel = normalize(safe.canalConfirmado(), firstNonBlank(current.currentChannel(), current.partnerSystem()));
        String statusCanal = normalize(safe.statusCanal(), safe.entregaDefinitivaResolvida() ? "ENTREGUE_EXTERNAMENTE" : "CANAL_CONFIRMADO");
        String targetStatus = safe.entregaDefinitivaResolvida() ? "ENTREGUE_EXTERNAMENTE" : "CANAL_CONFIRMADO";
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("ACK_CANAL", targetStatus, safe.observacaoOperacional(), current.routeHash(), now, current.attempts()));
        List<Map<String, Object>> confirmations = new ArrayList<>(current.channelConfirmations());
        confirmations.add(mapOfNonNulls(
                "channel", channel,
                "status", statusCanal,
                "providerReference", firstNonBlank(safe.providerReference(), stringValue(current.externalDispatch().get("providerReference"))),
                "protocol", safe.protocoloCanal(),
                "confirmedAt", now.toString(),
                "observacao", firstNonBlank(safe.observacaoOperacional(), "Confirmação de canal registrada.")
        ));
        Map<String, Object> externalDispatch = mergeMaps(current.externalDispatch(), mapOfNonNulls(
                "channelAckStatus", statusCanal,
                "providerReference", firstNonBlank(safe.providerReference(), stringValue(current.externalDispatch().get("providerReference"))),
                "protocolReference", safe.protocoloCanal(),
                "channelConfirmedAt", now.toString(),
                "deliveryStatus", targetStatus
        ));
        Map<String, Object> reconciliation = mergeMaps(current.reconciliationMaterialized(), mapOfNonNulls(
                "providerStatus", statusCanal,
                "materializedAt", now.toString(),
                "reconciliationHash", Hashes.sha256Hex(executionId + '|' + statusCanal + '|' + current.destinatarioResolvido() + '|' + current.minutaGovernada() + '|' + confirmations)
        ));
        String auditHash = auditHash(executionId, targetStatus, current.attempts(), current.destinatarioResolvido(), current.minutaGovernada(), externalDispatch, current.cartorioAcknowledgement(), reconciliation, trail);
        State updated = current.withMutation(
                targetStatus,
                current.attempts(),
                now,
                current.nextRetryAt(),
                auditHash,
                current.channelChain(),
                channel,
                current.caixaInstitucional(),
                current.meshMirror(),
                externalDispatch,
                List.copyOf(confirmations),
                current.cartorioAcknowledgement(),
                reconciliation,
                current.minutaGovernada(),
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_CHANNEL_ACK", "OFICIAL_OFICIO", executionId, auditHash, channel + "#" + statusCanal);
        return snapshot(updated);
    }

    public Map<String, Object> acknowledgeCartorio(TipoUsuario tipoUsuario, String executionId, OficialJusticaOficioCartorioAckRequest request) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        OficialJusticaOficioCartorioAckRequest safe = request == null ? new OficialJusticaOficioCartorioAckRequest(null, null, null, null, Boolean.FALSE) : request;
        Instant now = Instant.now();
        String statusCartorio = normalize(safe.statusCartorio(), safe.juntadaMaterializadaResolvida() ? "JUNTADA_MATERIALIZADA" : "ACK_CARTORARIO");
        String targetStatus = safe.juntadaMaterializadaResolvida() ? "JUNTADA_MATERIALIZADA" : "ACK_CARTORARIO";
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("ACK_CARTORARIO", targetStatus, safe.observacaoCartoraria(), current.routeHash(), now, current.attempts()));
        Map<String, Object> cartorioAcknowledgement = mergeMaps(cartorioAckDefault(current.cartorioAcknowledgement(), current.caixaInstitucional()), mapOfNonNulls(
                "status", statusCartorio,
                "protocol", safe.protocoloCartorio(),
                "caixaInstitucionalCodigo", firstNonBlank(safe.caixaInstitucionalCodigo(), stringValue(current.caixaInstitucional().get("caixaCodigo"))),
                "acknowledgedAt", now.toString(),
                "observacao", firstNonBlank(safe.observacaoCartoraria(), "Recebimento cartorário confirmado."),
                "juntadaMaterializada", safe.juntadaMaterializadaResolvida()
        ));
        Map<String, Object> reconciliation = mergeMaps(current.reconciliationMaterialized(), mapOfNonNulls(
                "cartorioStatus", statusCartorio,
                "materializedAt", now.toString(),
                "reconciliationHash", Hashes.sha256Hex(executionId + '|' + statusCartorio + '|' + cartorioAcknowledgement + '|' + current.externalDispatch())
        ));
        String auditHash = auditHash(executionId, targetStatus, current.attempts(), current.destinatarioResolvido(), current.minutaGovernada(), current.externalDispatch(), cartorioAcknowledgement, reconciliation, trail);
        State updated = current.withMutation(
                targetStatus,
                current.attempts(),
                now,
                current.nextRetryAt(),
                auditHash,
                current.channelChain(),
                current.currentChannel(),
                current.caixaInstitucional(),
                current.meshMirror(),
                current.externalDispatch(),
                current.channelConfirmations(),
                cartorioAcknowledgement,
                reconciliation,
                current.minutaGovernada(),
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_CARTORIO_ACK", "OFICIAL_OFICIO", executionId, auditHash, statusCartorio);
        return snapshot(updated);
    }

    public Map<String, Object> reconcileExecution(TipoUsuario tipoUsuario, String executionId, OficialJusticaOficioReconciliationRequest request) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        OficialJusticaOficioReconciliationRequest safe = request == null ? new OficialJusticaOficioReconciliationRequest(null, null, null, null, null, Boolean.FALSE) : request;
        Instant now = Instant.now();
        String partnerStatus = normalize(safe.statusParceiro(), stringValue(current.reconciliationMaterialized().get("providerStatus")));
        String targetStatus = safe.repararDivergenciaResolvida() ? "RECONCILIADO" : current.currentStatus();
        boolean divergent = safe.hashParceiro() != null
                && current.minutaGovernada().get("contentHash") != null
                && !safe.hashParceiro().trim().equals(String.valueOf(current.minutaGovernada().get("contentHash")));
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        trail.add(statusEvent("RECONCILIACAO", safe.repararDivergenciaResolvida() ? "RECONCILIADO" : current.currentStatus(), safe.observacao(), current.routeHash(), now, current.attempts()));
        Map<String, Object> reconciliation = mergeMaps(current.reconciliationMaterialized(), mapOfNonNulls(
                "mode", "OFICIAL_OFICIO_RECONCILIATION_MATERIALIZED",
                "origin", firstNonBlank(safe.origemReconciliacao(), "MALHA_INSTITUCIONAL_EXTERNA"),
                "providerStatus", partnerStatus,
                "partnerReference", safe.referenciaParceiro(),
                "partnerHash", safe.hashParceiro(),
                "detail", safe.observacao(),
                "divergent", divergent,
                "repaired", safe.repararDivergenciaResolvida(),
                "materializedAt", now.toString(),
                "reconciliationHash", Hashes.sha256Hex(executionId + '|' + partnerStatus + '|' + safe.referenciaParceiro() + '|' + safe.hashParceiro() + '|' + safe.observacao() + '|' + current.externalDispatch() + '|' + current.cartorioAcknowledgement())
        ));
        String auditHash = auditHash(executionId, targetStatus, current.attempts(), current.destinatarioResolvido(), current.minutaGovernada(), current.externalDispatch(), current.cartorioAcknowledgement(), reconciliation, trail);
        State updated = current.withMutation(
                targetStatus,
                current.attempts(),
                now,
                current.nextRetryAt(),
                auditHash,
                current.channelChain(),
                current.currentChannel(),
                current.caixaInstitucional(),
                current.meshMirror(),
                current.externalDispatch(),
                current.channelConfirmations(),
                current.cartorioAcknowledgement(),
                reconciliation,
                current.minutaGovernada(),
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_RECONCILED", "OFICIAL_OFICIO", executionId, auditHash, partnerStatus);
        return snapshot(updated);
    }

    public Map<String, Object> retryExecution(TipoUsuario tipoUsuario, String executionId, OficialJusticaOficioRetryRequest request) {
        State current = loadState(executionId);
        if (current == null) {
            return notFound(executionId);
        }
        if (!actorLane(tipoUsuario).equals(current.actorLane())) {
            return accessMismatch(executionId);
        }
        int attempts = Math.min(current.attempts() + 1, current.retryLimit());
        Instant now = Instant.now();
        Instant nextRetryAt = now.plus(Math.max(10, attempts * 15L), ChronoUnit.MINUTES);
        List<Map<String, Object>> trail = new ArrayList<>(current.statusTrail());
        String reason = request == null || request.motivo() == null || request.motivo().isBlank() ? "Retentativa manual do ofício pelo Oficial de Justiça." : request.motivo().trim();
        trail.add(statusEvent("RETENTATIVA_MANUAL", "RETENTATIVA_PROGRAMADA", reason, current.routeHash(), now, attempts));
        String currentChannel = request != null && request.novoCanalPreferencial() != null && !request.novoCanalPreferencial().isBlank()
                ? request.novoCanalPreferencial().trim().toUpperCase(Locale.ROOT)
                : current.currentChannel();
        Map<String, Object> externalDispatch = mergeMaps(current.externalDispatch(), mapOfNonNulls(
                "deliveryStatus", "RETENTATIVA_PROGRAMADA",
                "retryReason", reason,
                "retryPriority", request != null ? request.prioridade() : null,
                "currentChannel", currentChannel
        ));
        String auditHash = auditHash(executionId, "RETENTATIVA_PROGRAMADA", attempts, current.destinatarioResolvido(), current.minutaGovernada(), externalDispatch, current.cartorioAcknowledgement(), current.reconciliationMaterialized(), trail);
        State updated = current.withMutation(
                "RETENTATIVA_PROGRAMADA",
                attempts,
                now,
                nextRetryAt,
                auditHash,
                current.channelChain(),
                currentChannel,
                current.caixaInstitucional(),
                current.meshMirror(),
                externalDispatch,
                current.channelConfirmations(),
                current.cartorioAcknowledgement(),
                current.reconciliationMaterialized(),
                current.minutaGovernada(),
                List.copyOf(trail)
        );
        storeState(executionId, updated);
        auditLedgerService.appendSafely("OFICIAL_OFICIO_RETRY_PROGRAMMED", "OFICIAL_OFICIO", executionId, auditHash, reason);
        return snapshot(updated);
    }

    private State loadState(String executionId) {
        pruneStatesIfRequired(Instant.now(), false);
        return states.get(executionId);
    }

    private void storeState(String executionId, State state) {
        states.put(executionId, state);
        pruneStatesIfRequired(state.updatedAt(), false);
    }

    private List<State> recentStates(String actorLane) {
        pruneStatesIfRequired(Instant.now(), false);
        return states.values().stream()
                .filter(state -> actorLane.equals(state.actorLane()))
                .toList();
    }

    private void pruneStatesIfRequired(Instant reference, boolean force) {
        long sequence = pruneSequence.incrementAndGet();
        if (!force && sequence % 64 != 0 && states.size() <= MAX_STATES) {
            return;
        }
        long now = reference == null ? System.currentTimeMillis() : reference.toEpochMilli();
        states.entrySet().removeIf(entry -> isExpired(entry.getValue(), now));
        int overflow = states.size() - MAX_STATES;
        if (overflow <= 0) {
            return;
        }
        List<State> ordered = states.values().stream()
                .sorted(Comparator.comparing(State::updatedAt))
                .limit(overflow)
                .toList();
        for (State state : ordered) {
            states.remove(state.executionId(), state);
        }
    }

    private boolean isExpired(State state, long now) {
        Instant updatedAt = state.updatedAt() == null ? state.createdAt() : state.updatedAt();
        if (updatedAt == null) {
            return false;
        }
        long age = now - updatedAt.toEpochMilli();
        if (age < 0) {
            return false;
        }
        if (isTerminalStatus(state.currentStatus())) {
            return age > TERMINAL_RETENTION_MILLIS;
        }
        return age > ACTIVE_IDLE_RETENTION_MILLIS;
    }

    private boolean isTerminalStatus(String status) {
        return switch (normalize(status, "")) {
            case "CONFIRMADO", "ENTREGUE_E_JUNTADO", "JUNTADA_MATERIALIZADA", "RECONCILIADO", "ERRO_ENTREGA" -> true;
            default -> false;
        };
    }

    private Map<String, Object> snapshot(State state) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfNotNull(out, "executionId", state.executionId());
        putIfNotNull(out, "actorLane", state.actorLane());
        putIfNotNull(out, "processoId", state.processoId());
        putIfNotNull(out, "operationCode", state.operationCode());
        putIfNotNull(out, "partnerSystem", state.partnerSystem());
        putIfNotNull(out, "templateCode", state.templateCode());
        out.put("requiresConfirmation", state.requiresConfirmation());
        putIfNotNull(out, "currentStatus", state.currentStatus());
        out.put("attempts", state.attempts());
        out.put("retryLimit", state.retryLimit());
        putIfNotNull(out, "createdAt", state.createdAt().toString());
        putIfNotNull(out, "updatedAt", state.updatedAt().toString());
        putIfNotNull(out, "nextRetryAt", state.nextRetryAt() != null ? state.nextRetryAt().toString() : null);
        putIfNotNull(out, "idempotencyKey", state.idempotencyKey());
        putIfNotNull(out, "routeHash", state.routeHash());
        putIfNotNull(out, "auditHash", state.auditHash());
        out.put("queues", Map.of("confirmationQueue", state.confirmationQueue(), "errorQueue", state.errorQueue(), "retryQueue", state.retryQueue()));
        out.put("oficioType", state.oficioType());
        out.put("destinatarioResolvido", state.destinatarioResolvido());
        out.put("minutaGovernada", state.minutaGovernada());
        out.put("channelChain", state.channelChain());
        putIfNotNull(out, "currentChannel", state.currentChannel());
        out.put("caixaInstitucional", state.caixaInstitucional());
        out.put("meshMirror", state.meshMirror());
        out.put("externalDispatch", state.externalDispatch());
        out.put("channelConfirmations", state.channelConfirmations());
        out.put("cartorioAcknowledgement", state.cartorioAcknowledgement());
        out.put("reconciliationMaterialized", state.reconciliationMaterialized());
        out.put("statusTrail", state.statusTrail());
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> notFound(String executionId) {
        return Map.of("found", false, "executionId", executionId, "message", "Execução rastreável do ofício não encontrada.");
    }

    private static Map<String, Object> accessMismatch(String executionId) {
        return Map.of("found", false, "executionId", executionId, "message", "Execução rastreável pertence a outro trilho institucional.");
    }

    private static Map<String, Object> statusEvent(String eventCode, String targetStatus, String message, String routeHash, Instant occurredAt, int attempt) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("eventCode", eventCode);
        out.put("targetStatus", targetStatus);
        out.put("message", message == null || message.isBlank() ? eventCode : message.trim());
        out.put("routeHash", routeHash);
        out.put("occurredAt", occurredAt.toString());
        out.put("attempt", attempt);
        return Collections.unmodifiableMap(out);
    }

    private static String auditHash(String executionId,
                                    String status,
                                    int attempts,
                                    Map<String, Object> destinatario,
                                    Map<String, Object> minuta,
                                    Map<String, Object> externalDispatch,
                                    Map<String, Object> cartorioAck,
                                    Map<String, Object> reconciliation,
                                    List<Map<String, Object>> trail) {
        return Hashes.sha256Hex(executionId + '|' + status + '|' + attempts + '|' + destinatario + '|' + minuta + '|' + externalDispatch + '|' + cartorioAck + '|' + reconciliation + '|' + trail);
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        return "OFICIAL_JUSTICA";
    }

    private static String queue(String suffix) {
        return "OFICIAL_JUSTICA_DELIVERY_" + suffix;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
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

    private static Map<String, Object> immutableMap(Object values) {
        if (!(values instanceof Map<?, ?> source) || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                out.put(String.valueOf(key), value);
            }
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static List<String> immutableStringList(Object values) {
        if (!(values instanceof List<?> source) || source.isEmpty()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : source) {
            if (item != null && !String.valueOf(item).isBlank()) {
                out.add(String.valueOf(item).trim().toUpperCase(Locale.ROOT));
            }
        }
        return List.copyOf(out);
    }

    private static Map<String, Object> mergeMaps(Map<String, Object> base, Map<String, ?> updates) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (base != null) {
            base.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(String.valueOf(key), value);
                }
            });
        }
        if (updates != null) {
            updates.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(String.valueOf(key), value);
                }
            });
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> mapOfNonNulls(Object... entries) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (entries == null) {
            return Map.of();
        }
        for (int i = 0; i + 1 < entries.length; i += 2) {
            Object key = entries[i];
            Object value = entries[i + 1];
            if (key != null && value != null) {
                out.put(String.valueOf(key), value);
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> cartorioAckDefault(Map<String, Object> current, Map<String, Object> caixaInstitucional) {
        if (current != null && !current.isEmpty()) {
            return current;
        }
        if (caixaInstitucional == null || caixaInstitucional.isEmpty()) {
            return Map.of();
        }
        return mapOfNonNulls(
                "status", "AGUARDANDO_ACK_CARTORARIO",
                "caixaInstitucionalCodigo", firstNonBlank(stringValue(caixaInstitucional.get("caixaCodigo")), stringValue(caixaInstitucional.get("inboxKey"))),
                "expected", Boolean.TRUE
        );
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    private record State(
            String executionId,
            String actorLane,
            Long processoId,
            String operationCode,
            String partnerSystem,
            String templateCode,
            boolean requiresConfirmation,
            String currentStatus,
            int attempts,
            int retryLimit,
            Instant createdAt,
            Instant updatedAt,
            Instant nextRetryAt,
            String idempotencyKey,
            String routeHash,
            String auditHash,
            String confirmationQueue,
            String errorQueue,
            String retryQueue,
            Map<String, Object> oficioType,
            Map<String, Object> destinatarioResolvido,
            Map<String, Object> minutaGovernada,
            List<String> channelChain,
            String currentChannel,
            Map<String, Object> caixaInstitucional,
            Map<String, Object> meshMirror,
            Map<String, Object> externalDispatch,
            List<Map<String, Object>> channelConfirmations,
            Map<String, Object> cartorioAcknowledgement,
            Map<String, Object> reconciliationMaterialized,
            List<Map<String, Object>> statusTrail
    ) {
        private State withMutation(String currentStatus,
                                   int attempts,
                                   Instant updatedAt,
                                   Instant nextRetryAt,
                                   String auditHash,
                                   List<String> channelChain,
                                   String currentChannel,
                                   Map<String, Object> caixaInstitucional,
                                   Map<String, Object> meshMirror,
                                   Map<String, Object> externalDispatch,
                                   List<Map<String, Object>> channelConfirmations,
                                   Map<String, Object> cartorioAcknowledgement,
                                   Map<String, Object> reconciliationMaterialized,
                                   Map<String, Object> minutaGovernada,
                                   List<Map<String, Object>> statusTrail) {
            return new State(
                    executionId,
                    actorLane,
                    processoId,
                    operationCode,
                    partnerSystem,
                    templateCode,
                    requiresConfirmation,
                    currentStatus,
                    attempts,
                    retryLimit,
                    createdAt,
                    updatedAt,
                    nextRetryAt,
                    idempotencyKey,
                    routeHash,
                    auditHash,
                    confirmationQueue,
                    errorQueue,
                    retryQueue,
                    oficioType,
                    destinatarioResolvido,
                    minutaGovernada,
                    channelChain,
                    currentChannel,
                    caixaInstitucional,
                    meshMirror,
                    externalDispatch,
                    channelConfirmations,
                    cartorioAcknowledgement,
                    reconciliationMaterialized,
                    statusTrail
            );
        }
    }
}
