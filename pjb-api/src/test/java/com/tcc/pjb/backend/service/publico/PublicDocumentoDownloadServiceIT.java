package com.tcc.pjb.backend.service.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Prova, com Postgres real, a superfície de leitura "pública" de documento
 * ({@code /api/v1/public/processos/documentos/{id}/pdf}) — a rota que fecha, para um profissional
 * autenticado que NÃO é parte do processo, a leitura de uma petição inicial já materializada como
 * {@link DocumentoProcessual} real (a mesma peça que {@code LaianePeticaoInicialDraftService
 * .protocolar()} passou a criar). Não existia nenhuma cobertura de teste para esta rota antes.
 *
 * <p>Confirma o desenho real do serviço (não o que se supõe): exige usuário autenticado ativo, exige
 * "modo profissional" (advogado/magistrado/servidor/MP/defensoria/admin fórum — um cidadão comum é
 * negado mesmo para documento público), e só dispensa o ABAC individual quando o documento é
 * categoria PÚBLICO e nem o processo nem o documento exigem credencial de sigilo.</p>
 */
class PublicDocumentoDownloadServiceIT extends PjbIntegrationTestBase {

    @Autowired
    private PublicDocumentoDownloadService downloadService;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DocumentoProcessualRepository documentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @Test
    void profissionalNaoParteLePeticaoInicialDeProcessoPublico() {
        Processo processo = processoRepository.save(processoPublico("CP-DOC-01", NivelSigilo.PUBLICO));
        DocumentoProcessual peticao = documentoRepository.save(peticaoInicial(processo, NivelSigilo.PUBLICO));
        Usuario advogadoQualquer = usuario(TipoUsuario.ADVOGADO, "01");
        when(currentUserService.getOrNull()).thenReturn(advogadoQualquer);

        ResponseEntity<org.springframework.core.io.Resource> resposta = downloadService.baixarPdf(peticao.getId());

        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resposta.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_PDF);
    }

    @Test
    void cidadaoComumAutenticadoENegadoMesmoParaDocumentoPublico() {
        Processo processo = processoRepository.save(processoPublico("CP-DOC-02", NivelSigilo.PUBLICO));
        DocumentoProcessual peticao = documentoRepository.save(peticaoInicial(processo, NivelSigilo.PUBLICO));
        Usuario cidadao = usuario(TipoUsuario.CIDADAO, "02");
        when(currentUserService.getOrNull()).thenReturn(cidadao);

        assertThatThrownBy(() -> downloadService.baixarPdf(peticao.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void semUsuarioAutenticadoENegado() {
        Processo processo = processoRepository.save(processoPublico("CP-DOC-03", NivelSigilo.PUBLICO));
        DocumentoProcessual peticao = documentoRepository.save(peticaoInicial(processo, NivelSigilo.PUBLICO));
        when(currentUserService.getOrNull()).thenReturn(null);

        assertThatThrownBy(() -> downloadService.baixarPdf(peticao.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void profissionalSemVinculoENegadoParaPeticaoDeProcessoSigiloso() {
        Processo processo = processoRepository.save(processoPublico("CP-DOC-04", NivelSigilo.SIGILO_N2));
        DocumentoProcessual peticao = documentoRepository.save(peticaoInicial(processo, NivelSigilo.SIGILO_N2));
        Usuario advogadoEstranho = usuario(TipoUsuario.ADVOGADO, "04");
        when(currentUserService.getOrNull()).thenReturn(advogadoEstranho);

        assertThatThrownBy(() -> downloadService.baixarPdf(peticao.getId()))
                .isInstanceOfAny(AccessDeniedException.class, RecursoNaoEncontradoException.class,
                        com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException.class);
    }

    private Processo processoPublico(String numero, NivelSigilo sigilo) {
        return Processo.builder()
                .numeroProcesso(numero)
                .numeroUnificado(numero)
                .tipoJustica(TipoJustica.ESTADUAL)
                .ramoDireito(RamoDireito.CIVIL)
                .classeProcessual("Procedimento comum")
                .assunto("Obrigação de fazer")
                .parteAutoraNome("Autor Teste")
                .parteReuNome("Réu Teste")
                .tribunal("TJCE")
                .comarca("Fortaleza")
                .uf("CE")
                .nivelSigilo(sigilo)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();
    }

    private DocumentoProcessual peticaoInicial(Processo processo, NivelSigilo sigilo) {
        byte[] pdf = "%PDF-1.4\n%%EOF".getBytes(StandardCharsets.ISO_8859_1);
        return DocumentoProcessual.builder()
                .processo(processo)
                .titulo("Petição Inicial")
                .nomeOriginal("peticao-inicial.pdf")
                .sha256("a".repeat(64))
                .sha384("b".repeat(96))
                .contentType("application/pdf")
                .tamanhoBytes((long) pdf.length)
                .pdf(pdf)
                .origemSistema("LAIANE_PETICAO_INICIAL")
                .categoria(sigilo == NivelSigilo.PUBLICO ? DocumentoCategoria.PUBLICO : DocumentoCategoria.PESSOAL)
                .tipoDocumento(TipoDocumento.PETICAO_INICIAL)
                .nivelSigilo(sigilo)
                .build();
    }

    private Usuario usuario(TipoUsuario tipo, String sufixo) {
        Usuario u = new Usuario();
        u.setNome("Usuario Publico Teste " + sufixo);
        u.setEmail("usuario.publico." + sufixo + "." + System.nanoTime() + "@test.local");
        u.setCpf(String.valueOf(Math.abs(System.nanoTime())).substring(0, 11));
        u.setTipoUsuario(tipo);
        u.setPerfil(tipo.name());
        u.setAtivo(true);
        u.setUf("CE");
        u.setComarca("Fortaleza");
        return usuarioRepository.save(u);
    }
}
