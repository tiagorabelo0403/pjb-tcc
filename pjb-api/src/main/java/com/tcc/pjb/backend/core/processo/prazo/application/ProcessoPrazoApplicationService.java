package com.tcc.pjb.backend.core.processo.prazo.application;

import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPrazoApplicationService {

    private final ProcessoRepository processoRepository;
    private final PrazoProcessualNacionalService prazoProcessualNacionalService;

    public ProcessoPrazoApplicationService(ProcessoRepository processoRepository,
                                           PrazoProcessualNacionalService prazoProcessualNacionalService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.prazoProcessualNacionalService = Objects.requireNonNull(prazoProcessualNacionalService);
    }

    public ProcessoPrazoAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        LocalDate dataBase = dataBase(processo);
        RamoDireito ramo = processo.getRamoDireito();
        GrauJurisdicao grau = inferirGrau(processo);
        ProcessoPrazoCienciaProfile ciencia = cienciaProfile(processo);
        List<ProcessoPrazoMarco> marcos = Stream.of(
                        buildMarco(processo, dataBase, NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO, "MANIFESTACAO_BASE", "Manifestação processual base", "TRAMITACAO", true),
                        buildMarco(processo, dataBase, choosePrazoPrincipal(processo), "TRILHA_PRINCIPAL", "Prazo principal do rito", "TRILHA", true),
                        buildMarco(processo, dataBase, choosePrazoRecursal(processo), "TRILHA_RECURSAL", "Janela recursal e embargos", "RECURSAL", processo.getStatusProcesso() != null && processo.getStatusProcesso().isPosDecisao()),
                        buildMarco(processo, dataBase, choosePrazoExecutorio(processo), "TRILHA_EXECUTORIA", "Cumprimento e resposta executória", "EXECUCAO", isExecutorio(processo)),
                        buildMarco(processo, dataBase, NationalPrazoEngine.TipoPrazo.PRAZO_MP_MANIFESTACAO, "MANIFESTACAO_INSTITUCIONAL", "Manifestação institucional obrigatória", "INSTITUCIONAL", ramo != null && ramo.exigeAtuacaoMP())
                )
                .flatMap(java.util.Optional::stream)
                .sorted(Comparator.comparing(ProcessoPrazoMarco::vencimento).thenComparing(ProcessoPrazoMarco::codigo))
                .toList();
        long vencidos = marcos.stream().filter(ProcessoPrazoMarco::vencido).count();
        long criticos = marcos.stream().filter(ProcessoPrazoMarco::venceEmAteTresDias).count();
        long comCiencia = marcos.stream().filter(ProcessoPrazoMarco::exigeCiencia).count();
        return new ProcessoPrazoAggregate(
                identity(processo),
                ciencia,
                marcos,
                marcos.size(),
                vencidos,
                criticos,
                comCiencia,
                janelaPredominante(marcos),
                proximaOnda(marcos),
                alertasEstruturais(processo, marcos, ciencia, grau),
                Instant.now()
        );
    }

    public ProcessoPrazoMarco calcular(Long processoId, NationalPrazoEngine.TipoPrazo tipoPrazo) {
        Processo processo = loadProcesso(processoId);
        return buildMarco(processo, dataBase(processo), tipoPrazo, tipoPrazo.name(), humanize(tipoPrazo.name()), "CUSTOM", true)
                .orElseThrow(() -> new IllegalArgumentException("Prazo não disponível para o contexto atual."));
    }

    private java.util.Optional<ProcessoPrazoMarco> buildMarco(Processo processo,
                                                              LocalDate dataBase,
                                                              NationalPrazoEngine.TipoPrazo tipoPrazo,
                                                              String codigo,
                                                              String titulo,
                                                              String eixo,
                                                              boolean habilitado) {
        if (!habilitado || tipoPrazo == null) {
            return java.util.Optional.empty();
        }
        var resultado = prazoProcessualNacionalService.calcular(new PrazoProcessualNacionalService.CalculoPrazoCommand(
                dataBase,
                tipoPrazo,
                processo.getRamoDireito(),
                inferirGrau(processo),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                null
        ));
        int diasRestantes = (int) ChronoUnit.DAYS.between(LocalDate.now(), resultado.vencimentoForense());
        List<String> alertas = new ArrayList<>(resultado.advertencias());
        if (diasRestantes < 0) {
            alertas.add("Prazo vencido e exigindo reavaliação imediata do fluxo.");
        }
        if (diasRestantes >= 0 && diasRestantes <= 3) {
            alertas.add("Prazo em janela crítica de até 3 dias.");
        }
        return java.util.Optional.of(new ProcessoPrazoMarco(
                codigo,
                titulo,
                tipoPrazo.name(),
                eixo,
                dataBase,
                resultado.vencimentoForense(),
                resultado.diasUteisForenses(),
                resultado.diasCorridos(),
                diasRestantes,
                diasRestantes < 0,
                diasRestantes >= 0 && diasRestantes <= 3,
                exigeCiencia(tipoPrazo, processo),
                efeitoProcessual(tipoPrazo),
                List.copyOf(new LinkedHashSet<>(alertas)),
                List.of(resultado.fundamentoNacional(), resultado.fundamentoForense())
        ));
    }

    private ProcessoPrazoIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) {
            marcadores.add(processo.getRamoDireito().name());
            if (processo.getRamoDireito().exigeAtuacaoMP()) {
                marcadores.add("ATUACAO_MP");
            }
        }
        if (processo.getRito() != null) {
            marcadores.add(processo.getRito().name());
        }
        if (processo.getStatusProcesso() != null) {
            marcadores.add(processo.getStatusProcesso().name());
        }
        if (processo.getFaseAtual() != null) {
            marcadores.add(processo.getFaseAtual().name());
        }
        if (processo.getNivelSigilo() != null) {
            marcadores.add(processo.getNivelSigilo().name());
        }
        return new ProcessoPrazoIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                processo.getUf(),
                processo.getComarca(),
                processo.getVara(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                List.copyOf(marcadores)
        );
    }

    private ProcessoPrazoCienciaProfile cienciaProfile(Processo processo) {
        RamoDireito ramo = processo.getRamoDireito();
        boolean cienciaPessoalObrigatoria = ramo == RamoDireito.PENAL
                || ramo == RamoDireito.MILITAR
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || ramo == RamoDireito.FAMILIA;
        boolean prazoEmDobro = ramo == RamoDireito.FAMILIA
                || ramo == RamoDireito.INFANCIA_JUVENTUDE
                || ramo == RamoDireito.PREVIDENCIARIO;
        String modo = cienciaPessoalObrigatoria ? "PESSOAL_E_INSTITUCIONAL" : "ELETRONICA_INSTITUCIONAL";
        LinkedHashSet<String> guardas = new LinkedHashSet<>();
        guardas.add("CONTAGEM_VINCULADA_AO_CALENDARIO_FORENSE");
        guardas.add("TRAVA_DE_CIENCIA_ANTES_DO_ULTIMO_DIA_UTIL");
        if (cienciaPessoalObrigatoria) {
            guardas.add("VALIDACAO_DE_CIENCIA_PESSOAL");
        }
        if (prazoEmDobro) {
            guardas.add("ANALISE_DE_DUPLA_CONTAGEM_INSTITUCIONAL");
        }
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        fundamentos.add("O prazo processual precisa nascer do calendário forense do tribunal e da comarca.");
        fundamentos.add("A ciência não pode ficar separada do ramo, da fase e do perfil institucional responsável.");
        if (ramo != null && ramo.exigeAtuacaoMP()) {
            fundamentos.add("O ramo exige janela institucional para manifestação obrigatória do Ministério Público.");
        }
        return new ProcessoPrazoCienciaProfile(
                modo,
                cienciaPessoalObrigatoria,
                true,
                prazoEmDobro,
                cienciaPessoalObrigatoria || prazoEmDobro,
                List.copyOf(guardas),
                List.copyOf(fundamentos)
        );
    }

    private List<String> proximaOnda(List<ProcessoPrazoMarco> marcos) {
        return marcos.stream()
                .sorted(Comparator.comparing(ProcessoPrazoMarco::vencido).thenComparing(ProcessoPrazoMarco::vencimento))
                .limit(4)
                .map(marco -> marco.codigo() + ':' + marco.vencimento())
                .toList();
    }

    private List<String> alertasEstruturais(Processo processo,
                                            List<ProcessoPrazoMarco> marcos,
                                            ProcessoPrazoCienciaProfile ciencia,
                                            GrauJurisdicao grau) {
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (processo.getTribunal() == null || processo.getTribunal().isBlank()) {
            alertas.add("Tribunal ausente para contagem calibrada por calendário local.");
        }
        if (processo.getComarca() == null || processo.getComarca().isBlank()) {
            alertas.add("Comarca ausente pode enfraquecer feriados locais e plantões específicos.");
        }
        if (processo.getDataUltimaMovimentacao() == null) {
            alertas.add("Sem data de última movimentação o motor usou data de criação como marco inicial.");
        }
        if (grau == null) {
            alertas.add("Grau jurisdicional inferido em fallback reduz fineza de prazo e competência.");
        }
        if (marcos.stream().noneMatch(ProcessoPrazoMarco::exigeCiencia) && ciencia.cienciaPessoalObrigatoria()) {
            alertas.add("Cenário exige ciência pessoal mas nenhum marco foi marcado com ciência obrigatória.");
        }
        if (marcos.stream().allMatch(ProcessoPrazoMarco::vencido)) {
            alertas.add("Todos os marcos calculados estão vencidos e o processo exige reorquestração imediata.");
        }
        return List.copyOf(alertas);
    }

    private String janelaPredominante(List<ProcessoPrazoMarco> marcos) {
        if (marcos.stream().anyMatch(ProcessoPrazoMarco::vencido)) {
            return "EXPIRADA_OU_IRREGULAR";
        }
        if (marcos.stream().anyMatch(ProcessoPrazoMarco::venceEmAteTresDias)) {
            return "CRITICA_ATE_TRES_DIAS";
        }
        return "CONTROLADA";
    }

    private NationalPrazoEngine.TipoPrazo choosePrazoPrincipal(Processo processo) {
        if (processo.getRito() == null) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
        }
        if (processo.getRito().isPenal()) {
            return NationalPrazoEngine.TipoPrazo.APRESENTACAO_DEFESA_PENAL;
        }
        if (processo.getRito().isTrabalhista()) {
            return NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA;
        }
        if (processo.getRito().isEleitoral()) {
            return NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL;
        }
        if (processo.getRito().isTribFazenda()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
        }
        return NationalPrazoEngine.TipoPrazo.CONTESTACAO;
    }

    private NationalPrazoEngine.TipoPrazo choosePrazoRecursal(Processo processo) {
        if (processo.getStatusProcesso() != null && processo.getStatusProcesso().isRecursalOuEmbargos()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO;
        }
        if (processo.getRito() != null && processo.getRito().isTrabalhista()) {
            return NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA;
        }
        if (processo.getRito() != null && processo.getRito().isEleitoral()) {
            return NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL;
        }
        return NationalPrazoEngine.TipoPrazo.APELACAO;
    }

    private NationalPrazoEngine.TipoPrazo choosePrazoExecutorio(Processo processo) {
        if (processo.getRito() != null && processo.getRito().isTribFazenda()) {
            return NationalPrazoEngine.TipoPrazo.EMBARGOS_EXECUCAO;
        }
        if (processo.getRito() != null && processo.getRito().isPenal()) {
            return NationalPrazoEngine.TipoPrazo.ALEGACOES_FINAIS_PENAL;
        }
        return NationalPrazoEngine.TipoPrazo.CUMPRIMENTO_SENTENCA;
    }

    private boolean isExecutorio(Processo processo) {
        return processo.getFaseAtual() != null && processo.getFaseAtual().isExecutionLike()
                || processo.getStatusProcesso() != null && processo.getStatusProcesso().isExecutorio();
    }

    private boolean exigeCiencia(NationalPrazoEngine.TipoPrazo tipoPrazo, Processo processo) {
        return tipoPrazo == NationalPrazoEngine.TipoPrazo.APELACAO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.RECURSO_TRABALHISTA
                || tipoPrazo == NationalPrazoEngine.TipoPrazo.RECURSO_ELEITORAL
                || processo.getRamoDireito() == RamoDireito.PENAL
                || processo.getRamoDireito() == RamoDireito.FAMILIA;
    }

    private String efeitoProcessual(NationalPrazoEngine.TipoPrazo tipoPrazo) {
        return switch (tipoPrazo) {
            case APELACAO, RECURSO_TRABALHISTA, RECURSO_ELEITORAL, RECURSO_ESPECIAL, RECURSO_EXTRAORDINARIO -> "ABRE_JANELA_RECURSAL";
            case EMBARGOS_DECLARACAO -> "SUSPENDE_FLUXO_DECISORIO_ATE_APRECIACAO";
            case CUMPRIMENTO_SENTENCA, EMBARGOS_EXECUCAO, IMPUGNACAO_CUMPRIMENTO -> "ACIONA_TRILHA_EXECUTORIA";
            case PRAZO_MP_MANIFESTACAO -> "RESERVA_ATUACAO_INSTITUCIONAL";
            default -> "MANTEM_TRILHA_REGULAR";
        };
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private LocalDate dataBase(Processo processo) {
        LocalDateTime origem = processo.getDataUltimaMovimentacao() != null
                ? processo.getDataUltimaMovimentacao()
                : processo.getDataCriacao();
        return origem == null ? LocalDate.now() : origem.toLocalDate();
    }

    private GrauJurisdicao inferirGrau(Processo processo) {
        String tribunal = normalize(processo.getTribunal());
        String unidade = normalize(processo.getVara());
        if (tribunal.startsWith("STF")) {
            return GrauJurisdicao.CONSTITUCIONAL;
        }
        if (tribunal.startsWith("STJ") || tribunal.startsWith("TST") || tribunal.startsWith("TSE") || tribunal.startsWith("STM")) {
            return GrauJurisdicao.SUPERIOR;
        }
        if (unidade.contains("CAMARA") || unidade.contains("TURMA") || unidade.contains("COLEGIADO")) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        if (processo.getFaseAtual() == FaseProcessual.RECURSAL) {
            return GrauJurisdicao.SEGUNDO_GRAU;
        }
        return GrauJurisdicao.PRIMEIRO_GRAU;
    }

    private String humanize(String value) {
        return value.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
