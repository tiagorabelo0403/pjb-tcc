package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIdentityBaseProfileResponse;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalEntrySummaryResponse(
        Long usuarioId,
        String nomeUsuario,
        String tipoUsuario,
        NationalCommunicationInstitutionalIdentityBaseProfileResponse identidadeBase,
        boolean possuiAmbientePessoal,
        boolean possuiAmbienteInstitucional,
        List<NationalCommunicationInstitutionalEntryContextResponse> contextos,
        NationalCommunicationInstitutionalEntryContextResponse contextoPreferencial,
        NationalCommunicationInstitutionalOperationalProfileResponse perfilOperacionalAtivo,
        NationalCommunicationInstitutionalEntryActivationResponse ativacaoPosLogin,
        PjbAuthenticatedSessionResponse sessaoAutenticada,
        Instant generatedAt
) {}