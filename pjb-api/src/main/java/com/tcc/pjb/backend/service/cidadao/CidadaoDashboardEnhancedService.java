package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.cidadao.CidadaoPosicaoProcessualService.CidadaoPosicaoProcessual;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;

@Service
public class CidadaoDashboardEnhancedService {
    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final CidadaoPosicaoProcessualService posicaoProcessualService;

    public CidadaoDashboardEnhancedService(
            PerfilDashboardContextFactory contextFactory,
            PainelServiceCommons commons,
            ProcessoRepository processoRepository,
            com.tcc.pjb.backend.model.repository.WorkItemRepository workItemRepository,
            PjbAuthorizationService authorizationService,
            RecursalEffectiveSecrecyService secrecyService,
            CidadaoPosicaoProcessualService posicaoProcessualService
    ) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.authorizationService = authorizationService;
        this.secrecyService = secrecyService;
        this.posicaoProcessualService = posicaoProcessualService;
    }

    public CidadaoEnhancedSnapshot bootstrapDashboard() {
        PerfilDashboardContext ctx = contextFactory.build();
        Usuario usuario = ctx.usuario();
        authorizationService.requireRole(usuario, "ROLE_CIDADAO");
        List<Processo> meusProcessos = processoRepository.findByCidadaoCpf(usuario.getCpf(), PageRequest.of(0, 20)).getContent();
        List<ProcessoResumoCidadao> timeline = meusProcessos.stream()
                .limit(10)
                .map(p -> new ProcessoResumoCidadao(
                        p.getId(),
                        p.getNumeroProcesso(),
                        toTextoSimples(p.getFaseAtual(), posicaoProcessualService.polo(p, usuario.getCpf())),
                        toTextoSimples(p.getRito()),
                        p.getDataDistribuicao(),
                        p.getTribunal(),
                        p.getComarca()
                ))
                .toList();
        List<WorkItem> inbox = commons.inboxHibrido(usuario, 20);
        List<AcaoPendenteItem> acoesPendentes = inbox.stream()
                .filter(i -> i.getStatus() == WorkItemStatus.PENDENTE)
                .limit(10)
                .map(i -> new AcaoPendenteItem(
                        i.getId(),
                        toTextoSimples(i.getTitulo()),
                        i.getDueAt(),
                        i.getDueAt() == null ? null : ChronoUnit.DAYS.between(Instant.now(), i.getDueAt())
                ))
                .toList();
        List<String> audienciasProximas = commons.agenda(LocalDate.now(), LocalDate.now().plusDays(60))
                .stream()
                .map(e -> e.title() + " — " + e.start())
                .limit(5)
                .toList();
        int processosAtivos = (int) meusProcessos.stream()
                .filter(p -> p.getStatusProcesso() == null || p.getStatusProcesso().isAtivo())
                .count();
        return new CidadaoEnhancedSnapshot(
                ctx.generatedAt(),
                ctx.perfilAtivo(),
                ctx.tratamento(),
                processosAtivos,
                timeline,
                acoesPendentes,
                audienciasProximas,
                ctx.prazoRadar(),
                ctx.sessionRisk()
        );
    }

    public Map<String, Object> timelineVisualProcesso(Long processoId) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_CIDADAO");
        Processo processo = processoDaParteAtual(processoId);
        CidadaoPosicaoProcessual posicao = posicaoProcessualService.resolver(processo, ctx.usuario().getCpf());
        String fase = toTextoSimples(processo.getFaseAtual(), posicao.polo());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("tribunal", processo.getTribunal());
        out.put("faseAtual", fase);
        out.put("descricaoSimples", "Fase atual do seu processo: " + fase + ". " + gerarOrientacao(processo.getFaseAtual(), posicao.representacao()));
        out.put("dataInicio", processo.getDataDistribuicao());
        out.put("proximoPasso", resolverProximoPasso(processo.getFaseAtual(), posicao.representacao()));
        return out;
    }

    public Map<String, Object> orientacaoAudiencia(Long processoId) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_CIDADAO");
        Processo processo = processoDaParteAtual(processoId);
        CidadaoPosicaoProcessual posicao = posicaoProcessualService.resolver(processo, ctx.usuario().getCpf());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("tribunal", processo.getTribunal());
        out.put("comarca", processo.getComarca());
        out.put("orientacao", buildOrientacaoAudiencia(processo));
        out.put("documentosNecessarios", documentosParaAudiencia(posicao.representacao()));
        out.put("chegar", "Chegue com pelo menos 30 minutos de antecedência.");
        return out;
    }

    public Map<String, Object> exportarResumoProcesso(Long processoId) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_CIDADAO");
        Processo processo = processoDaParteAtual(processoId);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("numero", processo.getNumeroProcesso());
        out.put("tribunal", processo.getTribunal());
        out.put("comarca", processo.getComarca());
        out.put("faseAtual", toTextoSimples(processo.getFaseAtual(),
                posicaoProcessualService.resolver(processo, ctx.usuario().getCpf()).polo()));
        out.put("dataDistribuicao", processo.getDataDistribuicao());
        out.put("rito", toTextoSimples(processo.getRito()));
        out.put("geradoEm", Instant.now());
        out.put("assinaturaDigital", "PJB-HASH-" + Integer.toHexString(processoId.hashCode()));
        return out;
    }

    private Processo processoDaParteAtual(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcessoAsCidadaoParte(processo);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processoId));
        return processo;
    }

    private String toTextoSimples(Object tecnico) {
        return toTextoSimples(tecnico, null);
    }

    private String toTextoSimples(Object tecnico, TipoPolo polo) {
        String token = token(tecnico);
        if (token == null) {
            return "Em andamento";
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "Em análise pelo juízo";
            case "CITACAO", "CITACAO_REALIZADA" -> polo == TipoPolo.ATIVO ? "Citação da outra parte"
                    : polo == TipoPolo.PASSIVO ? "Sua citação" : "Citação";
            case "CONTESTACAO_APRESENTADA" -> "Resposta da outra parte apresentada";
            case "RESPOSTA" -> polo == TipoPolo.ATIVO ? "Prazo de resposta da outra parte"
                    : polo == TipoPolo.PASSIVO ? "Prazo para a sua resposta" : "Prazo de resposta";
            case "INSTRUTORIA", "PERICIA_TECNICA" -> "Fase de produção de provas";
            case "SENTENCA_PROFERIDA", "SENTENCA" -> "Sentença proferida";
            case "RECURSAL", "RECURSO_INTERPOSTO" -> "Em recurso";
            case "TRANSITO_EM_JULGADO" -> "Decisão definitiva";
            case "CUMPRIMENTO_SENTENCA" -> "Cumprimento da decisão";
            case "ARQUIVADO", "BAIXADO", "ARQUIVAMENTO" -> "Processo encerrado";
            case "AUTUACAO" -> "Registro do processo";
            case "DISTRIBUICAO" -> "Distribuição do processo";
            case "SANEAMENTO" -> "Organização do processo pelo juízo";
            case "JULGAMENTO" -> "Em julgamento";
            case "LIQUIDACAO" -> "Cálculo do valor devido";
            case "EXECUCAO", "EXECUTORIA" -> "Fase de execução";
            case "PENHORA" -> "Penhora de bens";
            default -> tecnico instanceof FaseProcessual fase ? fase.getDescricao() : token.replace('_', ' ').toLowerCase();
        };
    }

    private String gerarOrientacao(Object fase, TipoRepresentanteProcessual representacao) {
        String token = token(fase);
        if (token == null) {
            return acompanhamento(representacao);
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "O processo está em análise inicial pelo juízo.";
            case "INSTRUTORIA", "PERICIA_TECNICA" -> "Pode haver audiência, perícia ou produção de provas.";
            case "RECURSAL" -> "Há análise por instância revisora.";
            case "CUMPRIMENTO_SENTENCA" -> "A decisão está em fase de cumprimento.";
            default -> acompanhamento(representacao);
        };
    }

    private static String acompanhamento(TipoRepresentanteProcessual representacao) {
        if (representacao == null) {
            return "Acompanhe as movimentações por aqui.";
        }
        return switch (representacao) {
            case ADVOGADO_PRIVADO -> "Acompanhe as movimentações com seu advogado.";
            case DEFENSOR_PUBLICO -> "Acompanhe as movimentações com a Defensoria Pública.";
            case MINISTERIO_PUBLICO -> "Acompanhe as movimentações com o Ministério Público.";
            case PARTE_SEM_ADVOGADO -> "Acompanhe as movimentações por aqui. Você atua sem advogado; se precisar de orientação, procure a Defensoria Pública ou o setor de atendimento do juizado.";
        };
    }

    private String resolverProximoPasso(Object fase, TipoRepresentanteProcessual representacao) {
        String token = token(fase);
        if (token == null) {
            return "Aguardar movimentação";
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "Aguardar despacho inicial ou citação";
            case "INSTRUTORIA" -> "Possível audiência ou produção de provas";
            case "RECURSAL" -> "Aguardar julgamento do recurso";
            case "CUMPRIMENTO_SENTENCA" -> "Aguardar ato de satisfação da decisão";
            default -> proximoPassoPadrao(representacao);
        };
    }

    private static String proximoPassoPadrao(TipoRepresentanteProcessual representacao) {
        if (representacao == null) {
            return "Acompanhar as movimentações";
        }
        return switch (representacao) {
            case ADVOGADO_PRIVADO -> "Acompanhar com seu advogado";
            case DEFENSOR_PUBLICO -> "Acompanhar com a Defensoria Pública";
            case MINISTERIO_PUBLICO -> "Acompanhar com o Ministério Público";
            case PARTE_SEM_ADVOGADO -> "Acompanhar as movimentações";
        };
    }

    private static List<String> documentosParaAudiencia(TipoRepresentanteProcessual representacao) {
        List<String> documentos = new ArrayList<>(List.of(
                "Documento de identidade com foto",
                "CPF",
                "Comprovante de endereço",
                "Documentos do processo"
        ));
        if (representacao == TipoRepresentanteProcessual.ADVOGADO_PRIVADO) {
            documentos.add("Procuração do advogado");
        } else if (representacao == null) {
            documentos.add("Procuração do advogado, se houver");
        }
        return List.copyOf(documentos);
    }

    private String token(Object value) {
        if (value == null) {
            return null;
        }
        String texto = value instanceof Enum<?> constante ? constante.name() : value.toString();
        return texto.toUpperCase().replace(' ', '_');
    }

    private String buildOrientacaoAudiencia(Processo processo) {
        return "Sua audiência será realizada no fórum da comarca de "
                + (processo.getComarca() == null ? "sua localidade" : processo.getComarca())
                + ". Procure a sala de audiências do "
                + (processo.getTribunal() == null ? "juízo" : processo.getTribunal())
                + ". Apresente-se à recepção com seu documento de identidade.";
    }

    public record CidadaoEnhancedSnapshot(
            LocalDateTime generatedAt,
            String perfilAtivo,
            String tratamento,
            int processosAtivos,
            List<ProcessoResumoCidadao> timeline,
            List<AcaoPendenteItem> acoesPendentes,
            List<String> audienciasProximas,
            List<?> prazoRadar,
            Object sessionRisk
    ) {}

    public record ProcessoResumoCidadao(
            Long id,
            String numero,
            String faseSimples,
            String tipoSimples,
            LocalDateTime dataInicio,
            String tribunal,
            String comarca
    ) {}

    public record AcaoPendenteItem(Long workItemId, String descricaoSimples, Instant dueAt, Long diasRestantes) {}
}
