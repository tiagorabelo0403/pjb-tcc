package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.InstrumentoRepresentacaoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
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
    private final CidadaoPosicaoProcessualService service = new CidadaoPosicaoProcessualService(poloRepository);
    private final Processo processo = new Processo();

    @BeforeEach
    void setUp() {
        processo.setId(42L);
    }

    private void poloDoCidadao(TipoPolo tipoPolo, String oabNumero) {
        PoloProcessual polo = new PoloProcessual(42L, tipoPolo, tipoPolo == TipoPolo.ATIVO ? TipoParte.AUTOR : TipoParte.REU,
                "Maria da Silva", CPF, "CPF", oabNumero, oabNumero == null ? null : "CE", null, null, null, 0);
        when(poloRepository.findByProcessoIdAndDocumento(42L, CPF)).thenReturn(Optional.of(polo));
    }

    @Test
    void autoraEmJusPostulandiNoJuizadoAtuaSemAdvogado() {
        poloDoCidadao(TipoPolo.ATIVO, null);
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.JUS_POSTULANDI_JUIZADO.name());

        assertThat(service.resolver(processo, CPF))
                .isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.ATIVO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO));
    }

    @Test
    void autoraComMandatoAdJudiciaTemAdvogado() {
        poloDoCidadao(TipoPolo.ATIVO, "123456");
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA.name());

        assertThat(service.resolver(processo, CPF).representacao()).isEqualTo(TipoRepresentanteProcessual.ADVOGADO_PRIVADO);
    }

    @Test
    void autoraAssistidaPelaDefensoriaPublica() {
        poloDoCidadao(TipoPolo.ATIVO, null);
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.DEFENSORIA_PUBLICA_INSTITUCIONAL.name());

        assertThat(service.resolver(processo, CPF).representacao()).isEqualTo(TipoRepresentanteProcessual.DEFENSOR_PUBLICO);
    }

    @Test
    void instrumentoDoAjuizamentoNaoSeAplicaAoReu() {
        poloDoCidadao(TipoPolo.PASSIVO, null);
        processo.setInstrumentoRepresentacaoResolvido(InstrumentoRepresentacaoProcessual.MANDATO_AD_JUDICIA.name());

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, null));
    }

    @Test
    void reuComOabRegistradaNoPoloTemAdvogado() {
        poloDoCidadao(TipoPolo.PASSIVO, "654321");

        assertThat(service.resolver(processo, CPF))
                .isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO));
    }

    @Test
    void semLinhaNoPoloUsaOsCpfsDoProcessoParaOPolo() {
        processo.setParteReuCpf(CPF);

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.PASSIVO, null));
    }

    @Test
    void linhaDesativadaDoPoloNaoDefineAPosicao() {
        PoloProcessual polo = new PoloProcessual(42L, TipoPolo.PASSIVO, TipoParte.REU, "Maria da Silva", CPF, "CPF",
                "654321", "CE", null, null, null, 0);
        polo.desativar();
        when(poloRepository.findByProcessoIdAndDocumento(42L, CPF)).thenReturn(Optional.of(polo));
        processo.setParteAutoraCpf(CPF);

        assertThat(service.resolver(processo, CPF)).isEqualTo(new CidadaoPosicaoProcessual(TipoPolo.ATIVO, null));
    }

    @Test
    void cidadaoSemCpfNaoTemPosicaoResolvida() {
        assertThat(service.resolver(processo, " ")).isEqualTo(new CidadaoPosicaoProcessual(null, null));
    }
}
