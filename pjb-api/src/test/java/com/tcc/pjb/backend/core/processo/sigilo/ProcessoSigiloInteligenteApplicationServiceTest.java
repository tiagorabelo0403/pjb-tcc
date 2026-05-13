package com.tcc.pjb.backend.core.processo.sigilo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.validation.document.DocumentoNacionalValidator;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.SigiloService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessoSigiloInteligenteApplicationServiceTest {

    @Mock private ProcessoRepository processoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoSigiloApplicationService processoSigiloApplicationService;
    @Mock private SigiloService sigiloService;

    @Test
    void deveRestringirOperacaoPolicialParaJuizEDelegado() {
        Processo processo = new Processo();
        processo.setId(50L);
        processo.setNumeroProcesso("0000050-00.2026.4.01.0001");
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setClasseProcessual("Inquérito policial sigiloso");
        processo.setAssunto("Operação da Polícia Federal com inteligência e informação classificada");
        processo.setMaterialProbatorioResumo("Interceptação e operação da PF");
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setParteAutoraCpf("12345678901");
        processo.setUsuario(Usuario.builder().id(91L).nome("Advogado Autor").email("adv@x.com").senha("s").cpf("11111111111").tipoUsuario(TipoUsuario.ADVOGADO).perfil("ADVOGADO").build());

        Usuario juiz = Usuario.builder().id(1L).nome("Juiz Federal").email("juiz@x.com").senha("s").cpf("22222222222").tipoUsuario(TipoUsuario.JUIZ_FEDERAL).perfil("JUIZ_FEDERAL").ativo(true).comarca("Fortaleza").build();
        Usuario delegadoPf = Usuario.builder().id(2L).nome("Delegado PF").email("pf@x.com").senha("s").cpf("33333333333").tipoUsuario(TipoUsuario.DELEGADO_POLICIA_FEDERAL).perfil("DELEGADO_POLICIA_FEDERAL").ativo(true).comarca("Fortaleza").build();
        Usuario advogadoEscopo = Usuario.builder().id(3L).nome("Advogado Escopo").email("adv2@x.com").senha("s").cpf("44444444444").tipoUsuario(TipoUsuario.ADVOGADO).perfil("ADVOGADO").ativo(true).comarca("Fortaleza").build();

        when(processoRepository.findById(50L)).thenReturn(Optional.of(processo));
        when(usuarioRepository.findAll()).thenReturn(List.of(juiz, delegadoPf, advogadoEscopo));
        when(usuarioRepository.findByComarcaAndAtivoTrue("Fortaleza")).thenReturn(List.of(juiz, delegadoPf, advogadoEscopo));
        when(processoUnificadoApplicationService.detalhar(50L)).thenReturn(unificado(50L, "Fortaleza"));
        when(processoSigiloApplicationService.detalhar(50L)).thenReturn(sigiloBase(50L));
        when(sigiloService.avaliar(processo)).thenReturn(new SigiloService.SigiloDecision(
                NivelSigilo.SIGILO_N3,
                70,
                java.util.EnumSet.of(SigiloService.SigiloSignal.PENAL_SENSIVEL, SigiloService.SigiloSignal.LGPD),
                List.of("exigir_justificativa_need_to_know")
        ));

        ProcessoSigiloInteligenteApplicationService service = new ProcessoSigiloInteligenteApplicationService(
                processoRepository,
                usuarioRepository,
                processoUnificadoApplicationService,
                processoSigiloApplicationService,
                sigiloService,
                new DocumentoNacionalValidator()
        );

        var aggregate = service.avaliar(50L);
        assertThat(aggregate.nivelRecomendado()).isEqualTo(NivelSigilo.SEGREDO_ESTADO);
        assertThat(aggregate.statusClassificacao()).isEqualTo("REVISAO_PARA_SEGREDO_ESTADO");
        assertThat(aggregate.audienceMode()).isEqualTo("JUIZ_E_DELEGADO");
        assertThat(aggregate.destinatarios()).extracting("audienceCode").containsExactlyInAnyOrder("MAGISTRADO_NATURAL", "DELEGADO_OPERACAO");
        assertThat(aggregate.protecoesDados()).isNotEmpty();
        assertThat(aggregate.revisaoJudicialObrigatoria()).isTrue();
    }

    private ProcessoUnificadoAggregate unificado(Long processoId, String comarca) {
        return new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(processoId, "50", "50", "TRF5", "CE", comarca, "Vara Federal", "Classe", "Assunto", "Autor", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("FEDERAL", "PRIMEIRO_GRAU", "PENAL", "INQUERITO", "INVESTIGACAO", "EM_ANDAMENTO", "TRF5", "Tribunal", "Vara Federal", "Vara Federal", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "PENAL", "PADRAO", "AUTO", "CONTROLADO", "GABINETE", true, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 0, 0, 1, 1, List.of(), List.of(), Instant.now()),
                List.of(), List.of(), List.of(), Instant.now()
        );
    }

    private ProcessoSigiloAggregate sigiloBase(Long processoId) {
        return new ProcessoSigiloAggregate(
                new ProcessoUnificadoIdentity(processoId, "50", "50", "TRF5", "CE", "Fortaleza", "Vara Federal", "Classe", "Assunto", "Autor", "Réu", List.of("PENAL")),
                NivelSigilo.PUBLICO,
                "PUBLICO_CONTROLADO",
                false,
                false,
                false,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("fundamento"),
                Instant.now()
        );
    }
}
