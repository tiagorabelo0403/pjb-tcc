package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOperationalLifecycleStage;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.time.Instant;
import java.util.List;

public record InstitutionalOperationalLifecycle(
        String affiliationId,
        String requestId,
        DestinatarioInstitucionalKind destinatarioKind,
        InstitutionalOrganizationScope organizationScope,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String uf,
        String comarca,
        String cnpj,
        String esferaAdministrativa,
        List<String> ramosMateriais,
        List<String> abrangenciasTerritoriais,
        String dominioInstitucional,
        String autoridadeAderenteCargo,
        InstitutionalOperationalLifecycleStage lifecycleStage,
        boolean afiliacaoHomologada,
        boolean possuiNomeacoesAtivas,
        boolean prontoParaAtivacao,
        long totalNomeacoes,
        long totalNomeacoesAtivas,
        long totalCaixas,
        long totalCaixasAtivas,
        long totalAdministradores,
        List<String> caixasOperacionais,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        List<String> trilhosAutenticacao,
        List<String> eixosAutorizacao,
        List<String> fundamentos,
        Instant updatedAt
) {
}
