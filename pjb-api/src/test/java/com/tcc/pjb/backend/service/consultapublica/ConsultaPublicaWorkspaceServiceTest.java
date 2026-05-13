package com.tcc.pjb.backend.service.consultapublica;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.publico.PublicProcessoResumoCardDto;
import com.tcc.pjb.backend.model.dto.publico.SigiloUiDTO;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.publico.ProcessoPesquisaIdentidadePublicaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

class ConsultaPublicaWorkspaceServiceTest {

    @Test
    void workspaceForAnonymousUserFallsBackToPublicOnly() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(null);

        ConsultaPublicaWorkspaceService service = new ConsultaPublicaWorkspaceService(
                mock(NamedParameterJdbcTemplate.class),
                currentUserService,
                mock(MovimentacaoProcessualRepository.class),
                mock(EventoProcessualRepository.class),
                mock(DocumentoProcessualRepository.class),
                mock(ProcessoRepository.class),
                mock(DocumentoPaginaRepository.class),
                mock(WorkspaceProcessoEtiquetaRepository.class),
                mock(ProcessoPesquisaIdentidadePublicaService.class)
        );

        var response = service.workspace();

        assertThat(response.mode()).isEqualTo("PUBLIC_ONLY");
        assertThat(response.personalHub()).isNull();
        assertThat(response.meusProcessos()).isEmpty();
        assertThat(response.datasets().personalAvailable()).isFalse();
        assertThat(response.sections()).extracting("code").contains("CONSULTA_PUBLICA", "CONSULTA_POR_PESSOA", "ATOS_PUBLICOS");
        assertThat(response.journeys()).extracting("code").contains("PROCESS_NUMBER", "PERSON_NAME", "PERSON_CPF");
    }

    @Test
    void detailReflectsRestrictedPublicSummary() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoPesquisaIdentidadePublicaService publicService = mock(ProcessoPesquisaIdentidadePublicaService.class);
        DocumentoProcessualRepository documentoRepository = mock(DocumentoProcessualRepository.class);

        Processo processo = Processo.builder()
                .id(77L)
                .numeroUnificado("0001234-55.2026.8.06.0001")
                .nivelSigilo(NivelSigilo.SEGREDO_JUSTICA)
                .build();
        when(processoRepository.findByNumeroUnificado("0001234-55.2026.8.06.0001")).thenReturn(Optional.of(processo));
        when(processoRepository.findByNumeroProcesso("0001234-55.2026.8.06.0001")).thenReturn(Optional.empty());
        when(publicService.resumirProcessoPublico("0001234-55.2026.8.06.0001")).thenReturn(new PublicProcessoResumoCardDto(
                77L,
                "0001234-55.2026.8.06.0001",
                "TJCE",
                "CE",
                "Fortaleza",
                "1ª Vara",
                "ESTADUAL",
                "CIVIL",
                "Procedimento Comum",
                "Indenização",
                LocalDateTime.now().minusDays(30),
                LocalDateTime.now().minusDays(1),
                new SigiloUiDTO(true, 1, "Segredo de Justiça", "LOCK", "RED", "restrito"),
                true,
                "Resumo público disponível.",
                List.of(),
                "Exige credencial"
        ));
        when(documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(77L)).thenReturn(List.of());

        ConsultaPublicaWorkspaceService service = new ConsultaPublicaWorkspaceService(
                mock(NamedParameterJdbcTemplate.class),
                mock(CurrentUserService.class),
                mock(MovimentacaoProcessualRepository.class),
                mock(EventoProcessualRepository.class),
                documentoRepository,
                processoRepository,
                mock(DocumentoPaginaRepository.class),
                mock(WorkspaceProcessoEtiquetaRepository.class),
                publicService
        );

        var response = service.detail("0001234-55.2026.8.06.0001");

        assertThat(response.resumo().acessoRestrito()).isTrue();
        assertThat(response.actions()).extracting("code").contains("NOVA_BUSCA", "ACESSO_PESSOAL");
        assertThat(response.warnings()).anyMatch(item -> item.contains("restrições adicionais"));
    }
}
