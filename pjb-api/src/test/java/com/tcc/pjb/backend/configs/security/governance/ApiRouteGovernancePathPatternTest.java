package com.tcc.pjb.backend.configs.security.governance;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.pattern.PathPatternParser;

class ApiRouteGovernancePathPatternTest {

    private static final Path GOVERNANCA = Path.of("src/main/resources/application-api-governance.yml");
    private static final Pattern LINHA_PATHS = Pattern.compile("^\\s*paths:\\s*\\[(.+)]\\s*$", Pattern.MULTILINE);
    private static final Pattern PADRAO_CITADO = Pattern.compile("\"([^\"]+)\"");

    private record PadraoGovernado(int linha, String padrao) {
    }

    private List<PadraoGovernado> padroesGovernados() throws Exception {
        String conteudo = Files.readString(GOVERNANCA);
        List<PadraoGovernado> padroes = new ArrayList<>();
        Matcher linhas = LINHA_PATHS.matcher(conteudo);
        while (linhas.find()) {
            int numeroDaLinha = (int) conteudo.substring(0, linhas.start()).lines().count() + 1;
            Matcher citados = PADRAO_CITADO.matcher(linhas.group(1));
            while (citados.find()) {
                padroes.add(new PadraoGovernado(numeroDaLinha, citados.group(1)));
            }
        }
        return padroes;
    }

    @Test
    void extratorEnxergaTodasAsRegrasDoArquivo() throws Exception {
        long linhasDePaths = Files.readAllLines(GOVERNANCA).stream()
                .filter(linha -> linha.strip().startsWith("paths:"))
                .count();

        List<PadraoGovernado> padroes = padroesGovernados();

        assertThat(linhasDePaths)
                .as("o arquivo de governanca precisa continuar declarando regras; zero aqui tornaria o teste "
                        + "seguinte aprovado por vacuidade")
                .isGreaterThanOrEqualTo(40L);
        assertThat(padroes.stream().map(PadraoGovernado::linha).distinct().count())
                .as("o extrator precisa alcancar toda linha 'paths:' do arquivo, nao um subconjunto")
                .isEqualTo(linhasDePaths);
    }

    @Test
    void todoPadraoGovernadoEAceitoPeloPathPatternParser() throws Exception {
        PathPatternParser parser = new PathPatternParser();
        List<String> rejeitados = new ArrayList<>();

        for (PadraoGovernado governado : padroesGovernados()) {
            try {
                parser.parse(governado.padrao());
            } catch (RuntimeException recusado) {
                rejeitados.add(GOVERNANCA.getFileName() + ":" + governado.linha() + "  "
                        + governado.padrao() + "  -> " + recusado.getMessage());
            }
        }

        assertThat(rejeitados)
                .as("o Spring Security resolve estes padroes por AntPathMatcher hoje, mas PathPattern e o destino "
                        + "do framework e recusa construcoes como ** no meio do padrao; um padrao recusado aqui vira "
                        + "falha de boot do SecurityConfig no dia da migracao, com a autorizacao da rota junto")
                .isEmpty();
    }
}
