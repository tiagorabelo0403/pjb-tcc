package com.tcc.pjb.backend.service.cidadao;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.dashboard.PainelServiceCommons;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContext;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class CidadaoDashboardEnhancedService {
    private final PerfilDashboardContextFactory contextFactory;
    private final PainelServiceCommons commons;
    private final ProcessoRepository processoRepository;
    private final PjbAuthorizationService authorizationService;

    public CidadaoDashboardEnhancedService(
            PerfilDashboardContextFactory contextFactory,
            PainelServiceCommons commons,
            ProcessoRepository processoRepository,
            com.tcc.pjb.backend.model.repository.WorkItemRepository workItemRepository,
            PjbAuthorizationService authorizationService
    ) {
        this.contextFactory = contextFactory;
        this.commons = commons;
        this.processoRepository = processoRepository;
        this.authorizationService = authorizationService;
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
                        toTextoSimples(p.getFaseAtual()),
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
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("numero", processo.getNumeroProcesso());
        out.put("tribunal", processo.getTribunal());
        out.put("faseAtual", toTextoSimples(processo.getFaseAtual()));
        out.put("descricaoSimples", "Seu processo está em " + toTextoSimples(processo.getFaseAtual()) + ". " + gerarOrientacao(processo.getFaseAtual()));
        out.put("dataInicio", processo.getDataDistribuicao());
        out.put("proximoPasso", resolverProximoPasso(processo.getFaseAtual()));
        return out;
    }

    public Map<String, Object> orientacaoAudiencia(Long processoId) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_CIDADAO");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("processoId", processoId);
        out.put("tribunal", processo.getTribunal());
        out.put("comarca", processo.getComarca());
        out.put("orientacao", buildOrientacaoAudiencia(processo));
        out.put("documentosNecessarios", List.of(
                "Documento de identidade com foto",
                "CPF",
                "Comprovante de endereço",
                "Documentos do processo",
                "Procuração do advogado, se houver"
        ));
        out.put("chegar", "Chegue com pelo menos 30 minutos de antecedência.");
        return out;
    }

    public Map<String, Object> exportarResumoProcesso(Long processoId) {
        PerfilDashboardContext ctx = contextFactory.build();
        authorizationService.requireRole(ctx.usuario(), "ROLE_CIDADAO");
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("numero", processo.getNumeroProcesso());
        out.put("tribunal", processo.getTribunal());
        out.put("comarca", processo.getComarca());
        out.put("faseAtual", toTextoSimples(processo.getFaseAtual()));
        out.put("dataDistribuicao", processo.getDataDistribuicao());
        out.put("rito", toTextoSimples(processo.getRito()));
        out.put("geradoEm", Instant.now());
        out.put("assinaturaDigital", "PJB-HASH-" + Integer.toHexString(processoId.hashCode()));
        return out;
    }

    private String toTextoSimples(Object tecnico) {
        String token = token(tecnico);
        if (token == null) {
            return "Em andamento";
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "Em análise pelo juízo";
            case "CITACAO", "CITACAO_REALIZADA" -> "Citação da outra parte";
            case "CONTESTACAO_APRESENTADA" -> "Resposta da outra parte apresentada";
            case "INSTRUTORIA", "PERICIA_TECNICA" -> "Fase de produção de provas";
            case "SENTENCA_PROFERIDA", "SENTENCA" -> "Sentença proferida";
            case "RECURSAL", "RECURSO_INTERPOSTO" -> "Em recurso";
            case "TRANSITO_EM_JULGADO" -> "Decisão definitiva";
            case "CUMPRIMENTO_SENTENCA" -> "Cumprimento da decisão";
            case "ARQUIVADO", "BAIXADO" -> "Processo encerrado";
            default -> token.replace('_', ' ').toLowerCase();
        };
    }

    private String gerarOrientacao(Object fase) {
        String token = token(fase);
        if (token == null) {
            return "Acompanhe as movimentações com seu advogado.";
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "O processo está em análise inicial pelo juízo.";
            case "INSTRUTORIA", "PERICIA_TECNICA" -> "Pode haver audiência, perícia ou produção de provas.";
            case "RECURSAL" -> "Há análise por instância revisora.";
            case "CUMPRIMENTO_SENTENCA" -> "A decisão está em fase de cumprimento.";
            default -> "Acompanhe as movimentações com seu advogado.";
        };
    }

    private String resolverProximoPasso(Object fase) {
        String token = token(fase);
        if (token == null) {
            return "Aguardar movimentação";
        }
        return switch (token) {
            case "CONHECIMENTO", "COGNITIVA" -> "Aguardar despacho inicial ou citação";
            case "INSTRUTORIA" -> "Possível audiência ou produção de provas";
            case "RECURSAL" -> "Aguardar julgamento do recurso";
            case "CUMPRIMENTO_SENTENCA" -> "Aguardar ato de satisfação da decisão";
            default -> "Acompanhar com seu advogado";
        };
    }

    private String token(Object value) {
        if (value == null) {
            return null;
        }
        return value.toString().toUpperCase().replace(' ', '_');
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
