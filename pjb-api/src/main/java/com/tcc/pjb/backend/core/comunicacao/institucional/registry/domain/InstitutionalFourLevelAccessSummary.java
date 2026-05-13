package com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record InstitutionalFourLevelAccessSummary(
        Long userId,
        String userName,
        TipoUsuario tipoUsuario,
        String identityBaseCode,
        String affiliationId,
        DestinatarioInstitucionalKind destinatarioKind,
        InstitutionalOrganizationScope organizationScope,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        String caixaCodigo,
        String caixaNome,
        InstitutionalNominationRole nominationRole,
        FuncaoOperacionalInstitucional funcaoOperacional,
        InstitutionalProcessProfile processProfile,
        Set<CapacidadeCaixaInstitucional> capacidades,
        InstitutionalEntryLandingPanel landingPanel,
        boolean cadastroInstitucionalResolvido,
        boolean estruturaInternaResolvida,
        boolean pessoaVinculada,
        boolean contextoOperacionalAtivo,
        boolean plantaoAtivo,
        boolean substituicaoAtiva,
        boolean delegacaoAtiva,
        boolean autorizado,
        List<String> fundamentos,
        Instant generatedAt
) {
}
