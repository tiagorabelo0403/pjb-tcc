package com.tcc.pjb.backend.service.secretariat.routing;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePack;
import com.tcc.pjb.backend.service.secretariat.rules.SecretariatRulePackFactory;
import com.tcc.pjb.backend.service.secretariat.orchestration.SecretariatOperationalOrchestrationService;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingProfile;
import com.tcc.pjb.backend.service.secretariat.routing.SecretariatOperationalRoutingResolver;
import com.tcc.pjb.backend.service.secretariat.topology.JudicialTopologySegregationMeshService;

@Service
public class SecretariaEspecializadaRoutingService {

    private final SecretariatRulePackFactory rulePackFactory;
    private final ProcessoRepository processoRepository;
    private final WorkItemRepository workItemRepository;
    private final CurrentUserService currentUserService;
    private final SecretariatOperationalRoutingResolver operationalRoutingResolver;
    private final SecretariatOperationalOrchestrationService operationalOrchestrationService;
    private final JudicialTopologySegregationMeshService judicialTopologySegregationMeshService;
    private final SecretariatInstitutionalVisibilityService visibilityService;
    private final PjbAuthorizationService authorizationService;

    public SecretariaEspecializadaRoutingService(SecretariatRulePackFactory rulePackFactory,
                                                 ProcessoRepository processoRepository,
                                                 WorkItemRepository workItemRepository,
                                                 CurrentUserService currentUserService,
                                                 SecretariatOperationalRoutingResolver operationalRoutingResolver,
                                                 SecretariatOperationalOrchestrationService operationalOrchestrationService,
                                                 JudicialTopologySegregationMeshService judicialTopologySegregationMeshService,
                                                 SecretariatInstitutionalVisibilityService visibilityService,
                                                 PjbAuthorizationService authorizationService) {
        this.rulePackFactory = Objects.requireNonNull(rulePackFactory);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.operationalRoutingResolver = Objects.requireNonNull(operationalRoutingResolver);
        this.operationalOrchestrationService = Objects.requireNonNull(operationalOrchestrationService);
        this.judicialTopologySegregationMeshService = Objects.requireNonNull(judicialTopologySegregationMeshService);
        this.visibilityService = Objects.requireNonNull(visibilityService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
    }

    @Transactional(readOnly = true)
    public SecretariaEspecializadaView consultarPorRamo(String ramoDireito) {
        requireInstitutionalActor();
        RamoDireito ramo = ramoDireito == null ? null : RamoDireito.fromString(ramoDireito.trim().toUpperCase(Locale.ROOT));
        SecretariatRulePack pack = rulePackFactory.resolve(ramo);
        SecretariatOperationalRoutingProfile profile = operationalRoutingResolver.resolveCatalogProfile(ramo);
        visibilityService.requireRoutingAccess(profile);
        return toView(pack, profile, null);
    }

    @Transactional(readOnly = true)
    public SecretariaEspecializadaView diagnosticarProcesso(Long processoId) {
        requireInstitutionalActor();
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        SecretariatRulePack pack = rulePackFactory.resolve(processo.getRamoDireito());
        SecretariatOperationalRoutingProfile profile = operationalRoutingResolver.resolve(processo);
        visibilityService.requireRoutingAccess(profile);
        long pendenciasAbertas = workItemRepository.countOpenByProcesso(processo.getId());
        return toView(pack, profile, pendenciasAbertas);
    }

    @Transactional(readOnly = true)
    public JudicialTopologySegregationMeshService.JudicialTopologySegregationMeshSnapshot malhaProcesso(Long processoId) {
        Usuario actor = requireInstitutionalActor();
        visibilityService.requireProcessAccess(processoId);
        if (actor.getTipoUsuario() != null && actor.getTipoUsuario().isMagistratura()) {
            authorizationService.requireVinculoInstitucionalComProcesso(processoId);
        }
        return judicialTopologySegregationMeshService.snapshot(processoId);
    }

    @Transactional
    public SecretariaDispatchView enfileirarProcesso(Long processoId) {
        visibilityService.requireProcessAccess(processoId);
        SecretariatOperationalOrchestrationService.SecretariatDispatch dispatch = operationalOrchestrationService.receive(processoId, "SECRETARIA_ESPECIALIZADA", Boolean.FALSE);
        return new SecretariaDispatchView(
                dispatch.workItemId(),
                dispatch.processoId(),
                dispatch.numeroProcesso(),
                dispatch.actorId(),
                dispatch.actorNome(),
                dispatch.routing().receiptQueueCode(),
                dispatch.routing().receiptInboxKey(),
                dispatch.routing().secretariatCode(),
                dispatch.routing().organizationalPath(),
                dispatch.routing().specialization() == null ? null : dispatch.routing().specialization().secretariatClass(),
                dispatch.routing().specialization() == null ? null : dispatch.routing().specialization().namespacePjb(),
                dispatch.routing().specialization() == null ? null : dispatch.routing().specialization().painelPjb(),
                dispatch.routing().specialization() == null ? List.of() : dispatch.routing().specialization().connectedCapabilities(),
                dispatch.rulePack().despachoTemplate(),
                dispatch.rulePack().templatesDisponiveis(),
                dispatch.dueAt()
        );
    }

    private Usuario requireInstitutionalActor() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        boolean permitido = tipo != null && (tipo.isServidorJudiciario() || tipo.isMagistratura() || tipo.isAdmin());
        if (!permitido) {
            throw new AccessDeniedPjbException("Apenas secretaria, magistratura ou administracao podem operar filas especializadas");
        }
        return usuario;
    }

    private SecretariaEspecializadaView toView(SecretariatRulePack pack, SecretariatOperationalRoutingProfile profile, Long pendenciasAbertas) {
        SecretariatOperationalRoutingProfile effective = profile != null ? profile : operationalRoutingResolver.resolveCatalogProfile(RamoDireito.fromString(pack.ramoDireito()));
        return new SecretariaEspecializadaView(
                pack.ramoDireito(),
                effective.receiptQueueCode(),
                effective.receiptInboxKey(),
                effective.secretariatCode(),
                effective.tipoJustica(),
                effective.regimeAxis(),
                effective.instanciaAxis(),
                effective.organizationalPath(),
                effective.specialization() == null ? null : effective.specialization().secretariatClass(),
                effective.specialization() == null ? null : effective.specialization().secretariatInstanceClass(),
                effective.specialization() == null ? null : effective.specialization().secretariatBranchClass(),
                effective.specialization() == null ? null : effective.specialization().namespacePjb(),
                effective.specialization() == null ? null : effective.specialization().painelPjb(),
                effective.specialization() == null ? null : effective.specialization().specializedSecretariatName(),
                effective.specialization() == null ? List.of() : effective.specialization().connectedCapabilities(),
                pack.prazoResposta().toHours(),
                effective.receiptSla().toHours(),
                effective.audiencePreparationSla().toHours(),
                pack.despachoTemplate(),
                pack.templatesDisponiveis(),
                pack.processamentoEmHoras(),
                pack.exigeAtuacaoMinisterioPublico(),
                pack.admiteFluxoConciliatorio(),
                pack.geraSigiloAutomatico(),
                effective.secrecyAware(),
                effective.conciliationPreferred(),
                effective.checklist(),
                effective.flags(),
                pendenciasAbertas
        );
    }

    public record SecretariaEspecializadaView(
            String ramoDireito,
            String queueCode,
            String inboxKey,
            String secretariatCode,
            String tipoJustica,
            String regimeAxis,
            String instanciaAxis,
            String organizationalPath,
            String secretariatClass,
            String secretariatInstanceClass,
            String secretariatBranchClass,
            String namespacePjb,
            String painelPjb,
            String specializedSecretariatName,
            List<String> connectedCapabilities,
            long prazoRespostaHoras,
            long prazoRecebimentoHoras,
            long prazoPautaHoras,
            String despachoTemplate,
            List<String> templatesDisponiveis,
            boolean processamentoEmHoras,
            boolean exigeAtuacaoMinisterioPublico,
            boolean admiteFluxoConciliatorio,
            boolean geraSigiloAutomatico,
            boolean sigiloReforcado,
            boolean conciliacaoPreferencial,
            List<String> checklist,
            List<String> flags,
            Long pendenciasAbertas
    ) {
    }

    public record SecretariaDispatchView(
            Long workItemId,
            Long processoId,
            String processoNumero,
            Long actorId,
            String actorNome,
            String queueCode,
            String inboxKey,
            String secretariatCode,
            String organizationalPath,
            String secretariatClass,
            String namespacePjb,
            String painelPjb,
            List<String> connectedCapabilities,
            String despachoTemplate,
            List<String> templatesDisponiveis,
            Instant dueAt
    ) {
    }
}
