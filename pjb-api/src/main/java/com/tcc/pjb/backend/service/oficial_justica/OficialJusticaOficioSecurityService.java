package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.dto.profile.operational.OficialJusticaOficioRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaOficioSecurityService {

    private final PjbAuthorizationService authorizationService;
    private final OficialJusticaProcessoVinculoService vinculoService;
    private final PjbTimeService timeService;

    public OficialJusticaOficioSecurityService(PjbAuthorizationService authorizationService,
                                               OficialJusticaProcessoVinculoService vinculoService,
                                               PjbTimeService timeService) {
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.vinculoService = Objects.requireNonNull(vinculoService);
        this.timeService = Objects.requireNonNull(timeService);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> envelope(Processo processo, Usuario usuario, String operationCode) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String op = normalize(operationCode, "OFICIO_OFICIAL_JUSTICA");
        boolean officialProfile = isOficial(usuario);
        AuthzDecision readDecision = processo == null ? AuthzDecision.deny("processo_nulo", "manual") : authorizationService.canReadProcesso(processo);
        List<WorkItem> vinculos = processo == null || usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null
                ? List.of()
                : vinculoService.vinculosDiretosProcesso(processo.getId(), usuario.getId(), usuario.getTipoUsuario(), 200);
        List<WorkItem> ativos = vinculos.stream().filter(this::isOpen).toList();
        List<WorkItem> concluidos = vinculos.stream().filter(this::isDone).toList();
        boolean processoEncerrado = isProcessoClosed(processo);
        boolean postCompletionLock = ativos.isEmpty() && !vinculos.isEmpty();
        boolean layer1 = officialProfile && !vinculos.isEmpty();
        boolean layer2 = readDecision.allowed() && !processoEncerrado;
        boolean layer3 = !postCompletionLock;
        boolean writeAllowed = layer1 && layer2 && layer3;
        String blockedReason = writeAllowed ? null : resolveBlockedReason(officialProfile, vinculos, readDecision, processoEncerrado, postCompletionLock);

        out.put("operationCode", op);
        out.put("securityLevel", "GOVERNAMENTAL_TRIPLA_CAMADA");
        out.put("generatedAt", timeService.nowUtc());
        out.put("readDecision", mapDecision(readDecision));
        out.put("hasDirectOperationalVinculo", !vinculos.isEmpty());
        out.put("activeOperationalItems", ativos.size());
        out.put("completedOperationalItems", concluidos.size());
        out.put("writeAllowed", writeAllowed);
        out.put("sendIntoProcessAllowed", writeAllowed);
        out.put("sendMode", writeAllowed ? "READ_WRITE_WITH_OPERATIONAL_CHAIN" : "READ_ONLY_OR_BLOCKED");
        if (blockedReason != null && !blockedReason.isBlank()) {
            out.put("blockedReason", blockedReason);
        }
        out.put("lockedAfterCompletion", postCompletionLock);
        out.put("processoEncerrado", processoEncerrado);
        out.put("pendingTemplates", ativos.stream().map(WorkItem::getTemplateCode).filter(Objects::nonNull).distinct().toList());
        out.put("securityLayers", List.of(
                layer("LAYER_1_IDENTIDADE_E_VINCULO", layer1, layer1 ? "vinculo_operacional_materializado" : "oficial_sem_vinculo_operacional_materializado"),
                layer("LAYER_2_AUTORIZACAO_E_ESTADO_PROCESSUAL", layer2, layer2 ? "autorizacao_e_fluxo_ativo" : readDecision.allowed() ? "processo_sem_estado_escrevivel" : readDecision.reason()),
                layer("LAYER_3_LOCK_POS_CUMPRIMENTO", layer3, layer3 ? "janela_operacional_ainda_aberta" : "todas_as_ordens_do_oficial_foram_concluidas_ou_baixadas")
        ));
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    public void enforceCanSendIntoProcess(Processo processo, Usuario usuario, String operationCode) {
        Map<String, Object> envelope = envelope(processo, usuario, operationCode);
        if (!Boolean.TRUE.equals(envelope.get("sendIntoProcessAllowed"))) {
            throw new RegraNegocioException("Envio bloqueado no processo para o Oficial de Justiça. Motivo: " + envelope.getOrDefault("blockedReason", "operacao_nao_autorizada") + '.');
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> originalOnlyEnvelope(OficialJusticaOficioRequest request,
                                                    Map<String, Object> minutaGovernada,
                                                    boolean directIntoProcess) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean hasInlineMedia = request != null && request.midiaInline() != null && !request.midiaInline().isEmpty();
        boolean hasSupportingDocs = request != null && (
                hasAny(request.provasDocumentais())
                        || hasAny(request.documentosPessoais())
                        || hasAny(request.documentosRepresentacao())
                        || hasAny(request.documentosAnexados())
        );
        boolean hasGovernedBody = minutaGovernada != null
                && minutaGovernada.get("contentHash") != null
                && minutaGovernada.get("renderedBody") != null;
        boolean originalOnly = directIntoProcess && !hasInlineMedia && !hasSupportingDocs && hasGovernedBody;
        out.put("directIntoProcess", directIntoProcess);
        out.put("originalOnlyGoverned", originalOnly);
        out.put("hasInlineMedia", hasInlineMedia);
        out.put("hasSupportingDocs", hasSupportingDocs);
        out.put("hasGovernedBody", hasGovernedBody);
        out.put("blockedReason", originalOnly ? null : resolveOriginalOnlyBlockedReason(hasInlineMedia, hasSupportingDocs, hasGovernedBody, directIntoProcess));
        out.put("securityMode", "OFICIO_ORIGINAL_GOVERNADO_ONLY");
        return Map.copyOf(out.entrySet().stream().filter(entry -> entry.getValue() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new)));
    }

    @Transactional(readOnly = true)
    public void enforceOriginalOnlyForDirectProcessSubmission(OficialJusticaOficioRequest request,
                                                              Map<String, Object> minutaGovernada,
                                                              boolean directIntoProcess) {
        Map<String, Object> envelope = originalOnlyEnvelope(request, minutaGovernada, directIntoProcess);
        if (!Boolean.TRUE.equals(envelope.get("originalOnlyGoverned"))) {
            throw new RegraNegocioException("Envio direto ao processo bloqueado. O Oficial só pode protocolar o ofício original governado, sem mídia inline nem documentos adicionais. Motivo: " + envelope.getOrDefault("blockedReason", "oficio_original_nao_governado") + '.');
        }
    }

    private Map<String, Object> mapDecision(AuthzDecision decision) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("allowed", decision != null && decision.allowed());
        out.put("reason", decision == null || decision.reason() == null || decision.reason().isBlank() ? "nao_avaliado" : decision.reason());
        out.put("policyVersion", decision == null || decision.policyVersion() == null || decision.policyVersion().isBlank() ? "manual" : decision.policyVersion());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> layer(String code, boolean passed, String rationale) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", code);
        out.put("passed", passed);
        out.put("rationale", rationale);
        return Collections.unmodifiableMap(out);
    }

    private boolean isProcessoClosed(Processo processo) {
        if (processo == null) {
            return true;
        }
        StatusProcesso status = processo.getStatusProcesso();
        return status != null && (status.isArquivadoOuBaixado() || status.isTransitado() || status.isEncerrado());
    }

    private String resolveBlockedReason(boolean officialProfile,
                                        List<WorkItem> vinculos,
                                        AuthzDecision readDecision,
                                        boolean processoEncerrado,
                                        boolean postCompletionLock) {
        if (!officialProfile) {
            return "perfil_nao_oficial";
        }
        if (vinculos == null || vinculos.isEmpty()) {
            return "oficial_sem_nomeacao_ou_vinculo_direto";
        }
        if (readDecision == null || !readDecision.allowed()) {
            return readDecision == null ? "leitura_processual_indisponivel" : readDecision.reason();
        }
        if (processoEncerrado) {
            return "processo_em_estado_nao_escrevivel";
        }
        if (postCompletionLock) {
            return "lock_pos_cumprimento_integral_ativado";
        }
        return "bloqueio_operacional";
    }

    private boolean isOficial(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        return tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
    }

    private boolean isOpen(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.PENDENTE || item.getStatus() == WorkItemStatus.EM_EXECUCAO);
    }

    private boolean isDone(WorkItem item) {
        return item != null && (item.getStatus() == WorkItemStatus.CONCLUIDO || item.getStatus() == WorkItemStatus.CANCELADO);
    }

    private boolean hasAny(List<String> values) {
        return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private String resolveOriginalOnlyBlockedReason(boolean hasInlineMedia,
                                                    boolean hasSupportingDocs,
                                                    boolean hasGovernedBody,
                                                    boolean directIntoProcess) {
        if (!directIntoProcess) {
            return "envio_direto_nao_acionado";
        }
        if (hasInlineMedia) {
            return "midia_inline_nao_permitida_na_juntada_direta";
        }
        if (hasSupportingDocs) {
            return "documentos_adicionais_nao_permitidos_na_juntada_direta";
        }
        if (!hasGovernedBody) {
            return "oficio_original_sem_minuta_governada";
        }
        return "bloqueio_original_only";
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
