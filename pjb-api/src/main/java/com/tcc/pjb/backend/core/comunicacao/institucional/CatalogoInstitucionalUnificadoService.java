package com.tcc.pjb.backend.core.comunicacao.institucional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalCatalogGovernanceOverlayService;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.AlvoInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CaixaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.CanalEntregaInstitucional;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.ResolucaoDestinoInstitucionalResult;
import com.tcc.pjb.backend.core.comunicacao.institucional.model.UnidadeInstitucional;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.PapelProcessualInstitucional;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusComunicacaoInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoCaixaInstitucional;

@Service
public class CatalogoInstitucionalUnificadoService {

    private static final String CATALOG_VERSION = "PJB-CIU-2026.03-B1B2";
    private static final List<String> UFS = List.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );
    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("AC", "Acre"),
            Map.entry("AL", "Alagoas"),
            Map.entry("AP", "Amapá"),
            Map.entry("AM", "Amazonas"),
            Map.entry("BA", "Bahia"),
            Map.entry("CE", "Ceará"),
            Map.entry("DF", "Distrito Federal"),
            Map.entry("ES", "Espírito Santo"),
            Map.entry("GO", "Goiás"),
            Map.entry("MA", "Maranhão"),
            Map.entry("MT", "Mato Grosso"),
            Map.entry("MS", "Mato Grosso do Sul"),
            Map.entry("MG", "Minas Gerais"),
            Map.entry("PA", "Pará"),
            Map.entry("PB", "Paraíba"),
            Map.entry("PR", "Paraná"),
            Map.entry("PE", "Pernambuco"),
            Map.entry("PI", "Piauí"),
            Map.entry("RJ", "Rio de Janeiro"),
            Map.entry("RN", "Rio Grande do Norte"),
            Map.entry("RS", "Rio Grande do Sul"),
            Map.entry("RO", "Rondônia"),
            Map.entry("RR", "Roraima"),
            Map.entry("SC", "Santa Catarina"),
            Map.entry("SP", "São Paulo"),
            Map.entry("SE", "Sergipe"),
            Map.entry("TO", "Tocantins")
    );

    private final List<UnidadeInstitucional> unidades;
    private final Map<DestinatarioInstitucionalKind, List<UnidadeInstitucional>> unitsByKind;
    private final InstitutionalCatalogGovernanceOverlayService governanceOverlayService;

    public CatalogoInstitucionalUnificadoService(ObjectProvider<InstitutionalCatalogGovernanceOverlayService> governanceOverlayProvider) {
        this.unidades = List.copyOf(buildSeeds());
        EnumMap<DestinatarioInstitucionalKind, List<UnidadeInstitucional>> byKind = new EnumMap<>(DestinatarioInstitucionalKind.class);
        for (DestinatarioInstitucionalKind kind : DestinatarioInstitucionalKind.values()) {
            byKind.put(kind, unidades.stream()
                    .filter(unit -> unit.destinatarioKind() == kind)
                    .sorted(Comparator.comparing(UnidadeInstitucional::codigo))
                    .toList());
        }
        this.unitsByKind = Map.copyOf(byKind);
        this.governanceOverlayService = governanceOverlayProvider.getIfAvailable();
    }

    public ResolucaoDestinoInstitucionalResult resolver(ResolucaoDestinoInstitucionalRequest request) {
        Objects.requireNonNull(request, "request");
        List<ScoredUnit> scored = unitsByKind.getOrDefault(request.destinatarioKind(), List.of()).stream()
                .map(this::applyGovernance)
                .filter(UnidadeInstitucional::ativa)
                .map(unit -> score(request, unit))
                .filter(ScoredUnit::eligible)
                .sorted(Comparator.comparingInt(ScoredUnit::score).reversed().thenComparing(s -> s.unit().codigo()))
                .toList();
        UnidadeInstitucional selected = scored.isEmpty() ? buildSyntheticFallback(request) : scored.getFirst().unit();
        List<String> justificativas = scored.isEmpty()
                ? List.of(
                        "catálogo sem correspondência específica; fallback sintético institucional criado",
                        "destinatario=" + request.destinatarioKind().name(),
                        "papel=" + request.papelProcessual().name())
                : composeJustifications(request, scored);
        return buildResult(request, selected, justificativas);
    }

    public ResolucaoDestinoInstitucionalResult resolverPreferindoCodigo(ResolucaoDestinoInstitucionalRequest request,
                                                                        String unidadeCodigo,
                                                                        String justificativaForcada) {
        Objects.requireNonNull(request, "request");
        UnidadeInstitucional selected = unitsByKind.getOrDefault(request.destinatarioKind(), List.of()).stream()
                .map(this::applyGovernance)
                .filter(unit -> unit.codigo().equalsIgnoreCase(unidadeCodigo) && unit.ativa())
                .findFirst()
                .orElseGet(() -> buildSyntheticFallback(request));
        List<String> justificativas = new ArrayList<>();
        justificativas.add("catálogo=" + CATALOG_VERSION);
        justificativas.add(justificativaForcada == null || justificativaForcada.isBlank() ? "unidade preferencial por governança" : justificativaForcada);
        justificativas.add("unidade=" + selected.codigo());
        return buildResult(request, selected, List.copyOf(justificativas));
    }

    public List<UnidadeInstitucional> listarPorTipo(DestinatarioInstitucionalKind destinatarioKind) {
        if (destinatarioKind == null) {
            return unidades.stream().map(this::applyGovernance).toList();
        }
        return unitsByKind.getOrDefault(destinatarioKind, List.of()).stream().map(this::applyGovernance).toList();
    }

    public String version() {
        return CATALOG_VERSION;
    }

    private ResolucaoDestinoInstitucionalResult buildResult(ResolucaoDestinoInstitucionalRequest request,
                                                            UnidadeInstitucional selected,
                                                            List<String> justificativas) {
        AlvoInstitucional alvo = new AlvoInstitucional(
                request.processoId(),
                request.processoNumero(),
                request.destinatarioKind(),
                request.papelProcessual(),
                selected,
                selected.caixaPrincipal(),
                choosePrimaryChannel(request, selected),
                chooseEligibleChannels(request, selected),
                request.fundamentoLegal(),
                buildResolutionHash(request, selected, justificativas)
        );
        return new ResolucaoDestinoInstitucionalResult(alvo, StatusComunicacaoInstitucional.RESOLVIDA, justificativas, CATALOG_VERSION);
    }

    private UnidadeInstitucional applyGovernance(UnidadeInstitucional unit) {
        return governanceOverlayService == null ? unit : governanceOverlayService.apply(unit);
    }

    private List<ScoredUnit> composeTopMatches(List<ScoredUnit> scored) {
        return scored.stream().limit(3).toList();
    }

    private List<String> composeJustifications(ResolucaoDestinoInstitucionalRequest request, List<ScoredUnit> scored) {
        List<String> lines = new ArrayList<>();
        lines.add("catálogo=" + CATALOG_VERSION);
        for (ScoredUnit candidate : composeTopMatches(scored)) {
            lines.add(candidate.unit().codigo() + " score=" + candidate.score() + " razões=" + String.join(",", candidate.reasons()));
        }
        if (request.exigeCienciaPessoal()) {
            lines.add("ciência pessoal preferencial habilitada");
        }
        return List.copyOf(lines);
    }

    private ScoredUnit score(ResolucaoDestinoInstitucionalRequest request, UnidadeInstitucional unit) {
        List<String> reasons = new ArrayList<>(8);
        int score = 10;
        if (unit.destinatarioKind() != request.destinatarioKind()) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("destinatário divergente"));
        }
        reasons.add("destinatário=" + request.destinatarioKind().name());
        if (!unit.matchesUf(request.uf())) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("uf divergente"));
        }
        if (request.uf() != null && unit.uf() != null) {
            score += 25;
            reasons.add("uf");
        }
        if (!unit.matchesComarca(request.comarca())) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("comarca divergente"));
        }
        if (request.comarca() != null && unit.comarca() != null) {
            score += 18;
            reasons.add("comarca");
        }
        if (!unit.matchesForo(request.foro())) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("foro divergente"));
        }
        if (request.foro() != null && unit.foro() != null) {
            score += 12;
            reasons.add("foro");
        }
        if (!unit.matchesRamo(request.ramoDireito())) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("ramo divergente"));
        }
        if (request.ramoDireito() != null && unit.ramoDireito() != null) {
            score += 16;
            reasons.add("ramo=" + request.ramoDireito().name());
        }
        if (!unit.matchesGrau(request.grauJurisdicao())) {
            return new ScoredUnit(unit, Integer.MIN_VALUE, false, List.of("grau divergente"));
        }
        if (request.grauJurisdicao() != null && unit.grauJurisdicao() != null) {
            score += 8;
            reasons.add("grau=" + request.grauJurisdicao().name());
        }
        if (unit.papelPrincipal() == request.papelProcessual()) {
            score += 20;
            reasons.add("papel=" + request.papelProcessual().name());
        }
        if (request.unidadeSugerida() != null && unit.unidade() != null && unit.unidade().equalsIgnoreCase(request.unidadeSugerida())) {
            score += 8;
            reasons.add("unidadeSugerida");
        }
        if (request.nucleoSugerido() != null && unit.nucleo() != null && unit.nucleo().equalsIgnoreCase(request.nucleoSugerido())) {
            score += 6;
            reasons.add("nucleoSugerido");
        }
        if (request.exigeCienciaPessoal() && unit.canais().stream().anyMatch(CanalEntregaInstitucional::exigeCienciaPessoal)) {
            score += 7;
            reasons.add("cienciaPessoal");
        }
        if (unit.destinatarioKind().isInstituicaoEssencialJustica()) {
            score += 4;
            reasons.add("instituicaoEssencial");
        }
        return new ScoredUnit(unit, score, true, List.copyOf(reasons));
    }

    private List<CanalEntregaInstitucional> chooseEligibleChannels(ResolucaoDestinoInstitucionalRequest request, UnidadeInstitucional unit) {
        if (!request.exigeCienciaPessoal()) {
            return unit.canais();
        }
        List<CanalEntregaInstitucional> personal = unit.canais().stream()
                .filter(CanalEntregaInstitucional::exigeCienciaPessoal)
                .toList();
        return personal.isEmpty() ? unit.canais() : personal;
    }

    private CanalEntregaInstitucional choosePrimaryChannel(ResolucaoDestinoInstitucionalRequest request, UnidadeInstitucional unit) {
        return chooseEligibleChannels(request, unit).stream()
                .filter(CanalEntregaInstitucional::isCanalPrincipalJuridico)
                .findFirst()
                .orElseGet(unit::canalPrincipal);
    }

    private UnidadeInstitucional buildSyntheticFallback(ResolucaoDestinoInstitucionalRequest request) {
        String uf = request.uf() == null ? "NACIONAL" : request.uf();
        String codigo = request.destinatarioKind().name() + "-" + uf;
        CaixaInstitucional caixa = new CaixaInstitucional(
                codigo + "-TRIAGEM",
                "Caixa institucional de triagem",
                TipoCaixaInstitucional.CAIXA_TRIAGEM,
                codigo,
                request.destinatarioKind(),
                true,
                true
        );
        return new UnidadeInstitucional(
                codigo,
                request.destinatarioKind(),
                request.destinatarioKind().name(),
                "Unidade institucional sintética " + request.destinatarioKind().name(),
                request.uf(),
                request.comarca(),
                request.foro(),
                request.unidadeSugerida(),
                request.nucleoSugerido(),
                request.ramoDireito(),
                request.grauJurisdicao(),
                request.papelProcessual(),
                caixa,
                defaultChannelsFor(request.destinatarioKind(), request.papelProcessual()),
                inferTribunalCodigo(request.uf(), request.ramoDireito()),
                true,
                "fallback sintético até carga administrativa real"
        );
    }

    private String buildResolutionHash(ResolucaoDestinoInstitucionalRequest request, UnidadeInstitucional selected, List<String> justificativas) {
        return Hashes.sha256Hex(String.join("|",
                CATALOG_VERSION,
                String.valueOf(request.processoId()),
                String.valueOf(request.processoNumero()),
                request.destinatarioKind().name(),
                request.papelProcessual().name(),
                String.valueOf(request.ramoDireito()),
                String.valueOf(request.grauJurisdicao()),
                String.valueOf(request.uf()),
                String.valueOf(request.comarca()),
                selected.codigo(),
                selected.caixaPrincipal().codigo(),
                selected.canalPrincipal().canal().name(),
                justificativas.stream().collect(Collectors.joining(";"))
        ));
    }

    private List<UnidadeInstitucional> buildSeeds() {
        LinkedHashMap<String, UnidadeInstitucional> seeds = new LinkedHashMap<>();
        for (String uf : UFS) {
            put(seeds, buildMinisterioPublico(uf));
            put(seeds, buildDefensoriaPublica(uf));
            put(seeds, buildAdvocaciaPublica(uf));
            put(seeds, buildProcuradoriaEstado(uf));
            put(seeds, buildProcuradoriaMunicipio(uf));
            put(seeds, buildAguRegional(uf));
            put(seeds, buildFazendaPublica(uf));
            put(seeds, buildDelegacia(uf));
            put(seeds, buildDelegaciaPoliciaCivil(uf));
            put(seeds, buildDelegaciaPoliciaFederal(uf));
            put(seeds, buildPoliciaPenal(uf));
            put(seeds, buildUnidadePrisional(uf));
            put(seeds, buildConselhoTutelar(uf));
            put(seeds, buildPericia(uf));
            put(seeds, buildPeritoJudicial(uf));
            put(seeds, buildContadoria(uf));
            put(seeds, buildEquipePsicossocial(uf));
            put(seeds, buildAssistenteSocialJudicial(uf));
            put(seeds, buildCejusc(uf));
            put(seeds, buildCartorioExtrajudicial(uf));
            put(seeds, buildOrgaoTecnico(uf));
            put(seeds, buildJuizoDeprecado(uf));
        }
        put(seeds, buildMinisterioPublicoFamiliaFortaleza());
        put(seeds, buildDefensoriaFamiliaFortaleza());
        put(seeds, buildEquipePsicossocialFortaleza());
        put(seeds, buildCejuscFortaleza());
        put(seeds, buildAguNacional());
        return new ArrayList<>(seeds.values());
    }

    private UnidadeInstitucional buildMinisterioPublico(String uf) {
        return unidadePadrao(
                "MP-" + uf,
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "MP" + uf,
                "DF".equals(uf) ? "Ministério Público do Distrito Federal e Territórios" : "Ministério Público do Estado do " + nomeEstado(uf),
                uf,
                null,
                null,
                "Promotoria de Justiça Institucional",
                "Triagem Geral",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA),
                inferTribunalCodigo(uf, null),
                "catálogo estadual do Ministério Público"
        );
    }

    private UnidadeInstitucional buildDefensoriaPublica(String uf) {
        return unidadePadrao(
                "DP-" + uf,
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                "DP" + uf,
                "Defensoria Pública do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Núcleo Institucional",
                "Triagem Geral",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo(uf, null),
                "catálogo estadual da Defensoria Pública"
        );
    }

    private UnidadeInstitucional buildAdvocaciaPublica(String uf) {
        return unidadePadrao(
                "APUB-" + uf,
                DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA,
                "APUB" + uf,
                "Advocacia Pública Integrada do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Advocacia Pública",
                "Contencioso Público Geral",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo(uf, null),
                "catálogo geral de advocacia pública institucional"
        );
    }

    private UnidadeInstitucional buildProcuradoriaEstado(String uf) {
        return unidadePadrao(
                "PGE-EST-" + uf,
                DestinatarioInstitucionalKind.PROCURADORIA_ESTADO,
                "PGE" + uf,
                "Procuradoria-Geral do Estado do " + nomeEstado(uf),
                uf,
                null,
                null,
                "Procuradoria Estadual",
                "Contencioso Estadual",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.PROCURADORIA_ESTADO, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo(uf, null),
                "catálogo específico da procuradoria-geral do estado"
        );
    }

    private UnidadeInstitucional buildProcuradoriaMunicipio(String uf) {
        return unidadePadrao(
                "PGM-" + uf,
                DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO,
                "PGM" + uf,
                "Procuradoria Municipal Integrada da capital do Estado do " + nomeEstado(uf),
                uf,
                null,
                null,
                "Procuradoria Municipal",
                "Contencioso Municipal",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo(uf, null),
                "catálogo específico da procuradoria municipal"
        );
    }

    private UnidadeInstitucional buildAguRegional(String uf) {
        return unidadePadrao(
                "AGU-REG-" + uf,
                DestinatarioInstitucionalKind.AGU,
                "AGU",
                "Advocacia-Geral da União — representação federal no " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Representação Federal",
                "Contencioso da União",
                RamoDireito.PREVIDENCIARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_ENTIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.AGU, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                "TRF-" + uf,
                "catálogo regional da advocacia-geral da união"
        );
    }

    private UnidadeInstitucional buildFazendaPublica(String uf) {
        return unidadePadrao(
                "FAZENDA-" + uf,
                DestinatarioInstitucionalKind.FAZENDA_PUBLICA,
                "SEFAZ" + uf,
                "Fazenda Pública do Estado do " + nomeEstado(uf),
                uf,
                null,
                null,
                "Representação Fazendária",
                "Execução Fiscal",
                RamoDireito.TRIBUTARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.FAZENDA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo(uf, RamoDireito.TRIBUTARIO),
                "catálogo fazendário estadual"
        );
    }

    private UnidadeInstitucional buildDelegacia(String uf) {
        return unidadePadrao(
                "PC-" + uf,
                DestinatarioInstitucionalKind.DELEGACIA_POLICIA,
                "PC" + uf,
                "Polícia Judiciária do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Delegacia Integrada",
                "Comunicações Oficiais",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.ORGAO_REQUISITADO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.DELEGACIA_POLICIA, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                inferTribunalCodigo(uf, RamoDireito.PENAL),
                "catálogo policial judiciário"
        );
    }

    private UnidadeInstitucional buildDelegaciaPoliciaCivil(String uf) {
        return unidadePadrao(
                "PC-CIVIL-" + uf,
                DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL,
                "PC" + uf,
                "Polícia Civil do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Delegacia de Polícia Civil",
                "Comunicações Policiais",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.ORGAO_REQUISITADO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                inferTribunalCodigo(uf, RamoDireito.PENAL),
                "catálogo específico da polícia civil"
        );
    }

    private UnidadeInstitucional buildDelegaciaPoliciaFederal(String uf) {
        return unidadePadrao(
                "PF-" + uf,
                DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL,
                "PF",
                "Polícia Federal — unidade de referência no " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Delegacia de Polícia Federal",
                "Cooperação Investigativa",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.ORGAO_REQUISITADO,
                TipoCaixaInstitucional.CAIXA_ENTIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                "TRF-" + uf,
                "catálogo específico da polícia federal"
        );
    }

    private UnidadeInstitucional buildPoliciaPenal(String uf) {
        return unidadePadrao(
                "PP-" + uf,
                DestinatarioInstitucionalKind.POLICIA_PENAL,
                "PP" + uf,
                "Polícia Penal do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Central de Custódia",
                "Execução Penal",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.POLICIA_PENAL, PapelProcessualInstitucional.UNIDADE_EXECUTORA),
                inferTribunalCodigo(uf, RamoDireito.PENAL),
                "catálogo da polícia penal"
        );
    }

    private UnidadeInstitucional buildUnidadePrisional(String uf) {
        return unidadePadrao(
                "UP-" + uf,
                DestinatarioInstitucionalKind.UNIDADE_PRISIONAL,
                "UP" + uf,
                "Unidade Prisional Integrada do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Direção de Unidade",
                "Custódia",
                RamoDireito.PENAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.UNIDADE_EXECUTORA,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.UNIDADE_PRISIONAL, PapelProcessualInstitucional.UNIDADE_EXECUTORA),
                inferTribunalCodigo(uf, RamoDireito.PENAL),
                "catálogo de unidades prisionais"
        );
    }

    private UnidadeInstitucional buildConselhoTutelar(String uf) {
        return unidadePadrao(
                "CT-" + uf,
                DestinatarioInstitucionalKind.CONSELHO_TUTELAR,
                "CT" + uf,
                "Conselho Tutelar Integrado do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Plantão Institucional",
                "Infância e Juventude",
                RamoDireito.INFANCIA_JUVENTUDE,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.ORGAO_REQUISITADO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.CONSELHO_TUTELAR, PapelProcessualInstitucional.ORGAO_REQUISITADO),
                inferTribunalCodigo(uf, RamoDireito.INFANCIA_JUVENTUDE),
                "catálogo de comunicação a conselhos tutelares"
        );
    }

    private UnidadeInstitucional buildPericia(String uf) {
        return unidadePadrao(
                "PER-" + uf,
                DestinatarioInstitucionalKind.PERICIA_JUDICIAL,
                "PER" + uf,
                "Perícia Judicial Integrada do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Central Pericial",
                "Nomeações e Laudos",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.PERICIA_JUDICIAL, PapelProcessualInstitucional.AUXILIAR_JUSTICA),
                inferTribunalCodigo(uf, null),
                "catálogo pericial judicial"
        );
    }

    private UnidadeInstitucional buildPeritoJudicial(String uf) {
        return unidadePadrao(
                "PERITO-" + uf,
                DestinatarioInstitucionalKind.PERITO_JUDICIAL,
                "PERITO" + uf,
                "Perito Judicial Credenciado do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Perícia Especializada",
                "Nomeações Individualizadas",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.PERITO_JUDICIAL, PapelProcessualInstitucional.AUXILIAR_JUSTICA),
                inferTribunalCodigo(uf, null),
                "catálogo nominal de peritos judiciais"
        );
    }

    private UnidadeInstitucional buildContadoria(String uf) {
        return unidadePadrao(
                "CONTJUD-" + uf,
                DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL,
                "CONTJUD" + uf,
                "Contadoria Judicial do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Contadoria Central",
                "Cálculos",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL, PapelProcessualInstitucional.APOIO_TECNICO),
                inferTribunalCodigo(uf, null),
                "catálogo de contadorias judiciais"
        );
    }

    private UnidadeInstitucional buildEquipePsicossocial(String uf) {
        return unidadePadrao(
                "PSICO-" + uf,
                DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL,
                "PSICO" + uf,
                "Equipe Psicossocial Judicial do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Núcleo Psicossocial",
                "Família e Infância",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL, PapelProcessualInstitucional.APOIO_TECNICO),
                inferTribunalCodigo(uf, RamoDireito.FAMILIA),
                "catálogo psicossocial judicial"
        );
    }

    private UnidadeInstitucional buildAssistenteSocialJudicial(String uf) {
        return unidadePadrao(
                "ASSSOC-" + uf,
                DestinatarioInstitucionalKind.ASSISTENTE_SOCIAL_JUDICIAL,
                "ASSSOC" + uf,
                "Assistência Social Judicial do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Serviço Social Judicial",
                "Apoio Psicossocial e Relatórios",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.ASSISTENTE_SOCIAL_JUDICIAL, PapelProcessualInstitucional.APOIO_TECNICO),
                inferTribunalCodigo(uf, RamoDireito.FAMILIA),
                "catálogo específico do serviço social judicial"
        );
    }

    private UnidadeInstitucional buildCejusc(String uf) {
        return unidadePadrao(
                "CEJUSC-" + uf,
                DestinatarioInstitucionalKind.CEJUSC,
                "CEJUSC" + uf,
                "Centro Judiciário de Solução de Conflitos e Cidadania do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "CEJUSC Central",
                "Conciliação e Mediação",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.CEJUSC, PapelProcessualInstitucional.AUXILIAR_JUSTICA),
                inferTribunalCodigo(uf, null),
                "catálogo cejusc"
        );
    }

    private UnidadeInstitucional buildCartorioExtrajudicial(String uf) {
        return unidadePadrao(
                "CARTEXT-" + uf,
                DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL,
                "CARTEXT" + uf,
                "Cartório Extrajudicial Integrado do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Serventia Integrada",
                "Registro e Notas",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.DESTINATARIO_OFICIO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL, PapelProcessualInstitucional.DESTINATARIO_OFICIO),
                inferTribunalCodigo(uf, null),
                "catálogo cartorário extrajudicial"
        );
    }

    private UnidadeInstitucional buildOrgaoTecnico(String uf) {
        return unidadePadrao(
                "TEC-" + uf,
                DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO,
                "TEC" + uf,
                "Órgão Técnico Conveniado do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Unidade Conveniada",
                "Apoio Técnico",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO, PapelProcessualInstitucional.APOIO_TECNICO),
                inferTribunalCodigo(uf, null),
                "catálogo conveniado"
        );
    }

    private UnidadeInstitucional buildJuizoDeprecado(String uf) {
        return unidadePadrao(
                "JD-" + uf,
                DestinatarioInstitucionalKind.JUIZO_DEPRECADO,
                "JD" + uf,
                "Juízo de Cooperação e Cumprimento do " + ("DF".equals(uf) ? "Distrito Federal" : "Estado do " + nomeEstado(uf)),
                uf,
                null,
                null,
                "Central de Cooperação",
                "Cartas e Ofícios",
                null,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.JUIZO_COOPERANTE,
                TipoCaixaInstitucional.CAIXA_UNIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.JUIZO_DEPRECADO, PapelProcessualInstitucional.JUIZO_COOPERANTE),
                inferTribunalCodigo(uf, null),
                "catálogo de cooperação judiciária"
        );
    }

    private UnidadeInstitucional buildMinisterioPublicoFamiliaFortaleza() {
        return unidadePadrao(
                "MP-CE-FORTALEZA-FAMILIA",
                DestinatarioInstitucionalKind.MINISTERIO_PUBLICO,
                "MPCE",
                "Promotoria de Justiça de Família e Sucessões de Fortaleza",
                "CE",
                "FORTALEZA",
                "Foro de Fortaleza",
                "Promotoria de Família",
                "Família e Sucessões",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO, PapelProcessualInstitucional.FISCAL_ORDEM_JURIDICA),
                inferTribunalCodigo("CE", RamoDireito.FAMILIA),
                "seed reforçada para atos de família com interesse de incapaz"
        );
    }

    private UnidadeInstitucional buildDefensoriaFamiliaFortaleza() {
        return unidadePadrao(
                "DP-CE-FORTALEZA-FAMILIA",
                DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA,
                "DPCE",
                "Núcleo da Defensoria Pública de Família de Fortaleza",
                "CE",
                "FORTALEZA",
                "Foro de Fortaleza",
                "Núcleo de Família",
                "Família e Sucessões",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                inferTribunalCodigo("CE", RamoDireito.FAMILIA),
                "seed reforçada de defensoria para família"
        );
    }

    private UnidadeInstitucional buildEquipePsicossocialFortaleza() {
        return unidadePadrao(
                "PSICO-CE-FORTALEZA-FAMILIA",
                DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL,
                "PSICOCE",
                "Equipe Psicossocial Judicial de Família de Fortaleza",
                "CE",
                "FORTALEZA",
                "Foro de Fortaleza",
                "Núcleo Psicossocial de Família",
                "Família e Infância",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.APOIO_TECNICO,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL, PapelProcessualInstitucional.APOIO_TECNICO),
                inferTribunalCodigo("CE", RamoDireito.FAMILIA),
                "seed reforçada para estudos psicossociais em família"
        );
    }

    private UnidadeInstitucional buildCejuscFortaleza() {
        return unidadePadrao(
                "CEJUSC-CE-FORTALEZA",
                DestinatarioInstitucionalKind.CEJUSC,
                "CEJUSCCE",
                "CEJUSC de Fortaleza",
                "CE",
                "FORTALEZA",
                "Foro de Fortaleza",
                "CEJUSC de Família",
                "Conciliação e Mediação",
                RamoDireito.FAMILIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.AUXILIAR_JUSTICA,
                TipoCaixaInstitucional.CAIXA_NUCLEO,
                defaultChannelsFor(DestinatarioInstitucionalKind.CEJUSC, PapelProcessualInstitucional.AUXILIAR_JUSTICA),
                inferTribunalCodigo("CE", RamoDireito.FAMILIA),
                "seed reforçada de CEJUSC Fortaleza"
        );
    }

    private UnidadeInstitucional buildAguNacional() {
        return unidadePadrao(
                "AGU-NACIONAL",
                DestinatarioInstitucionalKind.AGU,
                "AGU",
                "Advocacia-Geral da União",
                null,
                null,
                null,
                "Caixa Nacional",
                "Contencioso Federal",
                RamoDireito.PREVIDENCIARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE,
                TipoCaixaInstitucional.CAIXA_ENTIDADE,
                defaultChannelsFor(DestinatarioInstitucionalKind.AGU, PapelProcessualInstitucional.REPRESENTANTE_JUDICIAL_PARTE),
                "AGU",
                "seed nacional da advocacia pública federal"
        );
    }

    private UnidadeInstitucional unidadePadrao(String codigo,
                                               DestinatarioInstitucionalKind destinatarioKind,
                                               String sigla,
                                               String nomeOficial,
                                               String uf,
                                               String comarca,
                                               String foro,
                                               String unidade,
                                               String nucleo,
                                               RamoDireito ramoDireito,
                                               GrauJurisdicao grauJurisdicao,
                                               PapelProcessualInstitucional papel,
                                               TipoCaixaInstitucional tipoCaixa,
                                               List<CanalEntregaInstitucional> canais,
                                               String tribunalCodigo,
                                               String observacao) {
        CaixaInstitucional caixa = new CaixaInstitucional(
                codigo + "-BOX",
                unidade == null ? "Caixa institucional" : unidade,
                tipoCaixa,
                codigo,
                destinatarioKind,
                true,
                tipoCaixa != TipoCaixaInstitucional.CAIXA_ENTIDADE
        );
        return new UnidadeInstitucional(
                codigo,
                destinatarioKind,
                sigla,
                nomeOficial,
                uf,
                comarca,
                foro,
                unidade,
                nucleo,
                ramoDireito,
                grauJurisdicao,
                papel,
                caixa,
                canais,
                tribunalCodigo,
                true,
                observacao
        );
    }

    private List<CanalEntregaInstitucional> defaultChannelsFor(DestinatarioInstitucionalKind kind,
                                                               PapelProcessualInstitucional papel) {
        LinkedHashSet<CanalEntregaInstitucional> channels = new LinkedHashSet<>();
        channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.PJB_INBOX, true, papel.exigeCienciaPessoalPreferencial(), 48, 120, null, "caixa institucional PJB"));
        if (kind.admiteCanalNacionalPessoal()) {
            channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.DOMICILIO_JUDICIAL_ELETRONICO, false, true, 48, 120, null, "canal nacional elegível"));
        }
        if (!papel.exigeCienciaPessoalPreferencial()) {
            channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.DJEN, false, false, 24, 96, null, "publicação quando a lei não exigir pessoalidade"));
        }
        if (kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA
                || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL
                || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL
                || kind == DestinatarioInstitucionalKind.POLICIA_PENAL
                || kind == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL
                || kind == DestinatarioInstitucionalKind.CARTORIO_EXTRAJUDICIAL
                || kind == DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO) {
            channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.WEBHOOK_INSTITUCIONAL, false, false, 24, 72, null, "integração externa institucional"));
        }
        channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.EMAIL_AVISO, false, false, 24, 24, null, "aviso acessório"));
        if (kind == DestinatarioInstitucionalKind.UNIDADE_PRISIONAL
                || kind == DestinatarioInstitucionalKind.POLICIA_PENAL
                || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA
                || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL
                || kind == DestinatarioInstitucionalKind.DELEGACIA_POLICIA_FEDERAL) {
            channels.add(new CanalEntregaInstitucional(CanalComunicacaoInstitucional.COMUNICACAO_FISICA_OFICIAL, false, false, 72, 168, null, "fallback físico oficial"));
        }
        return List.copyOf(channels);
    }

    private String inferTribunalCodigo(String uf, RamoDireito ramoDireito) {
        if (uf == null || uf.isBlank()) {
            return null;
        }
        RamoJusticaNacional ramo = toRamoJusticaNacional(ramoDireito);
        Optional<NationalCompetenceMatrix> resolved = NationalCompetenceMatrix.resolver(uf, ramo);
        if (resolved.isPresent()) {
            return resolved.get().codigo();
        }
        return NationalCompetenceMatrix.resolver(uf, RamoJusticaNacional.ESTADUAL)
                .map(NationalCompetenceMatrix::codigo)
                .orElse(null);
    }

    private RamoJusticaNacional toRamoJusticaNacional(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return RamoJusticaNacional.ESTADUAL;
        }
        return switch (ramoDireito) {
            case TRABALHISTA -> RamoJusticaNacional.TRABALHO;
            case ELEITORAL -> RamoJusticaNacional.ELEITORAL;
            case MILITAR -> RamoJusticaNacional.MILITAR_ESTADUAL;
            case PREVIDENCIARIO -> RamoJusticaNacional.FEDERAL;
            default -> RamoJusticaNacional.ESTADUAL;
        };
    }

    private String nomeEstado(String uf) {
        return ESTADOS.getOrDefault(uf, uf);
    }

    private void put(Map<String, UnidadeInstitucional> seeds, UnidadeInstitucional unit) {
        seeds.put(unit.codigo(), unit);
    }
}
