package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.processo.PoloProcessual;
import com.tcc.pjb.backend.model.repository.PoloProcessualRepository;
import com.tcc.pjb.backend.service.cidadao.CidadaoPosicaoProcessualService.CidadaoPosicaoProcessual;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CidadaoPosicaoProcessualServiceTest {

    private static final String CPF = "11122233344";

    private final PoloProcessualRepository poloRepository = mock(PoloProcessualRepository.class);
    private final CidadaoPosicaoProcessualService service =
            new CidadaoPosicaoProcessualService(poloRepository, new ProcessoPartyCpfMatcher());
    private final Processo processo = new Processo();

    @BeforeEach
    void setUp() {
        processo.setId(42L);
    }

    private void linhaDoPolo(TipoPolo tipoPolo, String oabNumero, Long usuarioRepresentante) {
        PoloProcessual polo = new PoloProcessual(42L, tipoPolo, tipoPolo == TipoPolo.ATIVO ? TipoParte.AUTOR : TipoParte.REU,
                "Maria da Silva", CPF, "CPF", oabNumero, oabNumero == null ? null : "CE", usuarioRepresentante, null, null, 0);
        when(poloRepository.findFirstByProcessoIdAndDocumentoAndAtivoTrueOrderByIdAsc(42L, CPF)).thenReturn(Optional.of(polo));
    }

    private void ajuizadoPor(TipoUsuario tipo, String cpf) {
        Usuario ator = new Usuario();
        ator.setId(900L);
        ator.setTipoUsuario(tipo);
        ator.setCpf(cpf);
        processo.setUsuario(ator);
    }

    @Test
    void autoraEmJusPostulandiPeloInstrumentoAtuaSemAdvogado() {
        linhaDoPolo(TipoPolo.ATIVO, null, null);
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO.name());

        assertThat(service.resolver(processo, CPF))
                .isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.ATIVO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO));
    }

    @Test
    void autoraComProcessoAjuizadoPorAdvogadoSemOabNaLinhaDoPoloTemAdvogado() {
        linhaDoPolo(TipoPolo.ATIVO, null, 900L);
        ajuizadoPor(TipoUsuario.ADVOGADO, "99988877766");

        assertThat(service.resolver(processo, CPF).representacao()).isEqualTo(TipoRepresentanteProcessual.ADVOGADO_PRIVADO);
    }

    @Test
    void autoraComProcessoAjuizadoPelaDefensoriaEAssistidaPelaDefensoria() {
        linhaDoPolo(TipoPolo.ATIVO, null, 900L);
        ajuizadoPor(TipoUsuario.DEFENSOR_PUBLICO, "99988877766");

        assertThat(service.resolver(processo, CPF).representacao()).isEqualTo(TipoRepresentanteProcessual.DEFENSOR_PUBLICO);
    }

    @Test
    void cidadaoQueAjuizouSozinhoAtuaSemAdvogado() {
        processo.setParteAutoraCpf(CPF);
        ajuizadoPor(TipoUsuario.CIDADAO, CPF);

        assertThat(service.resolver(processo, CPF))
                .isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.ATIVO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO));
    }

    @Test
    void cidadaoVinculadoComoUsuarioDoProcessoFicaNoPoloAtivo() {
        ajuizadoPor(TipoUsuario.CIDADAO, CPF);

        assertThat(service.resolver(processo, CPF).polo()).isEqualTo(TipoPolo.ATIVO);
        assertThat(service.polo(processo, CPF)).isEqualTo(TipoPolo.ATIVO);
    }

    @Test
    void processoCadastradoPorServidorNaoPresumeRepresentacao() {
        linhaDoPolo(TipoPolo.ATIVO, null, null);
        ajuizadoPor(TipoUsuario.SERVIDOR, "99988877766");

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.ATIVO, null));
    }

    @Test
    void instrumentoEAtorDoAjuizamentoNaoSeAplicamAoReu() {
        linhaDoPolo(TipoPolo.PASSIVO, null, null);
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA.name());
        ajuizadoPor(TipoUsuario.ADVOGADO, "99988877766");

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, null));
    }

    @Test
    void reuComOabRegistradaNoPoloTemAdvogado() {
        linhaDoPolo(TipoPolo.PASSIVO, "654321", null);

        assertThat(service.resolver(processo, CPF))
                .isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO));
    }

    @Test
    void semLinhaAtivaNoPoloUsaOsCpfsDoProcesso() {
        processo.setParteReuCpf(CPF);

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, null));
        assertThat(service.polo(processo, CPF)).isEqualTo(TipoPolo.PASSIVO);
    }

    @Test
    void cidadaoSemCpfNaoTemPosicaoResolvida() {
        assertThat(service.resolver(processo, " ")).isEqualTo(new CidadaoPosicaoProcessual(null, null));
        assertThat(service.polo(processo, " ")).isNull();
    }
}
