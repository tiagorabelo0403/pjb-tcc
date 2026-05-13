package com.tcc.pjb.backend.ai.legalai.audit.domain;

import java.util.List;

public interface LegalAiAuditRepository {

    LegalAiAuditLog salvar(LegalAiAuditLog log);

    List<LegalAiAuditLog> listarPorEntidade(String entidadeTipo, String entidadeId);

    List<LegalAiAuditLog> listarPorAcao(LegalAiAuditAction acao);
}
