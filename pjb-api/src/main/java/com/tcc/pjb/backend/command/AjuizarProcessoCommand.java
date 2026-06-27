package com.tcc.pjb.backend.command;

import com.tcc.pjb.backend.core.compiler.LegalCompilerService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.dto.ProcessoRequest;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.exception.enums.TipoErroValidacao;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.policy.SigiloPolicyFactory;
import com.tcc.pjb.backend.service.procedural.AjuizamentoCanonicalContextService;
import com.tcc.pjb.backend.service.processo.ProcessoMaterialObjetoEnrichmentService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AjuizarProcessoCommand {

    private final ProcessoRepository processoRepository;
    private final LegalCompilerService legalCompiler;
    private final SigiloPolicyFactory sigiloPolicyFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final ProcessoMaterialObjetoEnrichmentService materialObjetoEnrichmentService;
    private final CaseContinuityOrchestratorService caseContinuityOrchestratorService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final AjuizamentoCanonicalContextService ajuizamentoCanonicalContextService;
    private final TetoProcessualService tetoProcessualService;
    private final TerritorialProcessualService territorialProcessualService;
    private final CompletudeDocumentalPolicyService completudeDocumentalPolicyService;

    @Transactional
    @PjbTransactionalBudget(operation = "ajuizamento.command.persist", maxMillis = 3200, critical = true)
    public Processo execute(Processo processo,
                            Usuario advogado,
                            ProcessoRequest request,
                            List<Attachment> anexos,
                            boolean juizo100Digital) {

        Objects.requireNonNull(processo, "O processo é obrigatório");
        Objects.requireNonNull(advogado, "O advogado é obrigatório");

        processo.setUsuario(advogado);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        materialObjetoEnrichmentService.enrich(processo);

        var compiled = legalCompiler.compile(processo);
        materialObjetoEnrichmentService.enrich(processo);
        if (compiled.isBlocking()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ajuizamento bloqueado por inconsistência estrutural do contexto procedimental.")
                    .addMetadado("status", compiled.getStatus())
                    .addMetadado("statusCodes", compiled.getStatusCodes())
                    .addMetadado("metadata", compiled.getMetadata());
        }

        validateCpfIfPresent(processo.getParteAutoraCpf(), "parteAutoraCpf");
        validateCpfIfPresent(processo.getParteReuCpf(), "parteReuCpf");

        ProceduralRoutingReport routing = nationalProceduralRoutingService.analyzeProcess(processo, request, anexos);
        ajuizamentoCanonicalContextService.consolidate(processo, compiled, routing);
        var diagnosticoTerritorial = territorialProcessualService.diagnosticar(processo, routing);
        if (diagnosticoTerritorial.bloqueante()) {
            throw territorialProcessualService.toException(diagnosticoTerritorial);
        }
        var diagnosticoTeto = tetoProcessualService.diagnosticar(processo);
        if (diagnosticoTeto.bloqueante()) {
            throw tetoProcessualService.toException(diagnosticoTeto);
        }
        ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(processo, routing);
        ProceduralConnectorExecutionReport connectorExecution = proceduralConnectorExecutionService.analyzeProcess(processo, routing, submissionBlueprint);
        applyRoutingSnapshot(processo, routing, submissionBlueprint);
        ajuizamentoCanonicalContextService.consolidate(processo, compiled, routing);
        validateNationalRouting(routing);
        validateSubmissionBlueprint(submissionBlueprint);
        validateConnectorExecution(connectorExecution);

        var diagnosticoCompl = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), anexos);
        if (diagnosticoCompl.bloqueante()) {
            throw completudeDocumentalPolicyService.toException(diagnosticoCompl);
        }

        NivelSigilo nivel = sigiloPolicyFactory
                .obterPoliticaDeSigilo(processo)
                .definirNivel(processo);
        processo.setNivelSigilo(nivel);

        Processo salvo = processoRepository.save(processo);
        caseContinuityOrchestratorService.ensureRootCase(salvo.getId(), "AJUIZAMENTO_INICIAL");

        try {
            eventPublisher.publishEvent(ProcessoAjuizadoEvent.from(salvo, anexos, juizo100Digital));
        } catch (Exception e) {
            log.warn("Falha ao publicar evento ProcessoAjuizadoEvent (não bloqueante): {}", e.getMessage());
        }

        return salvo;
    }

    private void applyRoutingSnapshot(Processo processo,
                                      ProceduralRoutingReport routing,
                                      ProceduralSubmissionBlueprintReport submissionBlueprint) {
        if (processo == null) {
            return;
        }
        if (routing != null) {
            processo.setTipoJustica(routing.tipoJusticaSugerida());
            processo.setRito(RitoProcessual.tryParse(routing.ritoSugerido()).orElse(processo.getRito() == null ? RitoProcessual.COMUM_ORDINARIO : processo.getRito()));
            processo.setTribunalCodigoRoteado(firstNonBlank(processo.getTribunalCodigoRoteado(), routing.tribunalCodigo()));
            processo.setUnidadeJudiciariaCodigo(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), routing.varaSugerida()));
            if (routing.forumAllocation() != null) {
                processo.setTribunalCodigoRoteado(firstNonBlank(processo.getTribunalCodigoRoteado(), routing.forumAllocation().tribunalCodigo()));
                processo.setUnidadeJudiciariaCodigo(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), routing.forumAllocation().varaSugerida()));
                processo.setClasseProcessual(firstNonBlank(processo.getClasseProcessual(), routing.forumAllocation().classeTpuCodigo()));
                processo.setCompetenciaTerritorialModo(firstNonBlank(processo.getCompetenciaTerritorialModo(), routing.forumAllocation().competenciaTerritorialModo()));
                processo.setPreventionMode(firstNonBlank(processo.getPreventionMode(), routing.forumAllocation().preventionMode()));
                processo.setLinkageMode(firstNonBlank(processo.getLinkageMode(), routing.forumAllocation().linkageMode()));
                processo.setPreProtocoloStatus(firstNonBlank(processo.getPreProtocoloStatus(), routing.forumAllocation().preProtocoloStatus()));
                processo.setConnectorSystem(firstNonBlank(processo.getConnectorSystem(), routing.forumAllocation().connectorSystem()));
            }
        }
        if (submissionBlueprint != null) {
            processo.setConnectorSystem(firstNonBlank(processo.getConnectorSystem(), submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null));
            processo.setUnidadeJudiciariaCodigo(firstNonBlank(processo.getUnidadeJudiciariaCodigo(), submissionBlueprint.unidadeJudiciariaCodigo()));
            processo.setTribunalCodigoRoteado(firstNonBlank(processo.getTribunalCodigoRoteado(), submissionBlueprint.tribunalCodigo()));
            processo.setClasseProcessual(firstNonBlank(processo.getClasseProcessual(), submissionBlueprint.classeTpuCodigo()));
            processo.setCompetenciaTerritorialModo(firstNonBlank(processo.getCompetenciaTerritorialModo(), submissionBlueprint.territorialMode()));
            processo.setPreventionMode(firstNonBlank(processo.getPreventionMode(), submissionBlueprint.preventionMode()));
            processo.setLinkageMode(firstNonBlank(processo.getLinkageMode(), submissionBlueprint.linkageMode()));
            processo.setPreProtocoloStatus(firstNonBlank(processo.getPreProtocoloStatus(), submissionBlueprint.blueprintStatus()));
        }
    }

    private void validateSubmissionBlueprint(ProceduralSubmissionBlueprintReport submissionBlueprint) {
        if (submissionBlueprint == null) {
            return;
        }
        if (!submissionBlueprint.blockingIssues().isEmpty() && !submissionBlueprint.readyForAssistedSubmission()) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ajuizamento bloqueado por blueprint nacional de protocolo com inconsistências impeditivas.")
                    .addMetadado("blueprintStatus", submissionBlueprint.blueprintStatus())
                    .addMetadado("requestId", submissionBlueprint.requestId())
                    .addMetadado("tribunalCodigo", submissionBlueprint.tribunalCodigo())
                    .addMetadado("classeTpuCodigo", submissionBlueprint.classeTpuCodigo())
                    .addMetadado("unidadeJudiciariaCodigo", submissionBlueprint.unidadeJudiciariaCodigo())
                    .addMetadado("dryRunStatus", submissionBlueprint.dryRunStatus())
                    .addMetadado("blockingIssues", submissionBlueprint.blockingIssues())
                    .addMetadado("reviewChecklist", submissionBlueprint.reviewChecklist());
        }
    }

    private void validateConnectorExecution(ProceduralConnectorExecutionReport connectorExecution) {
        if (connectorExecution == null) {
            return;
        }
        if (!connectorExecution.blockers().isEmpty() && "BLOCKED".equals(connectorExecution.executionMode())) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ajuizamento bloqueado por malha de execução do protocolo judicial com impedimentos estruturais.")
                    .addMetadado("executionMode", connectorExecution.executionMode())
                    .addMetadado("submissionLane", connectorExecution.submissionLane())
                    .addMetadado("tribunalTargetKey", connectorExecution.tribunalTargetKey())
                    .addMetadado("signerMode", connectorExecution.signerMode())
                    .addMetadado("retryPolicy", connectorExecution.retryPolicy())
                    .addMetadado("blockers", connectorExecution.blockers())
                    .addMetadado("executionChecklist", connectorExecution.executionChecklist());
        }
    }

    private void validateNationalRouting(ProceduralRoutingReport routing) {
        if (routing == null) {
            return;
        }
        if (routing.forumAllocation() != null) {
            if (!routing.forumAllocation().preProtocoloApto() || !routing.forumAllocation().incompatibilities().isEmpty()) {
                throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ajuizamento bloqueado por incompatibilidade entre competência territorial, classe, rito, órgão julgador ou conector judicial.")
                        .addMetadado("riskLevel", routing.riskLevel())
                        .addMetadado("tribunalCodigo", routing.forumAllocation().tribunalCodigo())
                        .addMetadado("varaSugerida", routing.forumAllocation().varaSugerida())
                        .addMetadado("classeTpuCodigo", routing.forumAllocation().classeTpuCodigo())
                        .addMetadado("competenciaTerritorialModo", routing.forumAllocation().competenciaTerritorialModo())
                        .addMetadado("preventionMode", routing.forumAllocation().preventionMode())
                        .addMetadado("linkageMode", routing.forumAllocation().linkageMode())
                        .addMetadado("connectorSystem", routing.forumAllocation().connectorSystem())
                        .addMetadado("preProtocoloStatus", routing.forumAllocation().preProtocoloStatus())
                        .addMetadado("incompatibilities", routing.forumAllocation().incompatibilities())
                        .addMetadado("reviewChecklist", routing.forumAllocation().reviewChecklist());
            }
        }
        if (!routing.blockingIssues().isEmpty() && routing.confidence() < 0.70d) {
            throw new ErroDeValidacaoException(TipoErroValidacao.REGRA_NEGOCIO, "Ajuizamento bloqueado por malha procedural com pendências estruturais relevantes.")
                    .addMetadado("riskLevel", routing.riskLevel())
                    .addMetadado("confidence", routing.confidence())
                    .addMetadado("blockingIssues", routing.blockingIssues())
                    .addMetadado("reviewChecklist", routing.reviewChecklist());
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void validateCpfIfPresent(String cpf, String fieldName) {
        if (cpf == null) {
            return;
        }
        String digits = cpf.replaceAll("\\D+", "");
        if (digits.isBlank()) {
            return;
        }
        if (digits.length() != 11 || !isValidCpfDigits(digits)) {
            throw new ErroDeValidacaoException(TipoErroValidacao.CPF_INVALIDO, fieldName)
                    .addMetadado("valor_informado", cpf)
                    .addMetadado("regra", "cpf deve ter 11 dígitos e dígitos verificadores válidos");
        }
    }

    private static boolean isValidCpfDigits(String digits) {
        boolean allSame = true;
        for (int i = 1; i < 11; i++) {
            if (digits.charAt(i) != digits.charAt(0)) {
                allSame = false;
                break;
            }
        }
        if (allSame) {
            return false;
        }
        int d1 = calcCpfDigit(digits.substring(0, 9), 10);
        int d2 = calcCpfDigit(digits.substring(0, 9) + d1, 11);
        return digits.equals(digits.substring(0, 9) + d1 + d2);
    }

    private static int calcCpfDigit(String base, int weightStart) {
        int sum = 0;
        int weight = weightStart;
        for (int i = 0; i < base.length(); i++) {
            int num = base.charAt(i) - '0';
            sum += num * weight;
            weight--;
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
