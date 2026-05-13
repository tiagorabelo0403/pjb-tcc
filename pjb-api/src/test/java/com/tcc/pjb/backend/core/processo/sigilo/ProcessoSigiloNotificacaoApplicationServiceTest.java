package com.tcc.pjb.backend.core.processo.sigilo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloDestinatario;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloInteligenteAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloJurisdicaoBridge;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoSigiloNotificacaoApplicationServiceTest {

    @Mock private ProcessoRepository processoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProcessoSigiloInteligenteApplicationService processoSigiloInteligenteApplicationService;
    @Mock private NotificationService notificationService;

    @Test
    void devePlanejarENotificarDestinatariosComUsuario() {
        Processo processo = new Processo();
        processo.setId(77L);
        processo.setNumeroProcesso("77");
        when(processoRepository.findById(77L)).thenReturn(Optional.of(processo));
        when(processoSigiloInteligenteApplicationService.avaliar(77L)).thenReturn(aggregate());
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(Usuario.builder().id(10L).nome("Juiz").email("juiz@x.com").senha("s").cpf("11111111111").tipoUsuario(TipoUsuario.JUIZ).perfil("JUIZ").build()));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(Usuario.builder().id(20L).nome("Servidor").email("servidor@x.com").senha("s").cpf("22222222222").tipoUsuario(TipoUsuario.SERVIDOR_FORUM).perfil("SERVIDOR_FORUM").build()));

        ProcessoSigiloNotificacaoApplicationService service = new ProcessoSigiloNotificacaoApplicationService(
                processoRepository,
                usuarioRepository,
                processoSigiloInteligenteApplicationService,
                notificationService
        );

        var plano = service.planejar(77L);
        assertThat(plano.statusPlanejamento()).isEqualTo("PLANO_PRONTO");
        assertThat(plano.totalComUsuario()).isEqualTo(2);
        assertThat(plano.channels()).contains("PUSH_PJB", "EMAIL", "CAIXA_PJB");

        service.notificar(77L);
        verify(notificationService, times(2)).notifyUserAdvanced(org.mockito.ArgumentMatchers.any(Usuario.class), org.mockito.ArgumentMatchers.eq(processo), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(true));
    }

    private ProcessoSigiloInteligenteAggregate aggregate() {
        ProcessoUnificadoIdentity identity = new ProcessoUnificadoIdentity(77L, "77", "77", "TJCE", "CE", "Fortaleza", "Vara", "Classe", "Assunto", "Autor", "Réu", List.of("CIVEL"));
        return new ProcessoSigiloInteligenteAggregate(
                identity,
                NivelSigilo.PUBLICO,
                NivelSigilo.SEGREDO_JUSTICA,
                "RECLASSIFICACAO_JUDICIAL_OBRIGATORIA",
                true,
                true,
                false,
                true,
                "JUIZ_PARTES_E_INSTITUICOES_CREDENCIADAS",
                new ProcessoSigiloJurisdicaoBridge("ESTADUAL", "PRIMEIRO_GRAU", "CIVEL", "TJCE", "Vara", "CE", "Fortaleza", "Foro", "INSTITUCIONAL_REFORCADO", false, true, false, List.of("fundamento")),
                List.of("RECLASSIFICACAO_SUGERIDA"),
                List.of(
                        new ProcessoSigiloDestinatario(10L, "MAGISTRADO_NATURAL", "Magistrado natural", "JUIZ", "Juiz", List.of("PUSH_PJB", "EMAIL", "CAIXA_PJB"), false, true, true, "racional"),
                        new ProcessoSigiloDestinatario(20L, "SERVIDOR_FORO_CREDENCIADO", "Servidor do foro", "SERVIDOR_FORUM", "Servidor", List.of("PUSH_PJB", "EMAIL", "CAIXA_PJB"), true, true, true, "racional")
                ),
                List.of(),
                List.of(),
                List.of("fundamento"),
                Instant.now()
        );
    }
}
