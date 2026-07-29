package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Estrutura idêntica a {@link InstitutionalRecursoRequest}; mantida por coexistência do endpoint
 * legado {@code POST /api/v1/advogado/cockpit/processos/{id}/recurso}. A superfície canônica
 * {@code POST /api/v1/recursal/processos/{id}/recurso} consome {@link InstitutionalRecursoRequest}.
 * Remover junto com a Fatia 3 de {@code D-recursal-superficie-por-papel}, após zerar consumidores
 * do endpoint legado.
 */
public record AdvogadoRecursoRequest(
        @NotBlank @Size(max = 120) String tipoRecurso,
        @NotBlank String razoes,
        @NotBlank @Size(max = 4000) String fundamentacao,
        boolean pedidoEfeitoSuspensivo,
        boolean preparoDispensado,
        @Size(max = 2000) String observacoes
) {}
