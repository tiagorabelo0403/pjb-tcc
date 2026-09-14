package com.tcc.pjb.backend.service.demo;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Contagens do acervo exibidas no painel de demonstração.
 *
 * <p>A leitura é por SQL direto, e não pelos repositórios, de propósito: {@link Processo} carrega
 * filtro de equipe do Hibernate, então {@code count()} de repositório devolveria o recorte da equipe
 * ativa quando o filtro está ligado e o total quando não está. Painel de totais do sistema que muda
 * de número conforme quem pergunta é pior do que painel nenhum.
 *
 * <p>Os nomes de tabela saem do próprio mapeamento, não de literal digitado aqui: uma migração que
 * renomeie tabela não pode deixar o painel respondendo "indisponível" para sempre sem que ninguém
 * descubra o motivo.
 *
 * <p>Sem {@code @Transactional} também de propósito. A anotação abriria a transação no proxy, antes
 * do corpo do método, e uma falha de conexão passaria a nascer fora do {@code try} — justamente o
 * caso que este serviço precisa tratar.
 */
@Service
public class DemoAcervoContagemService {

    private static final Logger log = LoggerFactory.getLogger(DemoAcervoContagemService.class);

    /** Contagens globais do acervo, sem recorte por equipe, comarca ou perfil de quem pergunta. */
    public record ContagensDoAcervo(long usuarios, long processos, long documentos) {}

    private final JdbcTemplate jdbcTemplate;
    private final String sqlUsuarios;
    private final String sqlProcessos;
    private final String sqlDocumentos;

    public DemoAcervoContagemService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate);
        this.sqlUsuarios = contagemDe(Usuario.class);
        this.sqlProcessos = contagemDe(Processo.class);
        this.sqlDocumentos = contagemDe(DocumentoProcessual.class);
    }

    /**
     * @return vazio quando o acervo não pôde ser lido. Vazio é "não consegui ler", nunca "li e deu
     *     zero" — o motivo fica no log do servidor, com a causa, e não sobe para a resposta: mensagem
     *     de erro de SQL carrega nome de tabela e de coluna.
     */
    public Optional<ContagensDoAcervo> contar() {
        try {
            return Optional.of(new ContagensDoAcervo(
                    contarPor(sqlUsuarios), contarPor(sqlProcessos), contarPor(sqlDocumentos)));
        } catch (DataAccessException e) {
            // Só DataAccessException: o JdbcTemplate traduz toda falha de banco para essa hierarquia,
            // da conexão indisponível ao nome de tabela inexistente. Qualquer outra exceção aqui é
            // defeito de código e precisa aparecer, não ser convertida em "dados indisponíveis".
            log.warn("Contagens do acervo indisponiveis para o painel de demonstracao", e);
            return Optional.empty();
        }
    }

    private long contarPor(String sql) {
        Long total = jdbcTemplate.queryForObject(sql, Long.class);
        return total != null ? total : 0L;
    }

    /**
     * Falha na criação do bean, e portanto no boot, se o mapeamento não disser o nome da tabela. É
     * duro de propósito: as três entidades são centrais, perder o {@code @Table} delas é sintoma de
     * quebra bem maior que o painel, e qualquer teste de contexto pega isso antes de um ambiente real.
     */
    private static String contagemDe(Class<?> entidade) {
        Table tabela = entidade.getAnnotation(Table.class);
        if (tabela == null || tabela.name().isBlank()) {
            throw new IllegalStateException(entidade.getName()
                    + " nao declara @Table(name=...): sem o nome da tabela vindo do mapeamento, a "
                    + "contagem do painel de demonstracao voltaria a depender de literal digitado.");
        }
        return "SELECT COUNT(1) FROM " + tabela.name();
    }
}
