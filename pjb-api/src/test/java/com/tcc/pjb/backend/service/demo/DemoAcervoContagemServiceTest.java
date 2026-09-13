package com.tcc.pjb.backend.service.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.service.demo.DemoAcervoContagemService.ContagensDoAcervo;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

class DemoAcervoContagemServiceTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

    private final DemoAcervoContagemService service = new DemoAcervoContagemService(jdbcTemplate);

    private static BadSqlGrammarException falhaDeBanco() {
        return new BadSqlGrammarException("StatementCallback", "SELECT COUNT(1) FROM tb_usuario",
                new SQLException("ERROR: relation \"tb_usuario\" does not exist"));
    }

    @Test
    void contaUsuariosProcessosEDocumentos() {
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_usuario", Long.class)).thenReturn(7L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_processo", Long.class)).thenReturn(13L);
        when(jdbcTemplate.queryForObject("SELECT COUNT(1) FROM tb_documento_processual", Long.class))
                .thenReturn(29L);

        assertThat(service.contar()).contains(new ContagensDoAcervo(7L, 13L, 29L));
    }

    @Test
    void contaATabelaInteiraSemRecorteDeEquipe() {
        // Processo carrega @Filter de equipe do Hibernate: se a contagem viesse de repositorio, o painel
        // devolveria o recorte da equipe ativa quando o filtro estivesse ligado e o total quando nao.
        // Os literais abaixo sao os nomes reais declarados hoje em @Table; se uma migracao renomear
        // tabela, este teste reprova e obriga uma decisao, em vez de o painel ficar "indisponivel".
        service.contar();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, times(3)).queryForObject(sql.capture(), eq(Long.class));
        assertThat(sql.getAllValues()).containsExactly(
                "SELECT COUNT(1) FROM tb_usuario",
                "SELECT COUNT(1) FROM tb_processo",
                "SELECT COUNT(1) FROM tb_documento_processual");
    }

    @Test
    void falhaDeLeituraViraAusenciaEmVezDeZero() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenThrow(falhaDeBanco());

        assertThat(service.contar())
                .as("vazio e 'nao consegui ler'; zero seria 'li e o acervo esta vazio', que e outra "
                        + "afirmacao e nao pode sair daqui sem leitura")
                .isEmpty();
    }

    @Test
    void defeitoDeCodigoNaoEConvertidoEmDadosIndisponiveis() {
        // O catch e estreito de proposito: DataAccessException cobre toda falha de banco traduzida pelo
        // JdbcTemplate. Qualquer outra excecao e defeito de codigo e precisa aparecer.
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
                .thenThrow(new IllegalStateException("defeito de codigo"));

        assertThatThrownBy(service::contar).isInstanceOf(IllegalStateException.class);
    }
}
