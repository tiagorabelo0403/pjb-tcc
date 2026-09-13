package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Entidade JPA no corpo da resposta amarra o contrato público ao schema do banco e arrasta as
 * associações da entidade junto. A verificação é por fonte, e não por ArchUnit, porque o parâmetro
 * genérico de {@code ResponseEntity<Instituicao>} é apagado no bytecode: o modelo de classes só
 * enxerga {@code ResponseEntity}.
 */
class PjbControllerNaoDevolveEntidadeJpaTest {

    private static final Path BACKEND = Path.of("src/main/java/com/tcc/pjb/backend");
    private static final Path ENTIDADES = BACKEND.resolve("model/entity");

    /** {@code ResponseEntity<X>} e também {@code ResponseEntity<List<X>>}, {@code Page<X>} etc. */
    private static final Pattern TIPO_DE_RESPOSTA = Pattern.compile(
            "ResponseEntity\\s*<\\s*(?:(?:List|Set|Page|Slice|Collection|Optional|Iterable)\\s*<\\s*)?([A-Z]\\w*)");

    private static Set<String> entidadesJpa() throws IOException {
        Set<String> nomes = new TreeSet<>();
        try (Stream<Path> paths = Files.walk(ENTIDADES)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                if (ler(p).contains("@Entity")) {
                    nomes.add(p.getFileName().toString().replace(".java", ""));
                }
            });
        }
        return nomes;
    }

    private static List<String> entidadesDevolvidasPor(Path controller, Set<String> entidades) {
        List<String> achados = new ArrayList<>();
        String fonte = ler(controller);
        Matcher matcher = TIPO_DE_RESPOSTA.matcher(fonte);
        while (matcher.find()) {
            String tipo = matcher.group(1);
            if (entidades.contains(tipo)) {
                long linha = fonte.substring(0, matcher.start()).lines().count() + 1;
                achados.add(BACKEND.relativize(controller) + ":" + linha + " -> " + tipo);
            }
        }
        return achados;
    }

    @Test
    void oDetectorReconheceUmPositivoConhecidoESeparaODtoDeMesmoPrefixo() {
        Set<String> entidades = Set.of("Instituicao", "SecretariaInstitucionalItem");

        Matcher entidadeNua = TIPO_DE_RESPOSTA.matcher("public ResponseEntity<Instituicao> criar(");
        assertThat(entidadeNua.find()).isTrue();
        assertThat(entidades).contains(entidadeNua.group(1));

        Matcher entidadeEmLista =
                TIPO_DE_RESPOSTA.matcher("public ResponseEntity<List<SecretariaInstitucionalItem>> listar(");
        assertThat(entidadeEmLista.find()).isTrue();
        assertThat(entidades).contains(entidadeEmLista.group(1));

        Matcher dtoDeMesmoPrefixo =
                TIPO_DE_RESPOSTA.matcher("public ResponseEntity<List<SecretariaInstitucionalItemResponse>> listar(");
        assertThat(dtoDeMesmoPrefixo.find()).isTrue();
        assertThat(entidades)
                .as("o DTO que compartilha o prefixo do nome da entidade não pode ser confundido com ela")
                .doesNotContain(dtoDeMesmoPrefixo.group(1));
    }

    @Test
    void nenhumControllerDevolveEntidadeJpaNoCorpoDaResposta() throws IOException {
        Set<String> entidades = entidadesJpa();
        List<String> violacoes = new ArrayList<>();
        int controllersVaridos = 0;

        try (Stream<Path> paths = Files.walk(BACKEND)) {
            List<Path> controllers = paths
                    .filter(p -> p.toString().endsWith("Controller.java"))
                    .toList();
            controllersVaridos = controllers.size();
            for (Path controller : controllers) {
                violacoes.addAll(entidadesDevolvidasPor(controller, entidades));
            }
        }

        assertThat(entidades)
                .as("sem entidades catalogadas a comparação seguinte não acusaria nada")
                .hasSizeGreaterThan(100);
        assertThat(controllersVaridos)
                .as("sem controllers varridos a asserção seguinte passaria por vacuidade")
                .isGreaterThan(50);

        assertThat(violacoes)
                .as("Controller devolvendo entidade JPA no corpo da resposta. O contrato público passa a "
                    + "depender do schema do banco, e a serialização arrasta as associações da entidade — "
                    + "com LAZY e open-in-view=false isso vira erro em requisição real. Devolva um record "
                    + "de resposta em model.dto, com as associações achatadas em identificador.")
                .isEmpty();
    }

    private static String ler(Path p) {
        try { return Files.readString(p); } catch (IOException e) { return ""; }
    }
}
