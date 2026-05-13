package com.tcc.pjb.backend.core.lgpd;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class LgpdProcessualSensibilityEngine {
    private static final Set<RitoProcessual> RITOS_SENSIBILIDADE_MAXIMA = EnumSet.of(
            RitoProcessual.PROCEDIMENTO_PENAL_COMUM,
            RitoProcessual.PROCEDIMENTO_PENAL_SUMARIO,
            RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO,
            RitoProcessual.ESPECIAL_HABEAS_CORPUS,
            RitoProcessual.CIVIL_FAMILIA_DIVORCIO,
            RitoProcessual.CIVIL_ADOCAO,
            RitoProcessual.CIVIL_INVESTIGACAO_PATERNIDADE,
            RitoProcessual.CIVIL_TUTELA_CURATELA,
            RitoProcessual.CIVIL_FAMILIA_ALIMENTOS
    );

    private static final Set<RitoProcessual> RITOS_SENSIBILIDADE_ALTA = EnumSet.of(
            RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO,
            RitoProcessual.IMPROBIDADE_ADMINISTRATIVA,
            RitoProcessual.ELEITORAL_AIRC,
            RitoProcessual.TRABALHISTA_ORDINARIO,
            RitoProcessual.TRABALHISTA_SUMARISSIMO,
            RitoProcessual.TRABALHISTA_SUMARIO_ALCADA,
            RitoProcessual.TRABALHISTA_INQUERITO_FALTA_GRAVE,
            RitoProcessual.TRABALHISTA_ACAO_CUMPRIMENTO,
            RitoProcessual.ESPECIAL_MANDADO_SEGURANCA
    );

    private final ProcessoRepository processoRepository;
    private final PerfilDashboardContextFactory contextFactory;
    private final PjbAuthorizationService authorizationService;

    public LgpdProcessualSensibilityEngine(
            ProcessoRepository processoRepository,
            PerfilDashboardContextFactory contextFactory,
            PjbAuthorizationService authorizationService
    ) {
        this.processoRepository = processoRepository;
        this.contextFactory = contextFactory;
        this.authorizationService = authorizationService;
    }

    public SensibilidadeReport classificar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        NivelSensibilidade nivel = resolverNivel(processo);
        List<String> basesLegais = resolverBasesLegais(nivel);
        List<String> controles = resolverControles(nivel);
        long retencaoDias = resolverRetencao(nivel, processo);
        return new SensibilidadeReport(
                processoId,
                processo.getNumeroProcesso(),
                nivel,
                basesLegais,
                controles,
                retencaoDias,
                Instant.now(),
                nivel == NivelSensibilidade.MAXIMO
        );
    }

    public Map<String, Object> politicaRetencao(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        NivelSensibilidade nivel = resolverNivel(processo);
        long retencaoDias = resolverRetencao(nivel, processo);
        LocalDateTime expurgoPrevisto = processo.getDataDistribuicao() == null
                ? LocalDateTime.now().plusDays(retencaoDias)
                : processo.getDataDistribuicao().plusDays(retencaoDias);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("nivelSensibilidade", nivel.name());
        out.put("retencaoDias", retencaoDias);
        out.put("expurgoPrevisto", expurgoPrevisto);
        out.put("baseLegalRetencao", resolverBaseLegalRetencao(processo));
        out.put("permiteExpurgoAntecipado", nivel != NivelSensibilidade.MAXIMO);
        return out;
    }

    public Map<String, Object> auditarAcessosDados(Long processoId) {
        var ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_ADMIN", "ROLE_JUIZ", "ROLE_MAGISTRADO", "ROLE_DESEMBARGADOR");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        NivelSensibilidade nivel = resolverNivel(processo);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("nivelSensibilidade", nivel.name());
        out.put("sigiloPorPadrao", nivel == NivelSensibilidade.MAXIMO);
        out.put("acaoRecomendada", nivel == NivelSensibilidade.MAXIMO ? "Acesso restrito — requer autorização judicial explícita" : "Acesso conforme perfil RBAC");
        out.put("lgpdArtigos", resolverArtigosLgpd(nivel));
        return out;
    }

    public Map<String, Object> gerarRelatorioImpacto(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        NivelSensibilidade nivel = resolverNivel(processo);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("tipoTratamento", "DADOS_PROCESSUAIS_JUDICIAIS");
        out.put("finalidadeTratamento", "Prestação jurisdicional — interesse público");
        out.put("necessidade", "Dados mínimos necessários ao processo judicial");
        out.put("proporcionalidade", nivel != NivelSensibilidade.MAXIMO ? "PROPORCIONAL" : "VERIFICAR");
        out.put("medidasTecnicas", resolverControles(nivel));
        out.put("baseLegalPrincipal", "Art. 7º II LGPD — Cumprimento de Obrigação Legal");
        out.put("titular", "Partes do processo e terceiros relacionados");
        out.put("geradoEm", Instant.now());
        return out;
    }

    private NivelSensibilidade resolverNivel(Processo processo) {
        if (processo.getRito() != null && RITOS_SENSIBILIDADE_MAXIMA.contains(processo.getRito())) {
            return NivelSensibilidade.MAXIMO;
        }
        if (processo.getRito() != null && RITOS_SENSIBILIDADE_ALTA.contains(processo.getRito())) {
            return NivelSensibilidade.ALTO;
        }
        String fase = processo.getFaseAtual() == null ? null : processo.getFaseAtual().name();
        if (fase != null && (fase.contains("SIGILO") || fase.contains("SEGREDO"))) {
            return NivelSensibilidade.MAXIMO;
        }
        return NivelSensibilidade.NORMAL;
    }

    private List<String> resolverBasesLegais(NivelSensibilidade nivel) {
        return switch (nivel) {
            case MAXIMO -> List.of(
                    "Art. 7º II LGPD — Cumprimento de obrigação legal",
                    "Art. 11 II a LGPD — Dado sensível",
                    "Art. 23 LGPD — Tratamento pelo poder público"
            );
            case ALTO -> List.of(
                    "Art. 7º II LGPD — Cumprimento de obrigação legal",
                    "Art. 23 LGPD — Tratamento pelo poder público"
            );
            default -> List.of(
                    "Art. 7º II LGPD — Cumprimento de obrigação legal",
                    "Art. 7º III LGPD — Execução de políticas públicas"
            );
        };
    }

    private List<String> resolverControles(NivelSensibilidade nivel) {
        return switch (nivel) {
            case MAXIMO -> List.of(
                    "Segredo de justiça automático",
                    "Acesso somente por autorização judicial",
                    "Log imutável de acessos",
                    "Minimização máxima de dados exibidos",
                    "Criptografia forte em repouso",
                    "Step-up de autenticação para dados sensíveis"
            );
            case ALTO -> List.of(
                    "Controle RBAC estrito por perfil e comarca",
                    "Log de auditoria de acessos",
                    "Criptografia em repouso",
                    "Minimização de dados nas listagens"
            );
            default -> List.of(
                    "Controle RBAC padrão",
                    "Log de auditoria básico",
                    "Criptografia padrão em repouso"
            );
        };
    }

    private long resolverRetencao(NivelSensibilidade nivel, Processo processo) {
        if (processo.getRito() != null) {
            return switch (processo.getRito()) {
                case PROCEDIMENTO_PENAL_COMUM, PROCEDIMENTO_PENAL_SUMARIO -> 3650L;
                case EXECUCAO_FISCAL -> 2190L;
                case TRABALHISTA_ORDINARIO, TRABALHISTA_SUMARISSIMO, TRABALHISTA_SUMARIO_ALCADA, TRABALHISTA_INQUERITO_FALTA_GRAVE, TRABALHISTA_ACAO_CUMPRIMENTO -> 1825L;
                default -> nivel == NivelSensibilidade.MAXIMO ? 3650L : nivel == NivelSensibilidade.ALTO ? 1825L : 1095L;
            };
        }
        return nivel == NivelSensibilidade.MAXIMO ? 3650L : 1095L;
    }

    private String resolverBaseLegalRetencao(Processo processo) {
        if (processo.getRito() == null) {
            return "Resolução CNJ n. 324/2020 — Preservação";
        }
        return switch (processo.getRito()) {
            case PROCEDIMENTO_PENAL_COMUM -> "Resolução CNJ n. 324/2020 — Guarda permanente";
            case EXECUCAO_FISCAL -> "Lei 6.830/80 c/c Resolução CNJ n. 324/2020";
            default -> "Resolução CNJ n. 324/2020 — Tabela de temporalidade";
        };
    }

    private List<String> resolverArtigosLgpd(NivelSensibilidade nivel) {
        return switch (nivel) {
            case MAXIMO -> List.of("Art. 5º II", "Art. 7º II", "Art. 11 II a", "Art. 18 VIII", "Art. 23", "Art. 55-J");
            case ALTO -> List.of("Art. 5º I", "Art. 7º II", "Art. 18", "Art. 23");
            default -> List.of("Art. 7º II", "Art. 7º III", "Art. 23");
        };
    }

    public enum NivelSensibilidade {
        NORMAL,
        ALTO,
        MAXIMO
    }

    public record SensibilidadeReport(
            Long processoId,
            String numeroProcesso,
            NivelSensibilidade nivel,
            List<String> basesLegais,
            List<String> controles,
            long retencaoDias,
            Instant classificadoEm,
            boolean sigiloAutomatico
    ) {}
}
