package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ExecucaoEmContextoDeSistemaTest {

    private final Authentication tecnicoDeSuporte = new UsernamePasswordAuthenticationToken(
            "tecnico", "n/a", AuthorityUtils.createAuthorityList("ROLE_SUPORTE_TECNICO"));
    private final List<String> transacoesIniciadas = new ArrayList<>();
    private final ExecucaoEmContextoDeSistema execucao = new ExecucaoEmContextoDeSistema(new AbstractPlatformTransactionManager() {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            transacoesIniciadas.add(SecurityContextHolder.getContext().getAuthentication() == null ? "sem-ator" : "com-ator");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    });

    @BeforeEach
    void autenticarTecnico() {
        SecurityContextHolder.getContext().setAuthentication(tecnicoDeSuporte);
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void transacaoNovaComecaERodaSemAtorAutenticado() {
        Authentication durante = execucao.emTransacaoNova(() -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            return SecurityContextHolder.getContext().getAuthentication();
        });

        assertThat(durante).isNull();
        assertThat(transacoesIniciadas).containsExactly("sem-ator");
    }

    @Test
    void atorDaRequisicaoVoltaDepoisDaExecucao() {
        execucao.emTransacaoNova(() -> "ok");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(tecnicoDeSuporte);
    }

    @Test
    void atorDaRequisicaoVoltaMesmoQuandoAAcaoFalha() {
        assertThatThrownBy(() -> execucao.emTransacaoNova(() -> {
            throw new IllegalStateException("falha na gravacao");
        })).isInstanceOf(IllegalStateException.class).hasMessage("falha na gravacao");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(tecnicoDeSuporte);
    }
}
