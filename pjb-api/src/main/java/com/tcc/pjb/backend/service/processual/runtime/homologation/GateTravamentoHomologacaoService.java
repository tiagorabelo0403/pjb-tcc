package com.tcc.pjb.backend.service.processual.runtime.homologation;

import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.dto.processual.runtime.homologation.ProcessualHomologationBlockerDetailResponse;
import com.tcc.pjb.backend.model.dto.processual.runtime.homologation.ProcessualHomologationGateStatusResponse;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.service.processual.runtime.guard.PjbOperationalGuardException;

@Service
public class GateTravamentoHomologacaoService {

    public static final String HOMOLOGAR_ACORDO_DIVORCIO = "HOMOLOGAR_ACORDO_DIVORCIO";

    private final InstitutionalGateStateRepository institutionalGateStateRepository;

    public GateTravamentoHomologacaoService(InstitutionalGateStateRepository institutionalGateStateRepository) {
        this.institutionalGateStateRepository = Objects.requireNonNull(institutionalGateStateRepository, "institutionalGateStateRepository");
    }

    public ProcessualHomologationGateStatusResponse avaliar(Long processoId, String operationCode) {
        Instant now = Instant.now();
        String normalizedOperation = normalizeOperationCode(operationCode);
        boolean operationSensivel = isSensitive(normalizedOperation);
        if (!operationSensivel || processoId == null) {
            return new ProcessualHomologationGateStatusResponse(
                    processoId,
                    normalizedOperation,
                    operationSensivel,
                    false,
                    List.of(),
                    List.of(),
                    Hashes.sha256Hex(String.valueOf(processoId) + "|" + normalizedOperation + "|SEM_GATE|" + now),
                    now
            );
        }
        List<ProcessualHomologationBlockerDetailResponse> details = institutionalGateStateRepository.findByProcessoId(processoId).stream()
                .filter(InstitutionalGateState::bloqueado)
                .map(this::toDetail)
                .toList();
        List<String> blockerCodes = details.stream().map(ProcessualHomologationBlockerDetailResponse::blockerCode).distinct().toList();
        return new ProcessualHomologationGateStatusResponse(
                processoId,
                normalizedOperation,
                true,
                !details.isEmpty(),
                blockerCodes,
                details,
                Hashes.sha256Hex(processoId + "|" + normalizedOperation + "|" + blockerCodes + "|" + now),
                now
        );
    }

    public boolean bloqueia(Long processoId, String operationCode) {
        return avaliar(processoId, operationCode).blocked();
    }

    public void exigirHomologacaoLiberada(Long processoId, String operationCode) {
        var status = avaliar(processoId, operationCode);
        if (status.blocked()) {
            String code = status.blockerCodes().isEmpty() ? "GATE_PROCESSUAL_PENDENTE" : status.blockerCodes().getFirst();
            String message = status.details().isEmpty() ? "Operação bloqueada por gate processual." : status.details().getFirst().descricao();
            throw new PjbOperationalGuardException(code, message);
        }
    }

    private ProcessualHomologationBlockerDetailResponse toDetail(InstitutionalGateState gate) {
        return new ProcessualHomologationBlockerDetailResponse(
                blockerCode(gate.gateCode()),
                gate.gateCode(),
                categoria(gate.gateCode()),
                descricao(gate.gateCode(), gate.motivo()),
                gate.expedicaoUuid(),
                gate.bloqueado(),
                gate.createdAt(),
                gate.updatedAt(),
                gate.justificativas()
        );
    }

    private String blockerCode(String gateCode) {
        if (gateCode == null || gateCode.isBlank()) {
            return "GATE_PROCESSUAL_PENDENTE";
        }
        String normalized = gateCode.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("GATE_MP_")) {
            return "GATE_MP_VISTA_PENDENTE";
        }
        if (normalized.startsWith("GATE_DEFENSORIA_")) {
            return "GATE_DEFENSORIA_CIENCIA_PENDENTE";
        }
        if (normalized.startsWith("GATE_PERITO_") || normalized.startsWith("GATE_ESTUDO_PSICOSSOCIAL") || normalized.startsWith("GATE_ORGAO_TECNICO")) {
            return "GATE_LAUDO_TECNICO_PENDENTE";
        }
        if (normalized.startsWith("GATE_UNIDADE_PRISIONAL") || normalized.startsWith("GATE_APRESENTACAO_REU_PRESO")) {
            return "GATE_CUSTODIA_PENDENTE";
        }
        if (normalized.startsWith("GATE_CONTADORIA")) {
            return "GATE_CONTADORIA_PENDENTE";
        }
        if (normalized.startsWith("GATE_COOPERACAO_")) {
            return "GATE_COOPERACAO_JUDICIAL_PENDENTE";
        }
        if (normalized.startsWith("GATE_CONSELHO_TUTELAR")) {
            return "GATE_REDE_PROTETIVA_PENDENTE";
        }
        return normalized + "_PENDENTE";
    }

    private String categoria(String gateCode) {
        if (gateCode == null || gateCode.isBlank()) {
            return "PROCESSUAL";
        }
        String normalized = gateCode.toUpperCase(Locale.ROOT);
        if (normalized.startsWith("GATE_MP_")) {
            return "MINISTERIO_PUBLICO";
        }
        if (normalized.startsWith("GATE_DEFENSORIA_")) {
            return "DEFENSORIA_PUBLICA";
        }
        if (normalized.startsWith("GATE_PERITO_") || normalized.startsWith("GATE_ESTUDO_PSICOSSOCIAL") || normalized.startsWith("GATE_ORGAO_TECNICO")) {
            return "PROVA_TECNICA";
        }
        if (normalized.startsWith("GATE_UNIDADE_PRISIONAL") || normalized.startsWith("GATE_APRESENTACAO_REU_PRESO")) {
            return "CUSTODIA";
        }
        if (normalized.startsWith("GATE_CONTADORIA")) {
            return "CONTADORIA";
        }
        if (normalized.startsWith("GATE_COOPERACAO_")) {
            return "COOPERACAO_JUDICIAL";
        }
        if (normalized.startsWith("GATE_CONSELHO_TUTELAR")) {
            return "REDE_PROTETIVA";
        }
        return "PROCESSUAL";
    }

    private String descricao(String gateCode, String fallback) {
        String blockerCode = blockerCode(gateCode);
        return switch (blockerCode) {
            case "GATE_MP_VISTA_PENDENTE" -> "Homologação bloqueada: vista obrigatória do Ministério Público ainda pendente.";
            case "GATE_DEFENSORIA_CIENCIA_PENDENTE" -> "Homologação bloqueada: ciência institucional da Defensoria ainda pendente.";
            case "GATE_LAUDO_TECNICO_PENDENTE" -> "Homologação bloqueada: manifestação ou laudo técnico obrigatório ainda não foi consolidado.";
            case "GATE_CUSTODIA_PENDENTE" -> "Ato bloqueado: comunicação à unidade custodiante ou apresentação do custodiado ainda pendente.";
            case "GATE_CONTADORIA_PENDENTE" -> "Ato bloqueado: retorno obrigatório da contadoria judicial ainda pendente.";
            case "GATE_COOPERACAO_JUDICIAL_PENDENTE" -> "Ato bloqueado: cooperação judicial essencial ainda não foi concluída.";
            case "GATE_REDE_PROTETIVA_PENDENTE" -> "Ato bloqueado: comunicação obrigatória à rede protetiva ainda pendente.";
            default -> fallback == null || fallback.isBlank() ? "Marco processual sensível condicionado ao cumprimento institucional pendente." : fallback;
        };
    }

    private String normalizeOperationCode(String operationCode) {
        if (operationCode == null || operationCode.isBlank()) {
            return "UNSPECIFIED";
        }
        return operationCode.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isSensitive(String operationCode) {
        if (operationCode == null || operationCode.isBlank()) {
            return false;
        }
        return operationCode.equals(HOMOLOGAR_ACORDO_DIVORCIO) || operationCode.contains("HOMOLOG")
                || operationCode.contains("SENTENCA")
                || operationCode.contains("TRANSITO")
                || operationCode.contains("ARQUIV")
                || operationCode.contains("BAIXA");
    }
}
