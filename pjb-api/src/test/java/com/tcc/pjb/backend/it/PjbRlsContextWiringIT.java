package com.tcc.pjb.backend.it;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.configs.security.UsuarioPrincipal;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Prova que a correção de {@link com.tcc.pjb.backend.configs.datasource.PjbRlsContextDataSourceConfig}
 * funciona de ponta a ponta: autenticando via {@link SecurityContextHolder} (o mecanismo real da
 * aplicação, não um {@code set_config} manual de teste), a GUC {@code app.pjb_actor_id} aparece na
 * conexão de uma transação NOVA — mesmo com {@code pjb.datasource.routing.enabled=false} (o valor
 * herdado por todo IT via {@code application-integration-test.yml}, que não sobrescreve essa
 * propriedade, e que também é o valor do deploy padrão em produção).
 *
 * <p>Antes da correção, {@code PjbProcessoSigiloRlsDataSource} só existia dentro do
 * {@code @Bean} condicional a essa flag — este teste teria visto {@code current_setting} vazio.</p>
 */
class PjbRlsContextWiringIT extends PjbIntegrationTestBase {

    @Autowired
    UsuarioRepository usuarioRepository;

    @Autowired
    EntityManager entityManager;

    @Autowired
    PlatformTransactionManager transactionManager;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticacaoRealPopulaGucDeAtorNaConexaoSemSetConfigManualNoTeste() {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        Usuario usuario = usuarioRepository.save(novoUsuario(sufixo));
        Long usuarioId = usuario.getId();

        UsuarioPrincipal principal = new UsuarioPrincipal(usuario);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String actorIdNaConexao = transactionTemplate.execute(status ->
                (String) entityManager.createNativeQuery("SELECT current_setting('app.pjb_actor_id', true)")
                        .getSingleResult());
        String actorRolesNaConexao = transactionTemplate.execute(status ->
                (String) entityManager.createNativeQuery("SELECT current_setting('app.pjb_actor_roles', true)")
                        .getSingleResult());

        assertThat(actorIdNaConexao)
                .as("a GUC de ator deve refletir o usuario autenticado via SecurityContextHolder, "
                        + "sem nenhum set_config manual neste teste")
                .isEqualTo(String.valueOf(usuarioId));
        assertThat(actorRolesNaConexao).contains("|ROLE_ADVOGADO|");
    }

    @Test
    void semAutenticacao_gucDeAtorFicaVazia() {
        SecurityContextHolder.clearContext();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        String actorIdNaConexao = transactionTemplate.execute(status ->
                (String) entityManager.createNativeQuery("SELECT current_setting('app.pjb_actor_id', true)")
                        .getSingleResult());

        assertThat(actorIdNaConexao).isEmpty();
    }

    private Usuario novoUsuario(String sufixo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Ator Wiring RLS " + sufixo);
        usuario.setEmail("ator.wiring.rls." + sufixo + "@test.local");
        usuario.setSenha("x");
        usuario.setCpf("11122233303");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setPerfil(TipoUsuario.ADVOGADO.name());
        usuario.setUf("CE");
        usuario.setComarca("Fortaleza");
        usuario.setAtivo(true);
        return usuario;
    }
}
