package com.tcc.pjb.backend.service.processual.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefensoriaInstitutionalCompetenceGuardServiceTest {

    @Mock
    private CurrentUserService currentUserService;

    private DefensoriaInstitutionalCompetenceGuardService service;

    @BeforeEach
    void setUp() {
        service = new DefensoriaInstitutionalCompetenceGuardService(currentUserService);
    }

    @Test
    void deveBloquearDefensoriaEstadualEmFluxoFederalContraUniao() {
        when(currentUserService.getRequired()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO));
        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tipoJustica(TipoJustica.FEDERAL.name())
                .parteRe("União")
                .tituloCaso("Ação previdenciária contra o INSS")
                .build();
        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "req-1",
                "OK",
                true,
                true,
                true,
                JudicialSystem.PJE,
                "TRF5",
                "Tribunal Regional Federal da 5ª Região",
                null,
                null,
                "JEF-CE",
                "Juizado Especial Federal",
                "COMUM_ORDINARIO",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                true,
                false,
                false,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );

        DefensoriaInstitutionalCompetenceGuardService.GuardDecision decision = service.analyzeInitialFiling(request, blueprint);

        assertEquals(DefensoriaInstitutionalCompetenceGuardService.Verdict.BLOCK_WITH_REDIRECT, decision.verdict());
        assertEquals(DefensoriaInstitutionalCompetenceGuardService.TargetSphere.FEDERAL, decision.targetSphere());
        assertTrue(decision.publicMessage().contains("DPU"));
        assertThrows(RegraNegocioException.class, decision::throwIfBlocked);
    }

    @Test
    void devePermitirDefensoriaFederalEmFluxoFederal() {
        when(currentUserService.getRequired()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL));
        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setTribunal("TRF5");
        processo.setVara("Juizado Especial Federal Previdenciário");
        processo.setParteReuNome("INSS");

        DefensoriaInstitutionalCompetenceGuardService.GuardDecision decision = service.analyzeProcessParticipation(processo);

        assertEquals(DefensoriaInstitutionalCompetenceGuardService.Verdict.ALLOW, decision.verdict());
        assertFalse(decision.blocked());
    }

    @Test
    void deveSinalizarRevisaoQuandoDefensoriaFederalEncontraFluxoEstadual() {
        when(currentUserService.getRequired()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL));
        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setTribunal("TJCE");
        processo.setVara("2ª Vara Cível");
        processo.setParteReuNome("Município de Fortaleza");

        DefensoriaInstitutionalCompetenceGuardService.GuardDecision decision = service.analyzeProcessParticipation(processo);

        assertEquals(DefensoriaInstitutionalCompetenceGuardService.Verdict.REVIEW, decision.verdict());
        assertTrue(decision.warnings().stream().anyMatch(item -> item.contains("estadual")));
    }

    private static Usuario usuario(TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setNome("Defensor");
        usuario.setEmail("defensor@pjb.test");
        return usuario;
    }
}
