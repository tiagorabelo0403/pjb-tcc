package com.tcc.pjb.backend.controller.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova end-to-end, contra a cadeia real de seguranca do Spring (Testcontainers Postgres,
 * JWT real, sem mocks no caminho de autorizacao), que um usuario CIDADAO cujo CPF nao bate
 * com nenhuma parte do {@link Processo} recebe 403 e que essa decisao gera uma entrada real
 * no ledger de auditoria ({@code AUTHZ_CIDADAO_PARTE_DENY}) em vez de uma negacao silenciosa.
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
 */
@AutoConfigureMockMvc
class CidadaoInstanciasControllerCpfMismatchIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private AuditLedgerService auditLedgerService;

    @Test
    void cidadaoComCpfDivergenteRecebe403EGeraEntradaNoLedger() throws Exception {
        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadao CPF Divergente");
        cidadao.setEmail("cidadao.divergente@pjb.local");
        cidadao.setCpf("11111111111");
        cidadao.setAtivo(true);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setPerfil(TipoUsuario.CIDADAO.name());
        cidadao = usuarioRepository.save(cidadao);
        long cidadaoId = cidadao.getId();

        Processo processo = Processo.builder()
                .numeroUnificado("0009999-40.2026.8.06.0001")
                .numeroProcesso("0009999-40.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .parteAutoraCpf("22222222222")
                .parteReuCpf("33333333333")
                .dataCriacao(LocalDateTime.now())
                .build();
        processo = processoRepository.save(processo);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cidadao/processos/{processoId}/instancias", processo.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(cidadaoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_CIDADAO"))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("CPF do cidadao (11111111111) nao bate com autor (22222222222) nem reu (33333333333) do processo")
                .isEqualTo(403);

        String resourceIdEsperado = processo.getNumeroUnificado();
        boolean denyRegistrado = auditLedgerService.entries().stream()
                .anyMatch(entry -> "AUTHZ_CIDADAO_PARTE_DENY".equals(entry.getAction())
                        && resourceIdEsperado.equals(entry.getResourceId()));
        assertThat(denyRegistrado)
                .as("Decisao de negacao por CPF divergente deve gerar entrada AUTHZ_CIDADAO_PARTE_DENY no ledger, nao so a excecao HTTP")
                .isTrue();
    }
}
