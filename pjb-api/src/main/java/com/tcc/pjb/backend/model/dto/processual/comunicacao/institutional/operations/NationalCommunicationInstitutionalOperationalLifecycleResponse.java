package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalOperationalLifecycleResponse(
        String affiliationId,
        String requestId,
        String destinatarioInstitucionalKind,
        String organizationScope,
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
        String lifecycleStage,
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
