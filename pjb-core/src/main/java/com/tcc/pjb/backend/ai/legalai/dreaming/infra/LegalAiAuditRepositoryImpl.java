package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditAction;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditLog;
import com.tcc.pjb.backend.ai.legalai.audit.domain.LegalAiAuditRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LegalAiAuditRepositoryImpl implements LegalAiAuditRepository {

    private final LegalAiAuditJpaRepository jpaRepository;

    public LegalAiAuditRepositoryImpl(LegalAiAuditJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LegalAiAuditLog salvar(LegalAiAuditLog log) {
        return toDomain(jpaRepository.save(toEntity(log)));
    }

    @Override
    public List<LegalAiAuditLog> listarPorEntidade(String entidadeTipo, String entidadeId) {
        return jpaRepository.findByEntidadeTipoAndEntidadeId(entidadeTipo, entidadeId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<LegalAiAuditLog> listarPorAcao(LegalAiAuditAction acao) {
        return jpaRepository.findByAcaoOrderByCriadoEmDesc(acao.name())
                .stream().map(this::toDomain).toList();
    }

    private LegalAiAuditJpaEntity toEntity(LegalAiAuditLog log) {
        LegalAiAuditJpaEntity e = new LegalAiAuditJpaEntity();
        e.setId(log.id());
        e.setAcao(log.acao().name());
        e.setEntidadeTipo(log.entidadeTipo());
        e.setEntidadeId(log.entidadeId());
        e.setAtorId(log.atorId());
        e.setModelo(log.modelo());
        e.setInputTokens(log.inputTokens());
        e.setOutputTokens(log.outputTokens());
        e.setDetalhes(log.detalhes());
        e.setCriadoEm(log.criadoEm());
        return e;
    }

    private LegalAiAuditLog toDomain(LegalAiAuditJpaEntity e) {
        return new LegalAiAuditLog(
                e.getId(),
                LegalAiAuditAction.valueOf(e.getAcao()),
                e.getEntidadeTipo(),
                e.getEntidadeId(),
                e.getAtorId(),
                e.getModelo(),
                e.getInputTokens(),
                e.getOutputTokens(),
                e.getDetalhes(),
                e.getCriadoEm());
    }
}
