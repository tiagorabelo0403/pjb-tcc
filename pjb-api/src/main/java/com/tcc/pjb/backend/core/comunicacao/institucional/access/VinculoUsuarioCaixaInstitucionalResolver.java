package com.tcc.pjb.backend.core.comunicacao.institucional.access;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.CatalogoInstitucionalUnificadoService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AbrangenciaVinculoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.PapelEquipe;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public class VinculoUsuarioCaixaInstitucionalResolver {

    private final CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService;
    private final EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService;
    private final MatrizCapacidadeCaixaInstitucionalService matrizCapacidadeCaixaInstitucionalService;

    public VinculoUsuarioCaixaInstitucionalResolver(CatalogoInstitucionalUnificadoService catalogoInstitucionalUnificadoService,
                                                    EstruturaCaixaInstitucionalService estruturaCaixaInstitucionalService,
                                                    MatrizCapacidadeCaixaInstitucionalService matrizCapacidadeCaixaInstitucionalService) {
        this.catalogoInstitucionalUnificadoService = Objects.requireNonNull(catalogoInstitucionalUnificadoService);
        this.estruturaCaixaInstitucionalService = Objects.requireNonNull(estruturaCaixaInstitucionalService);
        this.matrizCapacidadeCaixaInstitucionalService = Objects.requireNonNull(matrizCapacidadeCaixaInstitucionalService);
    }

    public List<VinculoUsuarioCaixaInstitucional> resolver(Usuario usuario,
                                                           DestinatarioInstitucionalKind destinatarioKind,
                                                           String uf,
                                                           String comarca) {
        Objects.requireNonNull(usuario, "usuario");
        List<UnidadeInstitucional> unidades = catalogoInstitucionalUnificadoService.listarPorTipo(destinatarioKind).stream()
                .filter(unidade -> matchesUf(unidade, uf))
                .filter(unidade -> matchesComarca(unidade, comarca))
                .toList();
        return resolverInterno(usuario, unidades, null, null);
    }

    public List<VinculoUsuarioCaixaInstitucional> resolver(Usuario usuario,
                                                           UnidadeInstitucional unidade,
                                                           CaixaInstitucional caixa) {
        Objects.requireNonNull(usuario, "usuario");
        return resolverInterno(usuario, List.of(unidade), unidade.codigo(), caixa.codigo());
    }

    private List<VinculoUsuarioCaixaInstitucional> resolverInterno(Usuario usuario,
                                                                   List<UnidadeInstitucional> unidades,
                                                                   String unidadeCodigo,
                                                                   String caixaCodigo) {
        LinkedHashSet<VinculoUsuarioCaixaInstitucional> vinculos = new LinkedHashSet<>();
        for (UnidadeInstitucional unidade : unidades) {
            if (!isTerritorialmenteElegivel(usuario, unidade)) {
                continue;
            }
            for (CaixaInstitucional caixa : estruturaCaixaInstitucionalService.expandir(unidade)) {
                if (unidadeCodigo != null && !unidade.codigo().equalsIgnoreCase(unidadeCodigo)) {
                    continue;
                }
                if (caixaCodigo != null && !caixa.codigo().equalsIgnoreCase(caixaCodigo)) {
                    continue;
                }
                for (FuncaoOperacionalInstitucional funcao : inferirFuncoes(usuario, unidade, caixa)) {
                    vinculos.add(new VinculoUsuarioCaixaInstitucional(
                            usuario.getId(),
                            unidade,
                            caixa,
                            funcao,
                            determinarAbrangencia(usuario, unidade),
                            matrizCapacidadeCaixaInstitucionalService.capacidades(funcao),
                            buildJustificativa(usuario, unidade, caixa, funcao)
                    ));
                }
            }
        }
        return vinculos.stream()
                .sorted(Comparator.comparing((VinculoUsuarioCaixaInstitucional v) -> v.unidade().codigo())
                        .thenComparing(v -> v.caixa().codigo())
                        .thenComparing(v -> v.funcaoOperacional().name()))
                .toList();
    }

    private boolean matchesUf(UnidadeInstitucional unidade, String uf) {
        if (uf == null || uf.isBlank()) {
            return true;
        }
        return unidade.matchesUf(uf);
    }

    private boolean matchesComarca(UnidadeInstitucional unidade, String comarca) {
        if (comarca == null || comarca.isBlank()) {
            return true;
        }
        return unidade.matchesComarca(comarca);
    }

    private boolean isTerritorialmenteElegivel(Usuario usuario, UnidadeInstitucional unidade) {
        if (usuario.getTipoUsuario() == null) {
            return false;
        }
        if (usuario.getTipoUsuario().isAdmin()) {
            return true;
        }
        if (isPerfilNacional(usuario.getTipoUsuario(), unidade.destinatarioKind())) {
            return true;
        }
        if (unidade.uf() != null && usuario.getUf() != null && !unidade.uf().equalsIgnoreCase(usuario.getUf())) {
            return false;
        }
        if (unidade.comarca() != null && usuario.getComarca() != null && !unidade.matchesComarca(usuario.getComarca())) {
            return false;
        }
        return true;
    }

    private AbrangenciaVinculoInstitucional determinarAbrangencia(Usuario usuario, UnidadeInstitucional unidade) {
        if (usuario.getTipoUsuario() != null && (usuario.getTipoUsuario().isAdmin() || isPerfilNacional(usuario.getTipoUsuario(), unidade.destinatarioKind()))) {
            return AbrangenciaVinculoInstitucional.NACIONAL;
        }
        if (unidade.comarca() != null && usuario.getComarca() != null && unidade.matchesComarca(usuario.getComarca())) {
            return AbrangenciaVinculoInstitucional.UNIDADE;
        }
        if (unidade.uf() != null && usuario.getUf() != null && unidade.uf().equalsIgnoreCase(usuario.getUf())) {
            return AbrangenciaVinculoInstitucional.UF;
        }
        return AbrangenciaVinculoInstitucional.UNIDADE;
    }

    private List<FuncaoOperacionalInstitucional> inferirFuncoes(Usuario usuario,
                                                                UnidadeInstitucional unidade,
                                                                CaixaInstitucional caixa) {
        LinkedHashSet<FuncaoOperacionalInstitucional> funcoes = new LinkedHashSet<>();
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return List.of();
        }
        if (tipo.isAdmin()) {
            funcoes.add(FuncaoOperacionalInstitucional.GESTOR_CAIXA);
            if (caixa.tipo() == TipoCaixaInstitucional.CAIXA_SUBSTITUICAO) {
                funcoes.add(FuncaoOperacionalInstitucional.SUBSTITUTO);
            }
            return List.copyOf(funcoes);
        }
        if (!matchesInstituicao(tipo, unidade.destinatarioKind())) {
            return List.of();
        }
        if (isTitular(tipo, unidade.destinatarioKind())) {
            if (caixa.tipo() == TipoCaixaInstitucional.CAIXA_SUBSTITUICAO) {
                funcoes.add(FuncaoOperacionalInstitucional.SUBSTITUTO);
            } else if (caixa.tipo() == TipoCaixaInstitucional.CAIXA_GABINETE_FUNCIONAL) {
                funcoes.add(FuncaoOperacionalInstitucional.MEMBRO_TITULAR);
            } else {
                funcoes.add(FuncaoOperacionalInstitucional.MEMBRO_TITULAR);
            }
        }
        if (usuario.getPapelEquipe() == PapelEquipe.COORDENADOR || usuario.getPapelEquipe() == PapelEquipe.ADMINISTRADOR) {
            funcoes.add(FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE);
        }
        if (isAssessorOuApoio(usuario, unidade.destinatarioKind(), caixa.tipo())) {
            funcoes.add(usuario.getTipoUsuario().isAssessor()
                    ? FuncaoOperacionalInstitucional.ASSESSOR_INSTITUCIONAL
                    : FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM);
        }
        if (isApoioTecnico(tipo, unidade.destinatarioKind())) {
            funcoes.add(FuncaoOperacionalInstitucional.APOIO_TECNICO_SETORIAL);
        }
        if (caixa.tipo() == TipoCaixaInstitucional.CAIXA_SUBSTITUICAO && isTitular(tipo, unidade.destinatarioKind())) {
            funcoes.add(FuncaoOperacionalInstitucional.SUBSTITUTO);
        }
        if (caixa.tipo() == TipoCaixaInstitucional.CAIXA_TRIAGEM && supportsTriagem(tipo, unidade.destinatarioKind())) {
            funcoes.add(FuncaoOperacionalInstitucional.SERVIDOR_TRIAGEM);
        }
        return List.copyOf(funcoes);
    }

    private boolean matchesInstituicao(TipoUsuario tipo, DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO -> tipo.isMinisterioPublico() || tipo.isAssessor() || tipo.isServidorJudiciario();
            case DEFENSORIA_PUBLICA -> tipo.isDefensoriaPublica() || tipo.isAssessor() || tipo.isServidorJudiciario();
            case ADVOCACIA_PUBLICA, FAZENDA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU -> tipo.isProcuradoria() || tipo.isAssessor() || tipo.isServidorJudiciario();
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL, POLICIA_PENAL, UNIDADE_PRISIONAL -> tipo.isSegurancaPublica() || tipo.isServidorJudiciario();
            case CONSELHO_TUTELAR -> false;
            case PERICIA_JUDICIAL, PERITO_JUDICIAL -> tipo.isPerito() || tipo.isServidorJudiciario();
            case CONTADORIA_JUDICIAL -> tipo == TipoUsuario.CONTADOR_JUDICIAL || tipo.isServidorJudiciario();
            case EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL -> tipo == TipoUsuario.PSICOLOGO_JUDICIAL || tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL || tipo.isServidorJudiciario();
            case CEJUSC -> tipo.isConciliacaoMediacao() || tipo.isServidorJudiciario();
            case CARTORIO_EXTRAJUDICIAL -> tipo.isCartorioExtrajudicial();
            case ORGAO_TECNICO_CONVENIADO -> tipo.isPerito() || tipo.isSaude();
            case JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO -> tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario();
        };
    }

    private boolean isTitular(TipoUsuario tipo, DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case MINISTERIO_PUBLICO -> tipo.isMinisterioPublico();
            case DEFENSORIA_PUBLICA -> tipo.isDefensoriaPublica();
            case ADVOCACIA_PUBLICA, FAZENDA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU -> tipo.isProcuradoria();
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL -> tipo == TipoUsuario.DELEGADO_POLICIA || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL;
            case POLICIA_PENAL, UNIDADE_PRISIONAL -> tipo.isSegurancaPublica();
            case CONSELHO_TUTELAR -> tipo.isServidorJudiciario();
            case PERICIA_JUDICIAL, PERITO_JUDICIAL -> tipo.isPerito();
            case CONTADORIA_JUDICIAL -> tipo == TipoUsuario.CONTADOR_JUDICIAL;
            case EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL -> tipo == TipoUsuario.PSICOLOGO_JUDICIAL || tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL;
            case CEJUSC -> tipo == TipoUsuario.CONCILIADOR_CEJUSC || tipo == TipoUsuario.MEDIADOR;
            case CARTORIO_EXTRAJUDICIAL -> tipo == TipoUsuario.TABELIAO || tipo == TipoUsuario.REGISTRADOR_IMOVEIS;
            case ORGAO_TECNICO_CONVENIADO -> tipo.isPerito() || tipo.isSaude();
            case JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO -> tipo.isMagistratura();
        };
    }

    private boolean isAssessorOuApoio(Usuario usuario, DestinatarioInstitucionalKind kind, TipoCaixaInstitucional tipoCaixa) {
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == null) {
            return false;
        }
        if (tipo.isAssessor()) {
            return switch (kind) {
                case MINISTERIO_PUBLICO, DEFENSORIA_PUBLICA, ADVOCACIA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU, FAZENDA_PUBLICA,
                        CONTADORIA_JUDICIAL, EQUIPE_PSICOSSOCIAL, CEJUSC, JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO, ORGAO_TECNICO_CONVENIADO -> true;
                default -> false;
            };
        }
        if (tipo.isServidorJudiciario()) {
            return switch (kind) {
                case MINISTERIO_PUBLICO,
                        DEFENSORIA_PUBLICA,
                        ADVOCACIA_PUBLICA,
                        PROCURADORIA_ESTADO,
                        PROCURADORIA_MUNICIPIO,
                        AGU,
                        FAZENDA_PUBLICA,
                        CONTADORIA_JUDICIAL,
                        EQUIPE_PSICOSSOCIAL,
                        CEJUSC,
                        JUIZO_DEPRECADO,
                        ORGAO_JUDICIAL_EXTERNO,
                        ORGAO_TECNICO_CONVENIADO -> true;
                default -> false;
            };
        }
        return false;
    }

    private boolean supportsTriagem(TipoUsuario tipo, DestinatarioInstitucionalKind kind) {
        if (tipo.isAssessor()) {
            return true;
        }
        return switch (kind) {
            case MINISTERIO_PUBLICO, DEFENSORIA_PUBLICA, ADVOCACIA_PUBLICA, PROCURADORIA_ESTADO, PROCURADORIA_MUNICIPIO, AGU, FAZENDA_PUBLICA, CONTADORIA_JUDICIAL, EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL, CEJUSC, JUIZO_DEPRECADO, ORGAO_JUDICIAL_EXTERNO, ORGAO_TECNICO_CONVENIADO -> tipo.isServidorJudiciario() || tipo.isAssessor();
            case DELEGACIA_POLICIA, DELEGACIA_POLICIA_CIVIL, DELEGACIA_POLICIA_FEDERAL, POLICIA_PENAL, UNIDADE_PRISIONAL -> tipo == TipoUsuario.AGENTE_POLICIAL || tipo == TipoUsuario.ESCRIVAO_POLICIAL;
            case CARTORIO_EXTRAJUDICIAL -> tipo == TipoUsuario.ESCREVENTE_CARTORIO;
            default -> false;
        };
    }

    private boolean isApoioTecnico(TipoUsuario tipo, DestinatarioInstitucionalKind kind) {
        return switch (kind) {
            case PERICIA_JUDICIAL, PERITO_JUDICIAL -> tipo.isPerito() || tipo.isServidorJudiciario();
            case CONTADORIA_JUDICIAL -> tipo == TipoUsuario.CONTADOR_JUDICIAL || tipo.isServidorJudiciario();
            case EQUIPE_PSICOSSOCIAL, ASSISTENTE_SOCIAL_JUDICIAL -> tipo == TipoUsuario.PSICOLOGO_JUDICIAL || tipo == TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL || tipo.isServidorJudiciario();
            case CEJUSC -> tipo.isConciliacaoMediacao() || tipo.isServidorJudiciario();
            case ORGAO_TECNICO_CONVENIADO -> tipo.isPerito() || tipo.isSaude() || tipo.isServidorJudiciario();
            default -> false;
        };
    }

    private boolean isPerfilNacional(TipoUsuario tipo, DestinatarioInstitucionalKind kind) {
        return switch (tipo) {
            case PROCURADOR_GERAL_REPUBLICA -> kind == DestinatarioInstitucionalKind.MINISTERIO_PUBLICO;
            case DEFENSOR_PUBLICO_FEDERAL -> kind == DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA;
            case PROCURADORIA_FEDERAL -> kind == DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA
                    || kind == DestinatarioInstitucionalKind.AGU
                    || kind == DestinatarioInstitucionalKind.FAZENDA_PUBLICA;
            case DELEGADO_POLICIA_FEDERAL -> kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA
                    || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL;
            case MINISTRO -> kind == DestinatarioInstitucionalKind.JUIZO_DEPRECADO || kind == DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO;
            default -> false;
        };
    }

    private String buildJustificativa(Usuario usuario,
                                      UnidadeInstitucional unidade,
                                      CaixaInstitucional caixa,
                                      FuncaoOperacionalInstitucional funcao) {
        List<String> fragmentos = new ArrayList<>();
        fragmentos.add("tipo=" + usuario.getTipoUsuario().name());
        fragmentos.add("destinatario=" + unidade.destinatarioKind().name());
        fragmentos.add("caixa=" + caixa.tipo().name());
        fragmentos.add("funcao=" + funcao.name());
        if (usuario.getUf() != null && unidade.uf() != null) {
            fragmentos.add("uf=" + unidade.uf());
        }
        if (usuario.getComarca() != null && unidade.comarca() != null && unidade.matchesComarca(usuario.getComarca())) {
            fragmentos.add("comarca=" + unidade.comarca());
        }
        return String.join(";", fragmentos);
    }
}
