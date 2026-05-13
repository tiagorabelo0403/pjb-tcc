package com.tcc.pjb.backend.service.secretariat.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.infra.scaling.JudicialScaleProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.topology.SecretariatSpecializationResolver;

class SecretariatInstitutionalVisibilityServiceTest {

    @Test
    void deveBloquearSecretariaSegundaInstanciaAoTentarVerProcessoDePrimeiraInstancia() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getRequired()).thenReturn(user(
                "SEGREDO;INSTANCIA:SEGUNDA_INSTANCIA;RAMO:ELEITORAL;TRIBUNAL:TRE-CE;SECRETARIA:SECRETARIA_TRE_CE_2G",
                "TRIBUNAL:TRE-CE;UJ:ZE87"
        ));

        SecretariatInstitutionalVisibilityService service = new SecretariatInstitutionalVisibilityService(
                currentUserService,
                mock(ProcessoRepository.class),
                mock(WorkItemRepository.class),
                mock(SecretariatOperationalRoutingResolver.class),
                mock(SecretariatInboxAccessService.class),
                new SecretariatSpecializationResolver()
        );

        SecretariatSpecializationResolver.SecretariatSpecializationProfile specialization = new SecretariatSpecializationResolver.SecretariatSpecializationProfile(
                "SECRETARIA_PRIMEIRA_INSTANCIA_ELEITORAL",
                "PRIMEIRA_INSTANCIA",
                "ELEITORAL",
                "PJB_ELEITORAL",
                "PJB Eleitoral | Primeira Instância",
                "pjb-eleitoral",
                "SECRETARIA_TRE_CE_1G",
                "Secretaria Judiciária Eleitoral de Primeira Instância - TRE-CE",
                "SEC:TRE-CE:ZE87:ELEITORAL:CE:FORTALEZA",
                List.of("RECEBIMENTO"),
                Map.of("institutionBirthMode", "INSTITUICAO_JUDICIARIA_RAIZ")
        );

        SecretariatOperationalRoutingProfile routing = new SecretariatOperationalRoutingProfile(
                "ROUTE",
                "ELEITORAL",
                "TRE-CE",
                "PRIMEIRA_INSTANCIA",
                "JUSTICA_ELEITORAL",
                "ELEITORAL",
                "SECRETARIA",
                "SECRETARIA_TRE_CE_1G",
                "REC",
                "SEC:TRE-CE:ZE87:ELEITORAL:CE:FORTALEZA",
                "SAN",
                "SEC:TRE-CE:ZE87:ELEITORAL:CE:FORTALEZA",
                "AUD",
                "SEC:TRE-CE:ZE87:ELEITORAL:CE:FORTALEZA",
                "EXEC",
                "SEC:TRE-CE:ZE87:ELEITORAL:CE:FORTALEZA",
                "HR",
                "ELEITORAL>TRE_CE>PRIMEIRA",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(12),
                30,
                true,
                true,
                false,
                false,
                List.of(),
                List.of(),
                specialization,
                JudicialScaleProfile.VARA_1G,
                Map.of("uf", "CE", "comarca", "Fortaleza", "unidadeJudiciariaCodigo", "ZE87")
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.requireRoutingAccess(routing));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void devePermitirSecretariaEspecializadaQuandoRecorteInstitucionalConfere() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getRequired()).thenReturn(user(
                "INSTANCIA:SEGUNDA_INSTANCIA;RAMO:ELEITORAL;TRIBUNAL:TRE-CE;SECRETARIA:SECRETARIA_TRE_CE_2G",
                "UJ:SECRETARIA_TRE_CE_2G;TRIBUNAL:TRE-CE"
        ));

        SecretariatInstitutionalVisibilityService service = new SecretariatInstitutionalVisibilityService(
                currentUserService,
                mock(ProcessoRepository.class),
                mock(WorkItemRepository.class),
                mock(SecretariatOperationalRoutingResolver.class),
                mock(SecretariatInboxAccessService.class),
                new SecretariatSpecializationResolver()
        );

        SecretariatSpecializationResolver.SecretariatSpecializationProfile specialization = new SecretariatSpecializationResolver.SecretariatSpecializationProfile(
                "SECRETARIA_SEGUNDA_INSTANCIA_ELEITORAL",
                "SEGUNDA_INSTANCIA",
                "ELEITORAL",
                "PJB_SEGUNDA_INSTANCIA",
                "PJB Segunda Instância | Eleitoral",
                "pjb-segunda-instancia",
                "SECRETARIA_TRE_CE_2G",
                "Secretaria Judiciária Eleitoral de Segunda Instância - TRE-CE",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                List.of("RECEBIMENTO", "EMBARGOS"),
                Map.of("institutionBirthMode", "INSTITUICAO_JUDICIARIA_RAIZ", "unitCode", "SECRETARIA_TRE_CE_2G")
        );

        SecretariatOperationalRoutingProfile routing = new SecretariatOperationalRoutingProfile(
                "ROUTE",
                "ELEITORAL",
                "TRE-CE",
                "SEGUNDA_INSTANCIA",
                "JUSTICA_ELEITORAL",
                "ELEITORAL",
                "SECRETARIA",
                "SECRETARIA_TRE_CE_2G",
                "REC",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                "SAN",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                "AUD",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                "EXEC",
                "SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA",
                "HR",
                "ELEITORAL>TRE_CE>SEGUNDA",
                Duration.ofHours(4),
                Duration.ofHours(8),
                Duration.ofHours(12),
                30,
                true,
                true,
                false,
                false,
                List.of(),
                List.of(),
                specialization,
                JudicialScaleProfile.SECRETARIA_TRIBUNAL,
                Map.of("uf", "CE", "comarca", "Fortaleza", "unidadeJudiciariaCodigo", "SECRETARIA_TRE_CE_2G")
        );

        assertDoesNotThrow(() -> service.requireRoutingAccess(routing));
    }

    @Test
    void deveDescreverInboxEspecializadoComNamespacePainelEOrigemInstitucional() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        SecretariatInboxAccessService inboxAccessService = mock(SecretariatInboxAccessService.class);
        when(currentUserService.getRequired()).thenReturn(user(
                "INSTANCIA:SEGUNDA_INSTANCIA;RAMO:ELEITORAL;TRIBUNAL:TRE-CE;SECRETARIA:SECRETARIA_TRE_CE_2G",
                "UJ:SECRETARIA_TRE_CE_2G;TRIBUNAL:TRE-CE"
        ));
        when(inboxAccessService.requireAccess("sec:tre-ce:2g:eleitoral:ce:fortaleza")).thenReturn("SEC:TRE-CE:2G:ELEITORAL:CE:FORTALEZA");

        SecretariatInstitutionalVisibilityService service = new SecretariatInstitutionalVisibilityService(
                currentUserService,
                mock(ProcessoRepository.class),
                mock(WorkItemRepository.class),
                mock(SecretariatOperationalRoutingResolver.class),
                inboxAccessService,
                new SecretariatSpecializationResolver()
        );

        var profile = service.describeAuthorizedInbox("sec:tre-ce:2g:eleitoral:ce:fortaleza");

        assertEquals("PJB_SEGUNDA_INSTANCIA", profile.specialization().namespacePjb());
        assertEquals("ELEITORAL", profile.specialization().secretariatBranchClass());
        assertEquals("SEGUNDA_INSTANCIA", profile.specialization().secretariatInstanceClass());
        assertEquals("INSTITUICAO_JUDICIARIA_RAIZ", profile.specialization().metadata().get("institutionBirthMode"));
    }


    private Usuario user(String perfil, String registro) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Servidor Operacional");
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setEnteFederativo(EnteFederativo.ESTADO);
        usuario.setPerfil(perfil);
        usuario.setRegistroProfissional(registro);
        usuario.setEspecialidades(List.of("SECRETARIA", "ELEITORAL", "SEGUNDA_INSTANCIA"));
        return usuario;
    }
}
