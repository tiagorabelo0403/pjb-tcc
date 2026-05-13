package com.tcc.pjb.backend.service.institutional.workbench;

import com.tcc.pjb.backend.model.dto.institutional.InstitutionalWorkbenchProfileResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalWorkbenchProfileResolver {

    public InstitutionalWorkbenchProfileResponse resolve(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario");
        TipoUsuario tipoUsuario = usuario.getTipoUsuario();
        ProfileBlueprint blueprint = blueprint(tipoUsuario);
        return new InstitutionalWorkbenchProfileResponse(
                blueprint.actorClass(),
                blueprint.institutionalBranch(),
                federativeSphere(usuario, blueprint),
                blueprint.headline(),
                blueprint.materialFocus(),
                blueprint.justiceMesh(),
                territorialAnchors(usuario),
                usuario.getEspecialidades(),
                blueprint.capabilities()
        );
    }

    private String federativeSphere(Usuario usuario, ProfileBlueprint blueprint) {
        EnteFederativo enteFederativo = usuario.getEnteFederativo();
        if (enteFederativo != null) {
            return enteFederativo.name();
        }
        return blueprint.federativeSphere();
    }

    private List<String> territorialAnchors(Usuario usuario) {
        Set<String> anchors = new LinkedHashSet<>();
        if (usuario.getUf() != null && !usuario.getUf().isBlank()) {
            anchors.add(usuario.getUf().trim().toUpperCase(Locale.ROOT));
        }
        if (usuario.getComarca() != null && !usuario.getComarca().isBlank()) {
            anchors.add(usuario.getComarca().trim().toUpperCase(Locale.ROOT));
        }
        if (usuario.getEnteFederativo() != null) {
            anchors.add(usuario.getEnteFederativo().name());
        }
        if (anchors.isEmpty()) {
            anchors.add("MALHA_NACIONAL");
        }
        return List.copyOf(anchors);
    }

    private ProfileBlueprint blueprint(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return new ProfileBlueprint(
                    "INSTITUCIONAL",
                    "INSTITUCIONAL",
                    "INDETERMINADA",
                    "Workbench institucional",
                    "Atuação institucional",
                    List.of("ESTADUAL", "FEDERAL"),
                    List.of("VISIBILIDADE", "FILA_OPERACIONAL")
            );
        }
        return switch (tipoUsuario) {
            case DELEGADO_POLICIA -> new ProfileBlueprint(
                    "DELEGACIA_ESTADUAL",
                    "POLICIA_CIVIL_ESTADUAL",
                    "ESTADUAL",
                    "Gabinete investigativo estadual",
                    "Investigação criminal e cautelares",
                    List.of("ESTADUAL", "CRIMINAL"),
                    List.of("DILIGENCIA", "INQUERITO", "MALHA_MATERIAL", "FILA_OPERACIONAL")
            );
            case DELEGADO_POLICIA_FEDERAL -> new ProfileBlueprint(
                    "POLICIA_FEDERAL",
                    "POLICIA_FEDERAL",
                    "FEDERAL",
                    "Gabinete investigativo federal",
                    "Investigação federal e medidas cautelares",
                    List.of("FEDERAL", "CRIMINAL", "INTERESTADUAL"),
                    List.of("DILIGENCIA", "INQUERITO", "MALHA_MATERIAL", "COOPERACAO")
            );
            case MEMBRO_MINISTERIO_PUBLICO -> new ProfileBlueprint(
                    "MINISTERIO_PUBLICO_ESTADUAL",
                    "MPE",
                    "ESTADUAL",
                    "Workbench ministerial estadual",
                    "Tutela penal, cível e coletiva estadual",
                    List.of("ESTADUAL", "PENAL", "CIVEL"),
                    List.of("MANIFESTACAO", "PARECER", "RECURSO", "FILA_OPERACIONAL")
            );
            case PROMOTOR_ELEITORAL -> new ProfileBlueprint(
                    "MINISTERIO_PUBLICO_ELEITORAL",
                    "MPELEITORAL",
                    "FEDERAL",
                    "Workbench ministerial eleitoral",
                    "Atuação eleitoral e controle da lisura do pleito",
                    List.of("ELEITORAL"),
                    List.of("MANIFESTACAO", "PARECER", "RECURSO", "MALHA_ELEITORAL")
            );
            case PROMOTOR_TRABALHISTA -> new ProfileBlueprint(
                    "MINISTERIO_PUBLICO_TRABALHISTA",
                    "MPT",
                    "FEDERAL",
                    "Workbench ministerial trabalhista",
                    "Tutela coletiva e trabalhista",
                    List.of("TRABALHO"),
                    List.of("MANIFESTACAO", "PARECER", "RECURSO", "MALHA_TRABALHISTA")
            );
            case PROCURADOR_GERAL_REPUBLICA -> new ProfileBlueprint(
                    "MINISTERIO_PUBLICO_FEDERAL",
                    "MPF",
                    "FEDERAL",
                    "Workbench ministerial federal",
                    "Tutela penal, cível e coletiva federal",
                    List.of("FEDERAL", "CRIMINAL", "CIVEL", "ELEITORAL"),
                    List.of("MANIFESTACAO", "PARECER", "RECURSO", "CONTROLE_EXTERNO")
            );
            case DEFENSOR_PUBLICO -> new ProfileBlueprint(
                    "DEFENSORIA_ESTADUAL",
                    "DPE",
                    "ESTADUAL",
                    "Workbench da Defensoria estadual",
                    "Proteção integral e acesso à justiça estadual",
                    List.of("ESTADUAL", "CIVEL", "CRIMINAL", "FAMILIA"),
                    List.of("PETICAO", "RECURSO", "GRATUIDADE", "VULNERABILIDADE")
            );
            case DEFENSOR_PUBLICO_FEDERAL -> new ProfileBlueprint(
                    "DEFENSORIA_FEDERAL",
                    "DPU",
                    "FEDERAL",
                    "Workbench da Defensoria da União",
                    "Proteção integral e acesso à justiça federal",
                    List.of("FEDERAL", "PREVIDENCIARIO", "CIVEL", "CRIMINAL"),
                    List.of("PETICAO", "RECURSO", "GRATUIDADE", "ATRIBUICAO_FEDERAL")
            );
            case PROCURADORIA_MUNICIPAL -> new ProfileBlueprint(
                    "PROCURADORIA_MUNICIPAL",
                    "PGM",
                    "MUNICIPAL",
                    "Workbench da procuradoria municipal",
                    "Defesa judicial e consultiva do Município",
                    List.of("MUNICIPAL", "FAZENDA_PUBLICA", "CIVEL"),
                    List.of("CONTESTACAO", "PARECER", "RECURSO", "DEFESA_ENTE")
            );
            case PROCURADORIA_ESTADUAL -> new ProfileBlueprint(
                    "PROCURADORIA_ESTADUAL",
                    "PGE",
                    "ESTADUAL",
                    "Workbench da procuradoria estadual",
                    "Defesa judicial e consultiva do Estado",
                    List.of("ESTADUAL", "FAZENDA_PUBLICA", "CIVEL"),
                    List.of("CONTESTACAO", "PARECER", "RECURSO", "DEFESA_ENTE")
            );
            case PROCURADORIA_FEDERAL, PROCURADOR -> new ProfileBlueprint(
                    "PROCURADORIA_FEDERAL",
                    usuarioFiscal(tipoUsuario) ? "PGFN" : "AGU_PGF",
                    "FEDERAL",
                    usuarioFiscal(tipoUsuario) ? "Workbench fiscal federal" : "Workbench da advocacia pública federal",
                    usuarioFiscal(tipoUsuario) ? "Cobrança, execução fiscal e dívida ativa" : "Defesa judicial e consultiva federal",
                    usuarioFiscal(tipoUsuario) ? List.of("FEDERAL", "EXECUCAO_FISCAL", "TRIBUTARIO") : List.of("FEDERAL", "FAZENDA_PUBLICA", "CIVEL"),
                    usuarioFiscal(tipoUsuario) ? List.of("EXECUCAO_FISCAL", "RECURSO", "RECUPERACAO_CREDITO") : List.of("CONTESTACAO", "PARECER", "RECURSO", "DEFESA_ENTE")
            );
            default -> new ProfileBlueprint(
                    tipoUsuario.name(),
                    tipoUsuario.name(),
                    "INDETERMINADA",
                    "Workbench institucional",
                    "Atuação institucional",
                    List.of("ESTADUAL", "FEDERAL"),
                    List.of("VISIBILIDADE", "FILAS", "ACTIONS")
            );
        };
    }

    private boolean usuarioFiscal(TipoUsuario tipoUsuario) {
        return tipoUsuario == TipoUsuario.PROCURADOR;
    }

    private record ProfileBlueprint(
            String actorClass,
            String institutionalBranch,
            String federativeSphere,
            String headline,
            String materialFocus,
            List<String> justiceMesh,
            List<String> capabilities
    ) {
    }
}
