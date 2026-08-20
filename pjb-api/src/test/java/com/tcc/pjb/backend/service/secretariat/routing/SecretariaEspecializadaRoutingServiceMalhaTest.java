package com.tcc.pjb.backend.service.secretariat.routing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.orchestration.SecretariatOperationalOrchestrationService;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePackFactory;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SecretariaEspecializadaRoutingServiceMalhaTest {

    @Mock
    private SecretariatRulePackFactory rulePackFactory;
    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SecretariatOperationalRoutingResolver operationalRoutingResolver;
    @Mock
    private SecretariatOperationalOrchestrationService operationalOrchestrationService;
    @Mock
    private JudicialTopologySegregationMeshService judicialTopologySegregationMeshService;
    @Mock
    private SecretariatInstitutionalVisibilityService visibilityService;
    @Mock
    private PjbAuthorizationService authorizationService;

    private SecretariaEspecializadaRoutingService service;

    @BeforeEach
    void setUp() {
        service = new SecretariaEspecializadaRoutingService(
                rulePackFactory,
                processoRepository,
                workItemRepository,
                currentUserService,
                operationalRoutingResolver,
                operationalOrchestrationService,
                judicialTopologySegregationMeshService,
                visibilityService,
                authorizationService);
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    @Test
    void magistradoComVinculoInstitucionalNoProcessoObtemAMalha() {
        Usuario magistrado = usuario(TipoUsuario.JUIZ);
        when(currentUserService.getRequired()).thenReturn(magistrado);
        doNothing().when(authorizationService).requireVinculoInstitucionalComProcesso(7L);

        service.malhaProcesso(7L);

        verify(visibilityService).requireProcessAccess(7L);
        verify(authorizationService).requireVinculoInstitucionalComProcesso(7L);
    }

    @Test
    void magistradoSemVinculoInstitucionalNoProcessoENegado() {
        Usuario magistrado = usuario(TipoUsuario.JUIZ);
        when(currentUserService.getRequired()).thenReturn(magistrado);
        doThrow(new AccessDeniedPjbException("Acesso negado à malha do processo: malha_institucional_sem_workitem_no_processo"))
                .when(authorizationService).requireVinculoInstitucionalComProcesso(7L);

        assertThatThrownBy(() -> service.malhaProcesso(7L)).isInstanceOf(AccessDeniedPjbException.class);
    }

    @Test
    void servidorNaoAcionaOVinculoInstitucionalAdicional() {
        Usuario servidor = usuario(TipoUsuario.SERVIDOR);
        when(currentUserService.getRequired()).thenReturn(servidor);

        service.malhaProcesso(7L);

        verify(visibilityService).requireProcessAccess(7L);
        verifyNoInteractions(authorizationService);
    }

    @Test
    void administradorNaoAcionaOVinculoInstitucionalAdicional() {
        Usuario admin = usuario(TipoUsuario.ADMINISTRADOR);
        when(currentUserService.getRequired()).thenReturn(admin);

        service.malhaProcesso(7L);

        verify(visibilityService).requireProcessAccess(7L);
        verifyNoInteractions(authorizationService);
    }
}
