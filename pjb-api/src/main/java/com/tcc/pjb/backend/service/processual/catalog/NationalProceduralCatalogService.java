package com.tcc.pjb.backend.service.processual.catalog;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCatalogService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.model.dto.processual.catalog.NationalProceduralCatalogRequest;
import com.tcc.pjb.backend.model.dto.processual.catalog.NationalProceduralCatalogResponse;
import com.tcc.pjb.backend.model.dto.processual.catalog.ProcessualClassCatalogItemResponse;
import com.tcc.pjb.backend.model.dto.processual.catalog.ProcessualLocalizerCatalogItemResponse;
import com.tcc.pjb.backend.model.dto.processual.catalog.ProcessualMovementCatalogItemResponse;
import com.tcc.pjb.backend.model.dto.processual.catalog.ProcessualSubjectCatalogItemResponse;
import com.tcc.pjb.backend.model.dto.ui.UiPersona;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.ui.assunto.AssuntoCatalogRegistry;
import com.tcc.pjb.backend.service.ui.assunto.AssuntoGroup;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class NationalProceduralCatalogService {

    private final ProcessoRepository processoRepository;
    private final AssuntoCatalogRegistry assuntoCatalogRegistry;
    private final AtoProcessualCatalogService atoProcessualCatalogService;

    public NationalProceduralCatalogService(ProcessoRepository processoRepository,
                                            AssuntoCatalogRegistry assuntoCatalogRegistry,
                                            AtoProcessualCatalogService atoProcessualCatalogService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.assuntoCatalogRegistry = Objects.requireNonNull(assuntoCatalogRegistry);
        this.atoProcessualCatalogService = Objects.requireNonNull(atoProcessualCatalogService);
    }

    public NationalProceduralCatalogResponse consultar(NationalProceduralCatalogRequest request) {
        Objects.requireNonNull(request);
        Processo processo = request.processoId() == null ? null : processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        int limite = request.limite() == null ? 12 : Math.min(Math.max(request.limite(), 1), 40);
        String termo = termoAplicado(request.termo(), processo);
        List<String> alertas = new ArrayList<>();
        if (processo != null && processo.getClasseProcessual() == null && processo.getAssunto() == null && (termo == null || termo.isBlank())) {
            alertas.add("Processo sem classe e assunto preenchidos; a busca foi aberta em catálogo nacional amplo.");
        }
        return new NationalProceduralCatalogResponse(
                processo == null ? null : processo.getId(),
                processo == null ? null : processo.getNumeroProcesso(),
                assuntoCatalogRegistry.version(),
                termo,
                classes(termo, processo, limite),
                assuntos(termo, limite),
                movimentos(termo, processo, limite),
                localizadores(termo, processo, limite),
                List.copyOf(alertas)
        );
    }

    private List<ProcessualClassCatalogItemResponse> classes(String termo, Processo processo, int limite) {
        List<TpuClasseCnj> filtradasPorRamo = processo != null && processo.getRamoDireito() != null
                ? TpuClasseCnj.porRamo(processo.getRamoDireito())
                : List.of();
        List<TpuClasseCnj> base = filtradasPorRamo.isEmpty()
                ? TpuClasseCnj.todasEmOrdemCodigo()
                : filtradasPorRamo;
        String token = normalize(termo);
        List<TpuClasseCnj> resolved = base.stream()
                .filter(item -> token == null || matches(token, item.name(), item.descricao(), item.ramoDireito().name(), item.ramoJustica().name()))
                .limit(limite)
                .toList();
        if (resolved.isEmpty() && token != null) {
            resolved = base.stream().limit(limite).toList();
        }
        return resolved.stream()
                .map(item -> new ProcessualClassCatalogItemResponse(
                        item.codigoTpu(),
                        item.name(),
                        item.descricao(),
                        item.ramoDireito().name(),
                        item.ramoJustica().name(),
                        item.faixaProcedimental().name(),
                        item.exigeMP(),
                        item.exigeSigilo()
                ))
                .toList();
    }

    private List<ProcessualSubjectCatalogItemResponse> assuntos(String termo, int limite) {
        String token = normalize(termo);
        return assuntoCatalogRegistry.groups().stream()
                .filter(group -> token == null || matchesGroup(group, token))
                .sorted(Comparator.comparing(AssuntoGroup::id))
                .limit(limite)
                .map(group -> new ProcessualSubjectCatalogItemResponse(
                        group.id(),
                        firstMeaningfulLabel(group),
                        firstMeaningfulDescription(group),
                        group.icon(),
                        group.pattern(),
                        group.matchAny()
                ))
                .toList();
    }

    private List<ProcessualMovementCatalogItemResponse> movimentos(String termo, Processo processo, int limite) {
        String token = normalize(termo);
        List<AtoProcessualDescriptor> resolved = Stream.of(ProcessoLifecycleAction.values())
                .map(atoProcessualCatalogService::descriptorFor)
                .filter(Objects::nonNull)
                .filter(item -> token == null || matches(token, item.codigo(), item.titulo(), item.categoria().name(), item.workItemType().name(), item.filaPadrao(), item.inboxPadrao()))
                .filter(item -> processo == null || processo.getFaseAtual() == null || item.categoria() != null)
                .limit(limite)
                .toList();
        if (resolved.isEmpty() && token != null) {
            resolved = Stream.of(ProcessoLifecycleAction.values())
                    .map(atoProcessualCatalogService::descriptorFor)
                    .filter(Objects::nonNull)
                    .filter(item -> processo == null || processo.getFaseAtual() == null || item.categoria() != null)
                    .limit(limite)
                    .toList();
        }
        return resolved.stream()
                .map(item -> new ProcessualMovementCatalogItemResponse(
                        item.codigo(),
                        item.titulo(),
                        item.categoria().name(),
                        item.workItemType().name(),
                        item.filaPadrao(),
                        item.inboxPadrao(),
                        item.fundamentoPadrao()
                ))
                .toList();
    }

    private List<ProcessualLocalizerCatalogItemResponse> localizadores(String termo, Processo processo, int limite) {
        List<ProcessualLocalizerCatalogItemResponse> base = new ArrayList<>();
        base.add(new ProcessualLocalizerCatalogItemResponse("SECRETARIA_TRIAGEM", "Triagem inicial", UiPersona.SERVIDOR.name(), "Distribuição e saneamento", 1));
        base.add(new ProcessualLocalizerCatalogItemResponse("SECRETARIA_INTIMACOES", "Expedição de comunicações", UiPersona.SERVIDOR.name(), "Citação e intimação pendentes", 2));
        base.add(new ProcessualLocalizerCatalogItemResponse("GABINETE_DECISAO", "Gabinete decisório", UiPersona.MAGISTRATURA.name(), "Concluso para despacho ou decisão", 1));
        base.add(new ProcessualLocalizerCatalogItemResponse("COLEGIADO_RECURSAL", "Colegiado recursal", UiPersona.MAGISTRATURA.name(), "Fase recursal", 1));
        base.add(new ProcessualLocalizerCatalogItemResponse("CUMPRIMENTO_SENTENCA", "Cumprimento e execução", UiPersona.SERVIDOR.name(), "Título em fase executiva", 2));
        base.add(new ProcessualLocalizerCatalogItemResponse("CEJUSC_AUTOCOMPOSICAO", "Autocomposição", UiPersona.AUXILIAR_JUSTICA.name(), "Conciliação e mediação", 3));
        String token = normalize(termo);
        return base.stream()
                .filter(item -> token == null || matches(token, item.codigo(), item.titulo(), item.perfilAlvo(), item.gatilho(), processo == null ? null : processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name()))
                .sorted(Comparator.comparingInt(ProcessualLocalizerCatalogItemResponse::prioridade).thenComparing(ProcessualLocalizerCatalogItemResponse::codigo))
                .limit(limite)
                .toList();
    }

    private String termoAplicado(String termo, Processo processo) {
        String normalized = normalize(termo);
        if (normalized != null) {
            return termo.trim();
        }
        if (processo == null) {
            return null;
        }
        if (processo.getAssunto() != null && !processo.getAssunto().isBlank()) {
            return processo.getAssunto();
        }
        return processo.getClasseProcessual();
    }

    private boolean matchesGroup(AssuntoGroup group, String token) {
        return matches(token, group.id(), firstMeaningfulLabel(group), firstMeaningfulDescription(group))
                || group.matchAny().stream().anyMatch(item -> normalize(item) != null && normalize(item).contains(token));
    }

    private String firstMeaningfulLabel(AssuntoGroup group) {
        return group.labels().values().stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).findFirst().orElse(group.id());
    }

    private String firstMeaningfulDescription(AssuntoGroup group) {
        return group.descriptions().values().stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).findFirst().orElse(group.id());
    }

    private boolean matches(String token, String... values) {
        for (String value : values) {
            String normalized = normalize(value);
            if (normalized != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
