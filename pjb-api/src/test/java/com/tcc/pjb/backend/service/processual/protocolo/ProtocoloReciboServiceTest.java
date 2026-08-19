package com.tcc.pjb.backend.service.processual.protocolo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.protocolo.ProtocoloReciboResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ProtocoloReciboServiceTest {

    @Autowired
    private DocumentoProcessualRepository documentoProcessualRepository;

    @Autowired
    private DocumentoPaginaRepository documentoPaginaRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private ProtocoloReciboService service() {
        return new ProtocoloReciboService(documentoProcessualRepository, documentoPaginaRepository);
    }

    @Test
    void emiteReciboNaHoraQuandoNenhumFoiGeradoAinda() {
        Processo processo = salvarProcesso("0003333-33.2026.8.06.0001");
        Usuario advogado = salvarUsuario();

        ProtocoloReciboResponse recibo = service().obterOuEmitirRecibo(processo, advogado);

        assertThat(recibo.documentoId()).isNotBlank();
        assertThat(recibo.processoId()).isEqualTo(processo.getId());
        assertThat(recibo.numero()).isEqualTo(processo.getNumeroUnificado());
        assertThat(recibo.sha256()).isNotBlank();
        assertThat(recibo.conteudo()).contains("RECIBO DE PROTOCOLO");
        assertThat(documentoProcessualRepository.findByProcessoId(processo.getId())).hasSize(1);
    }

    @Test
    void naoDuplicaReciboQuandoJaExisteUmEmitido() {
        Processo processo = salvarProcesso("0004444-44.2026.8.06.0001");
        Usuario advogado = salvarUsuario();

        ProtocoloReciboResponse primeiro = service().obterOuEmitirRecibo(processo, advogado);
        ProtocoloReciboResponse segundo = service().obterOuEmitirRecibo(processo, advogado);

        assertThat(segundo.documentoId()).isEqualTo(primeiro.documentoId());
        assertThat(documentoProcessualRepository.findByProcessoId(processo.getId())).hasSize(1);
    }

    @Test
    void retornaOReciboJaEmitidoPeloFluxoDeLaianeSemGerarOutro() {
        Processo processo = salvarProcesso("0005555-55.2026.8.06.0001");
        Usuario advogado = salvarUsuario();
        service().emitirReciboPeticaoInicial(processo, advogado, "hash-laiane-exemplo");

        ProtocoloReciboResponse recibo = service().obterOuEmitirRecibo(processo, advogado);

        assertThat(documentoProcessualRepository.findByProcessoId(processo.getId())).hasSize(1);
        assertThat(recibo.conteudo()).contains("hash-laiane-exemplo");
    }

    private Processo salvarProcesso(String numero) {
        Processo processo = new Processo();
        processo.setNumeroProcesso(numero);
        processo.setNumeroUnificado(numero);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setNivelSigilo(NivelSigilo.PUBLICO);
        processo.setClasseProcessual("Procedimento Comum Civel");
        processo.setAssunto("Cobranca contratual");
        processo.setParteAutoraNome("Autor Recibo");
        processo.setParteReuNome("Reu Recibo");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setTribunal("TJCE");
        processo.setVara("1 Vara Civel de Fortaleza");
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        return processoRepository.saveAndFlush(processo);
    }

    private Usuario salvarUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNome("Advogado Recibo");
        usuario.setEmail("advogado.recibo." + System.nanoTime() + "@test.local");
        usuario.setCpf(cpfValido());
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setAtivo(true);
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String cpfValido() {
        long unique = Math.abs(System.nanoTime() % 900000000L);
        String base = String.format("%09d", unique);
        int[] digits = base.chars().map(c -> c - '0').toArray();
        int d1 = calcularDigitoCpf(digits, 10);
        int[] withD1 = java.util.Arrays.copyOf(digits, 10);
        withD1[9] = d1;
        int d2 = calcularDigitoCpf(withD1, 11);
        return base + d1 + d2;
    }

    private int calcularDigitoCpf(int[] digits, int peso) {
        int soma = 0;
        for (int i = 0; i < peso - 1; i++) {
            soma += digits[i] * (peso - i);
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
