package com.tcc.pjb.backend.service.processual.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.exception.RegraNegocioException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class InstitutionalMaterialActionGuardServiceTest {

    private CurrentUserService currentUserService;
    private DefensoriaInstitutionalCompetenceGuardService defensoriaGuardService;
    private InstitutionalMaterialActionGuardService service;

    @BeforeEach
    void setUp() {
        currentUserService = Mockito.mock(CurrentUserService.class);
        defensoriaGuardService = Mockito.mock(DefensoriaInstitutionalCompetenceGuardService.class);
        service = new InstitutionalMaterialActionGuardService(currentUserService, defensoriaGuardService);
    }

    @Test
    void shouldBlockStateDelegateOnFederalCriminalFlow() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DELEGADO_POLICIA);
        usuario.setEnteFederativo(EnteFederativo.ESTADO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setRamo(RamoDireito.PENAL);
        processo.setRito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM);
        processo.setParteReuNome("União");
        processo.setAssunto("Investigação criminal federal");

        InstitutionalMaterialActionGuardService.GuardDecision decision = service.analyzeProcessAction(
                processo,
                InstitutionalMaterialActionGuardService.MaterialAction.DELEGADO_DILIGENCIA
        );

        assertEquals(InstitutionalMaterialActionGuardService.Verdict.BLOCK_WITH_REDIRECT, decision.verdict());
        assertEquals(InstitutionalMaterialActionGuardService.TargetSphere.FEDERAL, decision.targetSphere());
    }

    @Test
    void shouldBlockElectoralPromoterOnCommonCivilFlow() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PROMOTOR_ELEITORAL);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamo(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setAssunto("Cobrança contratual");

        InstitutionalMaterialActionGuardService.GuardDecision decision = service.analyzeProcessAction(
                processo,
                InstitutionalMaterialActionGuardService.MaterialAction.MINISTERIO_PUBLICO_MANIFESTACAO
        );

        assertEquals(InstitutionalMaterialActionGuardService.Verdict.BLOCK_WITH_REDIRECT, decision.verdict());
    }

    @Test
    void shouldAllowFederalProcuracyOnFederalDefenseFlow() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.PROCURADORIA_FEDERAL);
        usuario.setEnteFederativo(EnteFederativo.UNIAO);
        when(currentUserService.getRequired()).thenReturn(usuario);

        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setRamo(RamoDireito.ADMINISTRATIVO);
        processo.setRito(RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO);
        processo.setParteReuNome("INSS");
        processo.setAssunto("Responsabilidade da autarquia federal");

        InstitutionalMaterialActionGuardService.GuardDecision decision = service.analyzeProcessAction(
                processo,
                InstitutionalMaterialActionGuardService.MaterialAction.PROCURADORIA_CONTESTACAO
        );

        assertEquals(InstitutionalMaterialActionGuardService.Verdict.ALLOW, decision.verdict());
    }

    @Test
    void shouldDelegateDefensoriaDecisionWithoutDuplicatingRule() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.DEFENSOR_PUBLICO);
        usuario.setEnteFederativo(EnteFederativo.ESTADO);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(defensoriaGuardService.analyzeProcessParticipation(Mockito.any())).thenReturn(
                new DefensoriaInstitutionalCompetenceGuardService.GuardDecision(
                        DefensoriaInstitutionalCompetenceGuardService.Verdict.BLOCK_WITH_REDIRECT,
                        DefensoriaInstitutionalCompetenceGuardService.InstitutionalBranch.DEFENSORIA_ESTADUAL,
                        DefensoriaInstitutionalCompetenceGuardService.TargetSphere.FEDERAL,
                        java.util.List.of("Competência federal detectada"),
                        java.util.List.of("Redirecionar para a DPU"),
                        Map.of("source", "defensoria")
                )
        );

        Processo processo = new Processo();
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setRamo(RamoDireito.PREVIDENCIARIO);
        processo.setParteReuNome("INSS");

        assertThrows(RegraNegocioException.class, () -> service.requireAllowedForProcessAction(
                processo,
                InstitutionalMaterialActionGuardService.MaterialAction.DEFENSORIA_PETICAO
        ));
    }
}
