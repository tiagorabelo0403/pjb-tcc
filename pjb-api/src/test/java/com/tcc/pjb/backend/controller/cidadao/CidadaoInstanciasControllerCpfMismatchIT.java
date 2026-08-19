package com.tcc.pjb.backend.controller.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end, contra a cadeia real de seguranca do Spring e Postgres real via
 * Testcontainers, que {@link com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService
 * #requireReadProcessoAsCidadaoParte} rejeita um CIDADAO cujo CPF nao bate com nenhuma parte
 * do processo, e libera quando o CPF bate com a parte autora.
 *
 * <p>{@link com.tcc.pjb.backend.service.julgamento.CidadaoInstanciasService#instancias} chama
 * {@code authz.requireReadProcessoAsCidadaoParte(p)} como primeira linha, antes de qualquer
 * outra logica — o que isola o teste ao branch de divergencia de CPF sem depender de mocks
 * adicionais.
 *
 * <p><b>nivelSigilo da fixture:</b> {@code requireReadProcessoAsCidadaoParte} chama primeiro
 * {@code requireReadProcesso} (o gate ABAC geral de leitura) antes do match de CPF. Um
 * {@link Processo} construido sem {@code nivelSigilo} explicito resolve para
 * {@code NivelSigilo.PUBLICO} (default em {@code AbacV1Policy.canReadProcesso} e em
 * {@code PjbAuthorizationSigiloResolver.computeProcessoSigiloEfetivo}), e PUBLICO nao exige
 * credencial — portanto qualquer usuario ativo passa por esse gate, e o 403 desta prova vem
 * especificamente do branch de CPF divergente, nao do ABAC de sigilo.
 *
 * <p>O teste de negacao tambem prova que a decisao gera uma entrada real no ledger de
 * auditoria ({@code AUTHZ_CIDADAO_PARTE_DENY}) em vez de uma negacao silenciosa.
 */
@AutoConfigureMockMvc
class CidadaoInstanciasControllerCpfMismatchIT extends PjbIntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    ProcessoRepository processoRepository;

    @Autowired
    AuditLedgerService auditLedgerService;

    @Test
    void cidadaoComCpfDivergenteDaParteRecebe403EGeraEntradaNoLedger() throws Exception {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CID-MISMATCH-1")
                .numeroUnificado("CID-MISMATCH-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .parteAutoraCpf("11111111111")
                .parteReuCpf("22222222222")
                .build());

        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadao Sem Vinculo");
        cidadao.setEmail("cidadao.sem.vinculo@pjb.local");
        cidadao.setCpf("99999999999");
        cidadao.setAtivo(true);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setPerfil(TipoUsuario.CIDADAO.name());
        cidadao = usuarioRepository.save(cidadao);
        long cidadaoId = cidadao.getId();

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cidadao/processos/{processoId}/instancias", processo.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(cidadaoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_CIDADAO"))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("CPF do cidadao autenticado nao bate com nenhuma parte do processo")
                .isEqualTo(403);

        String resourceIdEsperado = processo.getNumeroUnificado();
        boolean denyRegistrado = auditLedgerService.entries().stream()
                .anyMatch(entry -> "AUTHZ_CIDADAO_PARTE_DENY".equals(entry.getAction())
                        && resourceIdEsperado.equals(entry.getResourceId()));
        assertThat(denyRegistrado)
                .as("Decisao de negacao por CPF divergente deve gerar entrada AUTHZ_CIDADAO_PARTE_DENY no ledger, nao so a excecao HTTP")
                .isTrue();
    }

    @Test
    void cidadaoComCpfDaParteAutoraRecebe200() throws Exception {
        Processo processo = processoRepository.save(Processo.builder()
                .numeroProcesso("CID-MATCH-1")
                .numeroUnificado("CID-MATCH-U-1")
                .tribunal("TJCE")
                .uf("CE")
                .comarca("Fortaleza")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .parteAutoraCpf("33333333333")
                .parteReuCpf("44444444444")
                .build());

        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadao Parte Autora");
        cidadao.setEmail("cidadao.parte.autora@pjb.local");
        cidadao.setCpf("33333333333");
        cidadao.setAtivo(true);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setPerfil(TipoUsuario.CIDADAO.name());
        cidadao = usuarioRepository.save(cidadao);
        long cidadaoId = cidadao.getId();

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cidadao/processos/{processoId}/instancias", processo.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(cidadaoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_CIDADAO"))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("CPF do cidadao autenticado bate com a parte autora do processo")
                .isEqualTo(200);
    }
}
