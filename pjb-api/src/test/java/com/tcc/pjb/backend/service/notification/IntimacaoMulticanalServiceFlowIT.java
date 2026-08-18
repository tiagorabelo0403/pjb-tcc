package com.tcc.pjb.backend.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.document.template.OfficialDocumentTemplateRenderResponse;
import com.tcc.pjb.backend.model.entity.NotificationHistory;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.model.entity.notification.UserNotificationPreference;
import com.tcc.pjb.backend.model.repository.NotificationHistoryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UserNotificationPreferenceRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.processual.document.template.OfficialDocumentTemplateService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class IntimacaoMulticanalServiceFlowIT extends PjbIntegrationTestBase {

    @Autowired
    private IntimacaoMulticanalService service;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificationHistoryRepository historyRepository;

    @Autowired
    private UserNotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    private PjbAuthorizationService authorizationService;

    @MockitoBean
    private OfficialDocumentTemplateService officialDocumentTemplateService;

    @MockitoBean
    @Qualifier("logNotificationChannel")
    private NotificationChannel notificationChannel;

    @Test
    void devePersistirHistoricoGerarTrackingEDocumentoFormalNoDispatchMulticanal() {
        Processo processo = processoRepository.save(novoProcesso());
        Usuario usuario = usuarioRepository.save(novoUsuario());
        preferenceRepository.save(preferenciaEmail(usuario));

        doNothing().when(authorizationService).requireReadProcesso(any(Processo.class));
        when(notificationChannel.channelId()).thenReturn("EMAIL");
        when(notificationChannel.supports(any())).thenReturn(true);
        doNothing().when(notificationChannel).send(any(NotificationDispatchContext.class));
        when(officialDocumentTemplateService.renderizar(any())).thenReturn(new OfficialDocumentTemplateRenderResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                TemplateDocumentoOficial.INTIMACAO_FORMAL,
                "Intimação formal multicanal",
                List.of("destinatario", "conteudoIntimacao", "prazoResposta"),
                List.of(),
                "CONTEUDO",
                "abc123hash",
                UUID.fromString("00000000-0000-0000-0000-000000000321"),
                DocumentoCategoria.PUBLICO,
                NivelSigilo.PUBLICO,
                true,
                true,
                List.of(),
                Map.of("profile", "PAdES-LT", "issuer", "ICP-Brasil"),
                Map.of("soberano", true, "ledger", "ok")
        ));

        IntimacaoMulticanalService.DispatchPlanResponse response = service.dispatch(
                processo.getId(),
                usuario.getId(),
                "Intimação para manifestação",
                "Manifestar-se em cinco dias.",
                "https://pjb.test/processos/1",
                true
        );

        List<NotificationHistory> history = historyRepository.findTop50ByUsuarioIdAndProcessoIdOrderByEnviadoEmDesc(usuario.getId(), processo.getId());
        assertThat(response.processoId()).isEqualTo(processo.getId());
        assertThat(response.usuarioId()).isEqualTo(usuario.getId());
        assertThat(response.deliveredChannels()).containsExactly("EMAIL");
        assertThat(response.failedChannels()).isEmpty();
        assertThat(response.trackingTokens()).hasSize(1);
        assertThat(response.status()).isEqualTo("ENTREGA_PARCIAL_OU_TOTAL");
        assertThat(response.documentoFormalAssinado())
                .containsEntry("template", TemplateDocumentoOficial.INTIMACAO_FORMAL.name())
                .containsEntry("hashSha256", "abc123hash")
                .containsEntry("selado", true);
        assertThat(response.assinaturaQualificada()).containsEntry("profile", "PAdES-LT");
        assertThat(response.validacaoSoberana()).containsEntry("ledger", "ok");
        assertThat(history)
                .hasSize(1)
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getCanal()).isEqualTo("EMAIL");
                    assertThat(item.getStatus()).isEqualTo("ENVIADO");
                    assertThat(item.getTrackingToken()).isNotBlank();
                    assertThat(item.getTrackingHash()).isNotBlank();
                    assertThat(item.getTitulo()).isEqualTo("Intimação para manifestação");
                });
    }

    private static Processo novoProcesso() {
        return Processo.builder()
                .numeroProcesso("NOTIF-IT-2026-0001")
                .numeroUnificado("0009191-77.2026.8.06.0001")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .classeProcessual("Ação de obrigação de fazer")
                .assunto("Tutela de urgência")
                .pedidoPrincipal("Cumprimento imediato da obrigação")
                .parteAutoraNome("Maria Autora")
                .parteAutoraCpf("12345678909")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .vara("3ª Vara Cível")
                .uf("CE")
                .nivelSigilo(NivelSigilo.PUBLICO)
                .build();
    }

    private static Usuario novoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Advogado Notificado");
        usuario.setEmail("advogado.notificado@pjb.test");
        usuario.setCpf("33333333333");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setPerfil("ADVOGADO");
        return usuario;
    }

    private static UserNotificationPreference preferenciaEmail(Usuario usuario) {
        UserNotificationPreference preference = new UserNotificationPreference();
        preference.setUsuario(usuario);
        preference.setAllowEmail(true);
        preference.setAllowPush(false);
        preference.setAllowWhatsapp(false);
        preference.setAllowArDigital(false);
        preference.setAllowWebhook(false);
        preference.setAllowDigest(true);
        preference.setOnlyHighPriority(false);
        preference.setAntiSpamWindowMinutes(30);
        return preference;
    }
}
