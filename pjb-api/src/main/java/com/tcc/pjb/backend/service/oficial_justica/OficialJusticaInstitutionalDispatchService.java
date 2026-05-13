package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryDispatchResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain.InstitutionalDeliveryJob;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalExternalIntegrationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.integration.application.InstitutionalExternalIntegrationApplicationService.TraceableExternalDispatchResult;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class OficialJusticaInstitutionalDispatchService {

    private final InstitutionalExternalIntegrationApplicationService externalIntegrationApplicationService;

    public OficialJusticaInstitutionalDispatchService(InstitutionalExternalIntegrationApplicationService externalIntegrationApplicationService) {
        this.externalIntegrationApplicationService = Objects.requireNonNull(externalIntegrationApplicationService, "externalIntegrationApplicationService");
    }

    public Map<String, Object> dispatch(Processo processo,
                                        Usuario usuario,
                                        WorkItem encaminhamentoCartorio,
                                        String executionId,
                                        String operationCode,
                                        Map<String, Object> oficioType,
                                        Map<String, Object> destinatarioResolvido,
                                        Map<String, Object> minutaGovernada) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(usuario, "usuario");
        Objects.requireNonNull(encaminhamentoCartorio, "encaminhamentoCartorio");
        String safeExecutionId = Objects.requireNonNull(executionId, "executionId").trim();
        Map<String, Object> safeType = immutableMap(oficioType);
        Map<String, Object> safeRecipient = immutableMap(destinatarioResolvido);
        Map<String, Object> safeTemplate = immutableMap(minutaGovernada);
        DestinatarioInstitucionalKind recipientKind = resolveRecipientKind(safeRecipient);
        PapelProcessualInstitucional papel = resolvePapel(safeRecipient);
        List<CanalComunicacaoInstitucional> channelChain = resolveChannelChain(safeType, recipientKind);
        String unidadeCodigo = firstNonBlank(stringValue(safeRecipient.get("unidadeInstitucionalCodigo")), processo.getUnidadeJudiciariaCodigo(), processo.getTribunalCodigoRoteado(), usuario.getUf() + ":" + usuario.getComarca(), "PJB:OFICIAL_JUSTICA");
        String caixaCodigo = firstNonBlank(encaminhamentoCartorio.getInboxKey(), encaminhamentoCartorio.getQueueCode(), "OFICIAL:OFICIO:CAIXA:" + unidadeCodigo);
        Instant now = Instant.now();
        InstitutionalDeliveryJob job = new InstitutionalDeliveryJob(
                "OFICIAL-OFICIO-JOB-" + Hashes.sha256HexPrefix(safeExecutionId + "|" + channelChain + "|" + unidadeCodigo + "|" + caixaCodigo, 18).toUpperCase(Locale.ROOT),
                safeExecutionId,
                processo.getId(),
                processo.getNumeroProcesso(),
                unidadeCodigo,
                caixaCodigo,
                recipientKind,
                papel,
                channelChain,
                0,
                StatusEntregaInstitucional.PENDENTE,
                0,
                5,
                now,
                now,
                now,
                null,
                null,
                "oficial:institutional-dispatch:" + safeExecutionId,
                null,
                null,
                null,
                List.of("oficio_oficial", normalize(operationCode, "OFICIO_OFICIAL_JUSTICA"), recipientKind.name()),
                null
        );
        Map<String, Object> caixaInstitucional = caixaInstitucional(unidadeCodigo, caixaCodigo, encaminhamentoCartorio, safeExecutionId);
        if (job.currentChannel() == CanalComunicacaoInstitucional.PJB_INBOX) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("dispatchJob", deliveryJobMap(job));
            out.put("channelChain", channelChain.stream().map(Enum::name).toList());
            out.put("currentChannel", job.currentChannel().name());
            out.put("caixaInstitucional", caixaInstitucional);
            out.put("meshMirror", Map.of());
            out.put("externalDispatch", Map.of(
                    "status", "LOCAL_BOX_READY",
                    "providerStatus", "PJB_INBOX_READY",
                    "deliveryStatus", "DISPONIBILIZADA_CAIXA_INSTITUCIONAL",
                    "providerReference", safeExecutionId
            ));
            out.put("reconciliationMaterialized", materializedReconciliation(safeExecutionId, safeRecipient, safeTemplate, "PJB_INBOX_READY", null, false));
            out.put("statusSuggested", "DISPONIBILIZADA_CAIXA_INSTITUCIONAL");
            return Collections.unmodifiableMap(out);
        }
        TraceableExternalDispatchResult traceable = externalIntegrationApplicationService.despacharRastreavel(job);
        InstitutionalDeliveryDispatchResult deliveryResult = traceable.deliveryResult();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("dispatchJob", deliveryJobMap(job));
        out.put("channelChain", channelChain.stream().map(Enum::name).toList());
        out.put("currentChannel", job.currentChannel().name());
        out.put("caixaInstitucional", caixaInstitucional);
        out.put("meshMirror", traceable.meshMirror() != null ? meshMirrorMap(traceable.meshMirror()) : Map.of());
        out.put("externalDispatch", externalDispatchMap(traceable, deliveryResult));
        out.put("reconciliationMaterialized", materializedReconciliation(
                safeExecutionId,
                safeRecipient,
                safeTemplate,
                deliveryResult.providerStatus(),
                deliveryResult.detail(),
                deliveryResult.status() == com.tcc.pjb.backend.model.entity.enums.StatusTentativaEntregaInstitucional.FALHA_TERMINAL
        ));
        out.put("statusSuggested", switch (deliveryResult.status()) {
            case ENTREGUE -> "ENTREGUE_EXTERNAMENTE";
            case ENCAMINHADA -> "ENCAMINHADA_MALHA_EXTERNA";
            case RETRY_AGENDADO -> "RETENTATIVA_PROGRAMADA";
            case FALHA_TERMINAL -> "ERRO_ENTREGA";
            default -> "ENCAMINHADA_MALHA_EXTERNA";
        });
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> deliveryJobMap(InstitutionalDeliveryJob job) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("jobId", job.jobId());
        out.put("expedicaoUuid", job.expedicaoUuid());
        out.put("processoId", job.processoId());
        out.put("processoNumero", job.processoNumero());
        out.put("unidadeCodigo", job.unidadeCodigo());
        out.put("caixaCodigo", job.caixaCodigo());
        out.put("destinatarioKind", job.destinatarioKind().name());
        out.put("papelProcessual", job.papelProcessual().name());
        out.put("channelChain", job.channelChain().stream().map(Enum::name).toList());
        out.put("currentChannel", job.currentChannel().name());
        out.put("correlationKey", job.correlationKey());
        out.put("hashIntegridade", job.hashIntegridade());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> caixaInstitucional(String unidadeCodigo, String caixaCodigo, WorkItem encaminhamentoCartorio, String executionId) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("expectedCartorioAck", Boolean.TRUE);
        out.put("executionId", executionId);
        out.put("unidadeCodigo", unidadeCodigo);
        out.put("caixaCodigo", caixaCodigo);
        out.put("workItemId", encaminhamentoCartorio.getId());
        out.put("queueCode", encaminhamentoCartorio.getQueueCode());
        out.put("inboxKey", encaminhamentoCartorio.getInboxKey());
        out.put("dueAt", encaminhamentoCartorio.getDueAt() != null ? encaminhamentoCartorio.getDueAt().toString() : null);
        out.put("recebimentoTitle", encaminhamentoCartorio.getTitulo());
        out.put("recebimentoRoute", firstNonBlank(encaminhamentoCartorio.getInboxKey(), encaminhamentoCartorio.getQueueCode(), unidadeCodigo));
        return immutableMap(out);
    }

    private Map<String, Object> meshMirrorMap(com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain.InstitutionalCommunicationMeshMirrorResult mirror) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mirrorId", mirror.mirrorId());
        out.put("expedicaoUuid", mirror.expedicaoUuid());
        out.put("channel", mirror.channel());
        out.put("routingKey", mirror.routingKey());
        out.put("externalSystemCode", mirror.externalSystemCode());
        out.put("destinationBox", mirror.destinationBox());
        out.put("meshOrgKey", mirror.meshOrgKey());
        out.put("meshUnitKey", mirror.meshUnitKey());
        out.put("payloadDigest", mirror.payloadDigestSha256());
        out.put("payloadSignature", mirror.payloadSignatureHmacSha256());
        out.put("outboxEventId", mirror.outboxEventId());
        out.put("mirroredAt", mirror.mirroredAt() != null ? mirror.mirroredAt().toString() : null);
        out.put("integrityHash", mirror.hashIntegridade());
        return immutableMap(out);
    }

    private Map<String, Object> externalDispatchMap(TraceableExternalDispatchResult traceable,
                                                    InstitutionalDeliveryDispatchResult deliveryResult) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (traceable.dispatch() != null) {
            out.put("dispatchId", traceable.dispatch().dispatchId());
            out.put("provider", traceable.dispatch().provider());
            out.put("routingKey", traceable.dispatch().routingKey());
            out.put("eventType", traceable.dispatch().eventType());
            out.put("status", traceable.dispatch().status().name());
            out.put("providerReference", traceable.dispatch().providerReference());
            out.put("requestHash", traceable.dispatch().payloadHash());
            out.put("createdAt", traceable.dispatch().createdAt() != null ? traceable.dispatch().createdAt().toString() : null);
            out.put("updatedAt", traceable.dispatch().updatedAt() != null ? traceable.dispatch().updatedAt().toString() : null);
        }
        out.put("deliveryStatus", deliveryResult.status().name());
        out.put("providerStatus", deliveryResult.providerStatus());
        out.put("detail", deliveryResult.detail());
        out.put("transientFailure", deliveryResult.transientFailure());
        out.put("failureReason", deliveryResult.failureReason() != null ? deliveryResult.failureReason().name() : null);
        out.put("providerReference", firstNonBlank(deliveryResult.providerReference(), stringValue(out.get("providerReference"))));
        return immutableMap(out);
    }

    private Map<String, Object> materializedReconciliation(String executionId,
                                                           Map<String, Object> destinatario,
                                                           Map<String, Object> minuta,
                                                           String providerStatus,
                                                           String detail,
                                                           boolean divergent) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "OFICIAL_OFICIO_RECONCILIATION_MATERIALIZED");
        out.put("executionId", executionId);
        out.put("providerStatus", providerStatus);
        out.put("detail", detail);
        out.put("divergent", divergent);
        out.put("destinatarioHash", Hashes.sha256Hex(String.valueOf(destinatario)));
        out.put("contentHash", stringValue(minuta.get("contentHash")));
        out.put("materializedAt", Instant.now().toString());
        out.put("reconciliationHash", Hashes.sha256Hex(executionId + '|' + providerStatus + '|' + detail + '|' + destinatario + '|' + minuta));
        return immutableMap(out);
    }

    private List<CanalComunicacaoInstitucional> resolveChannelChain(Map<String, Object> oficioType,
                                                                    DestinatarioInstitucionalKind recipientKind) {
        LinkedHashSet<CanalComunicacaoInstitucional> chain = new LinkedHashSet<>();
        if (recipientKind.admiteCanalNacionalPessoal()) {
            chain.add(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO);
            chain.add(CanalComunicacaoInstitucional.DJEN);
        }
        if (recipientKind == DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL) {
            chain.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            chain.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
        }
        if (recipientKind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA
                || recipientKind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL
                || recipientKind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL
                || recipientKind == DestinatarioInstitucionalKind.POLICIA_PENAL
                || recipientKind == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL
                || recipientKind == DestinatarioInstitucionalKind.CONSELHO_TUTELAR
                || recipientKind == DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO
                || recipientKind == DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO
                || recipientKind == DestinatarioInstitucionalKind.JUIZO_DEPRECADO) {
            chain.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
            chain.add(CanalComunicacaoInstitucional.PORTAL_LEGADO_INTEGRADO);
        }
        Object modes = oficioType.get("deliveryModes");
        if (modes instanceof List<?> list) {
            for (Object item : list) {
                String mode = normalize(stringValue(item), null);
                if (mode == null) {
                    continue;
                }
                switch (mode) {
                    case "CAIXA_INSTITUCIONAL_PJB", "JUNTADA_CARTORARIA", "ESPERA_CONFIRMACAO_DESTINATARIO" -> chain.add(CanalComunicacaoInstitucional.PJB_INBOX);
                    case "REMESSA_INTEROPERAVEL", "INTEROP_HUB_SOBERANO" -> {
                        chain.add(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL);
                        chain.add(CanalComunicacaoInstitucional.PORTAL_LEGADO_INTEGRADO);
                    }
                    case "CONFIRMACAO_MANUAL_CARTORIO" -> chain.add(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL);
                    default -> chain.add(CanalComunicacaoInstitucional.PJB_INBOX);
                }
            }
        }
        chain.add(CanalComunicacaoInstitucional.PJB_INBOX);
        return List.copyOf(chain);
    }

    private DestinatarioInstitucionalKind resolveRecipientKind(Map<String, Object> destinatarioResolvido) {
        DestinatarioInstitucionalKind kind = DestinatarioInstitucionalKind.fromTexto(stringValue(destinatarioResolvido.get("destinatarioInstitucionalKind")));
        return kind != null ? kind : DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO;
    }

    private PapelProcessualInstitucional resolvePapel(Map<String, Object> destinatarioResolvido) {
        String raw = stringValue(destinatarioResolvido.get("papelProcessualInstitucional"));
        if (raw != null) {
            try {
                return PapelProcessualInstitucional.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return PapelProcessualInstitucional.DESTINATARIO_OFICIO;
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

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> immutableMap(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            if (value instanceof Map<?, ?> nested) {
                LinkedHashMap<String, Object> nestedOut = new LinkedHashMap<>();
                nested.forEach((nestedKey, nestedValue) -> {
                    if (nestedKey != null && nestedValue != null) {
                        nestedOut.put(String.valueOf(nestedKey), nestedValue);
                    }
                });
                if (!nestedOut.isEmpty()) {
                    out.put(String.valueOf(key), Map.copyOf(nestedOut));
                }
                return;
            }
            if (value instanceof List<?> list) {
                List<Object> filtered = new ArrayList<>();
                for (Object item : list) {
                    if (item != null) {
                        filtered.add(item);
                    }
                }
                out.put(String.valueOf(key), List.copyOf(filtered));
                return;
            }
            out.put(String.valueOf(key), value);
        });
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }
}
