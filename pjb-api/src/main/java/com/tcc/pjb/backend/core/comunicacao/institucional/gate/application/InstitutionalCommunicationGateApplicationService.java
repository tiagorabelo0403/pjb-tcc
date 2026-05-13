package com.tcc.pjb.backend.core.comunicacao.institucional.gate.application;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.audit.application.InstitutionalCommunicationAuditApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.infrastructure.InstitutionalGateStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.inbox.domain.InstitutionalInboxItem;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class InstitutionalCommunicationGateApplicationService {

    private final InstitutionalGateStateRepository repository;
    private final InstitutionalCommunicationAuditApplicationService auditService;

    public InstitutionalCommunicationGateApplicationService(InstitutionalGateStateRepository repository,
                                                            InstitutionalCommunicationAuditApplicationService auditService) {
        this.repository = Objects.requireNonNull(repository);
        this.auditService = Objects.requireNonNull(auditService);
    }

    public Optional<InstitutionalGateState> criarSeNecessario(InstitutionalInboxItem item) {
        if (!item.bloqueiaFluxo() || item.gateCode() == null || item.gateCode().isBlank()) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        List<String> justificativas = new ArrayList<>(item.justificativas());
        justificativas.add("gate_pendente_ciencia");
        InstitutionalGateState gate = new InstitutionalGateState(
                UUID.nameUUIDFromBytes((item.expedicaoUuid() + "|" + item.gateCode()).getBytes(StandardCharsets.UTF_8)).toString(),
                item.expedicaoUuid(),
                item.processoId(),
                item.processoNumero(),
                item.gateCode(),
                InstitutionalGateStatus.AGUARDANDO_CIENCIA,
                true,
                "Fluxo processual sensível condicionado ao cumprimento institucional.",
                null,
                now,
                now,
                null,
                justificativas,
                hash(item.expedicaoUuid(), item.gateCode(), InstitutionalGateStatus.AGUARDANDO_CIENCIA, now)
        );
        repository.save(gate);
        auditService.registrarGateCriado(item, gate);
        return Optional.of(gate);
    }

    public Optional<InstitutionalGateState> marcarCiencia(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        return repository.findByExpedicaoUuid(item.expedicaoUuid()).map(current -> {
            Instant now = Instant.now();
            List<String> justificativas = new ArrayList<>(current.justificativas());
            justificativas.add("ciencia_registrada");
            InstitutionalGateState updated = current.withStatus(
                    InstitutionalGateStatus.AGUARDANDO_CUMPRIMENTO,
                    "CIENCIA_INSTITUCIONAL",
                    now,
                    justificativas,
                    hash(current.expedicaoUuid(), current.gateCode(), InstitutionalGateStatus.AGUARDANDO_CUMPRIMENTO, now)
            );
            repository.save(updated);
            auditService.registrarGateTransicao(item, actor, updated, detalhe == null || detalhe.isBlank() ? "Ciência institucional registrada." : detalhe);
            return updated;
        });
    }

    public Optional<InstitutionalGateState> marcarCumprimento(InstitutionalInboxItem item, Usuario actor, String detalhe) {
        return repository.findByExpedicaoUuid(item.expedicaoUuid()).map(current -> {
            Instant now = Instant.now();
            List<String> justificativas = new ArrayList<>(current.justificativas());
            justificativas.add("cumprimento_institucional_concluido");
            InstitutionalGateState updated = current.withStatus(
                    InstitutionalGateStatus.LIBERADO,
                    "CUMPRIMENTO_INSTITUCIONAL",
                    now,
                    justificativas,
                    hash(current.expedicaoUuid(), current.gateCode(), InstitutionalGateStatus.LIBERADO, now)
            );
            repository.save(updated);
            auditService.registrarGateLiberado(item, actor, updated, detalhe == null || detalhe.isBlank() ? "Cumprimento institucional consolidado." : detalhe);
            return updated;
        });
    }

    public Optional<InstitutionalGateState> consultarPorExpedicao(String expedicaoUuid) {
        return repository.findByExpedicaoUuid(expedicaoUuid);
    }

    public List<InstitutionalGateState> consultarPorProcesso(Long processoId) {
        return repository.findByProcessoId(processoId);
    }

    public boolean possuiGateBloqueado(Long processoId) {
        return consultarPorProcesso(processoId).stream().anyMatch(InstitutionalGateState::bloqueado);
    }

    private String hash(String expedicaoUuid, String gateCode, InstitutionalGateStatus status, Instant instant) {
        return Hashes.sha256Hex(expedicaoUuid + "|" + gateCode + "|" + status.name() + "|" + instant.toString());
    }
}
