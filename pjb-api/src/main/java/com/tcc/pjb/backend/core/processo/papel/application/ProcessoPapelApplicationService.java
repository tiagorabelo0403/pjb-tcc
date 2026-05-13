package com.tcc.pjb.backend.core.processo.papel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessAuthorityBand;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessQueueSectionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessSeparatorSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelIdentity;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPapelApplicationService {

    private final ProcessoRepository processoRepository;
    private final InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService;
    private final InstitutionalProcessWorkspaceApplicationService workspaceApplicationService;

    public ProcessoPapelApplicationService(ProcessoRepository processoRepository,
                                           InstitutionalAccessProfileCatalogApplicationService accessProfileCatalogApplicationService,
                                           InstitutionalProcessWorkspaceApplicationService workspaceApplicationService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.accessProfileCatalogApplicationService = Objects.requireNonNull(accessProfileCatalogApplicationService);
        this.workspaceApplicationService = Objects.requireNonNull(workspaceApplicationService);
    }

    public ProcessoPapelAggregate detalhar(Long processoId) {
        Processo processo = loadProcesso(processoId);
        List<ProcessoPapelPerfil> perfis = accessProfileCatalogApplicationService.listarPerfis().stream()
                .map(entry -> toPerfil(entry, workspaceApplicationService.detalharPerfil(
                        entry.codigo(),
                        processoId,
                        safeName(processo.getRito()),
                        safeName(processo.getFaseAtual()),
                        safeName(processo.getStatusProcesso()),
                        safeName(processo.getRamoDireito())
                )))
                .sorted(Comparator.comparing((ProcessoPapelPerfil item) -> item.assinar().isEmpty())
                        .thenComparing(item -> item.recorrer().isEmpty())
                        .thenComparing(ProcessoPapelPerfil::codigo))
                .toList();
        LinkedHashSet<String> alertas = new LinkedHashSet<>();
        if (perfis.stream().noneMatch(item -> !item.assinar().isEmpty())) {
            alertas.add("Nenhum perfil ficou com trilha explícita de assinatura para o contexto atual.");
        }
        if (processo.getFaseAtual() != null && processo.getFaseAtual().isRecursal() && perfis.stream().noneMatch(item -> !item.recorrer().isEmpty())) {
            alertas.add("Fase recursal sem perfil institucional ou direto habilitado para trilha recursal exige revisão do catálogo.");
        }
        if (processo.getFaseAtual() != null && processo.getFaseAtual().isExecutionLike() && perfis.stream().noneMatch(item -> !item.certificar().isEmpty() || !item.redistribuir().isEmpty())) {
            alertas.add("Fase executiva sem papel de certificação, custódia ou redistribuição interna ficou subdimensionada.");
        }
        long totalAssinantes = perfis.stream().filter(item -> !item.assinar().isEmpty()).count();
        long totalRecursais = perfis.stream().filter(item -> !item.recorrer().isEmpty() || !item.embargar().isEmpty()).count();
        long totalCertificadores = perfis.stream().filter(item -> !item.certificar().isEmpty()).count();
        return new ProcessoPapelAggregate(
                identity(processo),
                perfis.size(),
                totalAssinantes,
                totalRecursais,
                totalCertificadores,
                perfis,
                List.copyOf(alertas),
                Instant.now()
        );
    }

    public ProcessoPapelPerfil detalharPerfil(Long processoId, String profileCode) {
        Processo processo = loadProcesso(processoId);
        InstitutionalAccessProfileCatalogEntry entry = accessProfileCatalogApplicationService.listarPerfis().stream()
                .filter(item -> item.codigo().equalsIgnoreCase(profileCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Perfil institucional ou direto não encontrado: " + profileCode));
        InstitutionalProcessWorkspace workspace = workspaceApplicationService.detalharPerfil(
                entry.codigo(),
                processoId,
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                safeName(processo.getRamoDireito())
        );
        return toPerfil(entry, workspace);
    }

    private ProcessoPapelPerfil toPerfil(InstitutionalAccessProfileCatalogEntry entry, InstitutionalProcessWorkspace workspace) {
        LinkedHashSet<String> visualizar = new LinkedHashSet<>();
        LinkedHashSet<String> receber = new LinkedHashSet<>();
        LinkedHashSet<String> preparar = new LinkedHashSet<>();
        LinkedHashSet<String> aprovar = new LinkedHashSet<>();
        LinkedHashSet<String> assinar = new LinkedHashSet<>();
        LinkedHashSet<String> peticionar = new LinkedHashSet<>();
        LinkedHashSet<String> certificar = new LinkedHashSet<>();
        LinkedHashSet<String> redistribuir = new LinkedHashSet<>();
        LinkedHashSet<String> recorrer = new LinkedHashSet<>();
        LinkedHashSet<String> embargar = new LinkedHashSet<>();
        LinkedHashSet<String> sugerir = new LinkedHashSet<>();
        LinkedHashSet<String> separadores = new LinkedHashSet<>();
        LinkedHashSet<String> guardas = new LinkedHashSet<>();
        workspace.tabs().forEach(tab -> visualizar.add("ABA:" + tab));
        workspace.quickFilters().forEach(filter -> visualizar.add("FILTRO:" + filter));
        workspace.sections().stream().map(InstitutionalProcessQueueSectionSpec::title).forEach(section -> visualizar.add("SECAO:" + section));
        workspace.separators().stream().map(InstitutionalProcessSeparatorSpec::code).forEach(separadores::add);
        guardas.addAll(workspace.authorityBands().stream().flatMap(band -> band.requiredGuards().stream()).toList());
        workspace.actions().forEach(action -> classifyAction(action, receber, preparar, aprovar, assinar, peticionar, certificar, redistribuir, recorrer, embargar, sugerir, guardas));
        workspace.recursosHabilitados().forEach(recorrer::add);
        workspace.embargosHabilitados().forEach(embargar::add);
        if (workspace.processProfile().contains("ASSESSOR") || workspace.processProfile().contains("ANALISTA") || workspace.processProfile().contains("TECNICO") || workspace.processProfile().contains("TRIAGEM")) {
            workspace.actions().stream().map(InstitutionalProcessActionSpec::title).forEach(sugerir::add);
        }
        if (workspace.profileCode().contains("MAGISTRADO") && assinar.isEmpty()) {
            assinar.add("ASSINAR_ATO_JURISDICIONAL");
            aprovar.add("APROVAR_MINUTA_DE_GABINETE");
        }
        if (workspace.profileCode().contains("OAB_SECCIONAL")) {
            aprovar.add("HOMOLOGAR_ATO_SECCIONAL");
            redistribuir.add("GERIR_FLUXO_INSTITUCIONAL_OAB");
        }
        return new ProcessoPapelPerfil(
                workspace.profileCode(),
                workspace.displayName(),
                workspace.panel(),
                workspace.processProfile(),
                workspace.trustFloor(),
                workspace.accentColor(),
                List.copyOf(visualizar),
                List.copyOf(receber),
                List.copyOf(preparar),
                List.copyOf(aprovar),
                List.copyOf(assinar),
                List.copyOf(peticionar),
                List.copyOf(certificar),
                List.copyOf(redistribuir),
                List.copyOf(recorrer),
                List.copyOf(embargar),
                List.copyOf(sugerir),
                List.copyOf(separadores),
                List.copyOf(guardas),
                merge(entry.fundamentos(), workspace.fundamentos())
        );
    }

    private void classifyAction(InstitutionalProcessActionSpec action,
                                LinkedHashSet<String> receber,
                                LinkedHashSet<String> preparar,
                                LinkedHashSet<String> aprovar,
                                LinkedHashSet<String> assinar,
                                LinkedHashSet<String> peticionar,
                                LinkedHashSet<String> certificar,
                                LinkedHashSet<String> redistribuir,
                                LinkedHashSet<String> recorrer,
                                LinkedHashSet<String> embargar,
                                LinkedHashSet<String> sugerir,
                                LinkedHashSet<String> guardas) {
        String code = normalize(action.code());
        String title = action.title();
        if (code.contains("RECEBER") || code.contains("CIENCIA") || code.contains("PANEL_RECEB")) {
            receber.add(title);
        }
        if (code.contains("PREPARAR") || code.contains("MINUTA") || code.contains("ANALISE") || code.contains("CLASSIFICAR") || code.contains("SOLICITAR_COMPLEMENTACAO")) {
            preparar.add(title);
        }
        if (code.contains("HOMOLOGAR") || code.contains("APROVAR") || action.requiresTitularApproval()) {
            aprovar.add(title);
        }
        if (code.contains("ASSINAR") || action.requiresCertificate() || code.contains("EMITIR_PARECER") || code.contains("APRESENTAR_DEFESA")) {
            assinar.add(title);
        }
        if (code.contains("PETICIONAR") || code.contains("APRESENTAR") || code.contains("RESPONDER_OFICIO") || code.contains("SUBMETER_LAUDO") || code.contains("NEGOCIAR_ACORDO") || code.contains("EMITIR_PARECER")) {
            peticionar.add(title);
        }
        if (code.contains("CERTIDAO") || code.contains("CONFIRMAR_CUSTODIA") || code.contains("REGISTRAR_APRESENTACAO") || code.contains("REGISTRAR_TERMO")) {
            certificar.add(title);
        }
        if (code.contains("REDISTRIBUIR") || code.contains("ATRIBUIR") || code.contains("ESCALAR") || code.contains("GERIR_LOTACAO")) {
            redistribuir.add(title);
        }
        if (code.contains("RECURSO") || code.contains("RECORRER") || code.contains("CONTRARRAZOES") || code.contains("RESP") || code.contains("RE") && !code.contains("RECEBER")) {
            recorrer.add(title);
        }
        if (code.contains("EMBARG")) {
            embargar.add(title);
        }
        if (code.contains("ANALISE") || code.contains("MINUTA") || code.contains("CLASSIFICAR") || code.contains("SOLICITAR_COMPLEMENTACAO")) {
            sugerir.add(title);
        }
        if (action.modifiesFlow()) {
            guardas.add("MODIFICA_FLUXO:" + action.code());
        }
        if (action.requiresCertificate()) {
            guardas.add("ASSINATURA_FORTE:" + action.code());
        }
    }

    private ProcessoPapelIdentity identity(Processo processo) {
        LinkedHashSet<String> marcadores = new LinkedHashSet<>();
        if (processo.getRamoDireito() != null) marcadores.add(processo.getRamoDireito().name());
        if (processo.getRito() != null) marcadores.add(processo.getRito().name());
        if (processo.getFaseAtual() != null) marcadores.add(processo.getFaseAtual().name());
        if (processo.getStatusProcesso() != null) marcadores.add(processo.getStatusProcesso().name());
        return new ProcessoPapelIdentity(
                processo.getId(),
                processo.getNumeroProcesso(),
                safeName(processo.getRamoDireito()),
                safeName(processo.getRito()),
                safeName(processo.getFaseAtual()),
                safeName(processo.getStatusProcesso()),
                List.copyOf(marcadores)
        );
    }

    private List<String> merge(List<String> left, List<String> right) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        return List.copyOf(merged);
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
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
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C')
                .replaceAll("[^A-Z0-9]+", "_");
    }

    private String safeName(Enum<?> value) {
        return value == null ? "NAO_INFORMADO" : value.name();
    }
}
