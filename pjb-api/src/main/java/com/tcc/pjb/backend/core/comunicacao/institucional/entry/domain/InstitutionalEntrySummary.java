package com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;

public record InstitutionalEntrySummary(
        Long usuarioId,
        String nomeUsuario,
        TipoUsuario tipoUsuario,
        InstitutionalIdentityBaseProfile identidadeBase,
        boolean possuiAmbientePessoal,
        boolean possuiAmbienteInstitucional,
        List<InstitutionalEntryContext> contextos,
        InstitutionalEntryContext contextoPreferencial,
        Instant generatedAt
) {}
