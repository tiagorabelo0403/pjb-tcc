package com.tcc.pjb.backend.service.processual.linkage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageAnalysisRequest;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageAnalysisResponse;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageApplyRequest;
import com.tcc.pjb.backend.model.dto.processual.linkage.ProcessoLinkageCandidateResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class ProcessoLinkageGovernanceService {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityOrchestratorService caseContinuityOrchestratorService;

    public ProcessoLinkageGovernanceService(ProcessoRepository processoRepository,
                                            CurrentUserService currentUserService,
                                            PjbAuthorizationService authorizationService,
                                            AuditLedgerService auditLedgerService,
                                            CaseContinuityOrchestratorService caseContinuityOrchestratorService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.caseContinuityOrchestratorService = Objects.requireNonNull(caseContinuityOrchestratorService);
    }

    public ProcessoLinkageAnalysisResponse analisar(ProcessoLinkageAnalysisRequest request) {
        Objects.requireNonNull(request);
        Processo base = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        authorizationService.requireReadProcesso(base);
        Map<Long, Processo> candidatos = coletarCandidatos(base, request);
        List<ProcessoLinkageCandidateResponse> resposta = candidatos.values().stream()
                .map(candidato -> avaliar(base, candidato))
                .filter(item -> item.vinculoTipo() != VinculoProcessualTipo.NENHUM)
                .sorted(Comparator.comparingInt(ProcessoLinkageCandidateResponse::score).reversed()
                        .thenComparing(ProcessoLinkageCandidateResponse::processoId, Comparator.nullsLast(Long::compareTo)))
                .toList();
        ProcessoLinkageCandidateResponse candidatoPrimario = resposta.isEmpty() ? null : resposta.getFirst();
        VinculoProcessualTipo primario = candidatoPrimario == null ? VinculoProcessualTipo.NENHUM : candidatoPrimario.vinculoTipo();
        List<String> alertas = new ArrayList<>();
        if (resposta.isEmpty()) {
            alertas.add("Nenhum candidato estruturalmente relevante foi encontrado para prevenção, dependência ou conexão.");
        }
        if (resposta.size() > 5) {
            alertas.add("Há múltiplos candidatos relevantes; recomenda-se saneamento por secretaria ou gabinete antes de consolidar o vínculo.");
        }
        String preventionMode = primario == VinculoProcessualTipo.PREVENCAO && candidatoPrimario != null
                ? "PREVENCAO_PROCESSO:" + candidatoPrimario.processoId()
                : base.getPreventionMode();
        String linkageMode = primario != VinculoProcessualTipo.NENHUM && candidatoPrimario != null
                ? primario.name() + ":" + candidatoPrimario.processoId()
                : base.getLinkageMode();
        if (Boolean.TRUE.equals(request.consolidarSinalizacao()) && primario != VinculoProcessualTipo.NENHUM) {
            base.setPreventionMode(preventionMode);
            base.setLinkageMode(linkageMode);
            processoRepository.save(base);
            auditLedgerService.appendSafely("PROCESSO_LINKAGE_ANALISADO", "PROCESSO", String.valueOf(base.getId()), linkageMode);
        }
        return new ProcessoLinkageAnalysisResponse(
                base.getId(),
                base.getNumeroProcesso(),
                resposta.size(),
                primario,
                preventionMode,
                linkageMode,
                List.copyOf(alertas),
                resposta
        );
    }

    @Transactional
    public ProcessoLinkageAnalysisResponse aplicar(ProcessoLinkageApplyRequest request) {
        Objects.requireNonNull(request);
        Processo base = processoRepository.findById(request.processoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoId()));
        Processo relacionado = processoRepository.findById(request.processoRelacionadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", request.processoRelacionadoId()));
        Usuario usuario = currentUserService.getRequired();
        authorizationService.requireRoleAny(
                usuario,
                "ROLE_ADMIN",
                "ROLE_ADMINISTRADOR",
                "ROLE_SERVIDOR",
                "ROLE_SERVIDOR_FORUM",
                "ROLE_JUIZ",
                "ROLE_MAGISTRADO",
                "ROLE_DESEMBARGADOR",
                "ROLE_MINISTRO"
        );
        base.setLinkageMode(request.vinculoTipo().name() + ":" + relacionado.getId());
        if (request.vinculoTipo() == VinculoProcessualTipo.PREVENCAO) {
            base.setPreventionMode("PREVENCAO_PROCESSO:" + relacionado.getId());
        }
        processoRepository.save(base);
        caseContinuityOrchestratorService.unifyLinkedCases(base.getId(), relacionado.getId(), request.vinculoTipo(), request.justificativa());
        auditLedgerService.appendSafely(
                "PROCESSO_LINKAGE_APLICADO",
                "PROCESSO",
                String.valueOf(base.getId()),
                request.vinculoTipo().name() + "|" + relacionado.getId(),
                request.justificativa()
        );
        return analisar(new ProcessoLinkageAnalysisRequest(base.getId(), List.of(relacionado.getId()), false, false, false));
    }

    private Map<Long, Processo> coletarCandidatos(Processo base, ProcessoLinkageAnalysisRequest request) {
        LinkedHashMap<Long, Processo> map = new LinkedHashMap<>();
        if (request.processoRelacionadoIds() != null) {
            request.processoRelacionadoIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(50)
                    .forEach(id -> processoRepository.findById(id)
                            .filter(p -> !Objects.equals(p.getId(), base.getId()))
                            .ifPresent(p -> map.putIfAbsent(p.getId(), p)));
        }
        if (!Boolean.FALSE.equals(request.incluirMesmoDocumento())) {
            coletarPorDocumento(base, map, base.getParteAutoraCpf());
            coletarPorDocumento(base, map, base.getParteReuCpf());
            coletarPorDocumento(base, map, base.getUsuario() == null ? null : base.getUsuario().getCpf());
        }
        if (!Boolean.FALSE.equals(request.incluirMesmaClasseAssunto())) {
            processoRepository.findByComarcaAndUf(base.getComarca(), base.getUf(), PageRequest.of(0, 80))
                    .getContent().stream()
                    .filter(p -> !Objects.equals(p.getId(), base.getId()))
                    .filter(p -> similaridadeClassificatoria(base, p))
                    .forEach(p -> map.putIfAbsent(p.getId(), p));
        }
        return map;
    }

    private void coletarPorDocumento(Processo base, Map<Long, Processo> map, String documento) {
        String normalizado = digits(documento);
        if (normalizado == null) {
            return;
        }
        processoRepository.findAllByPartesCpf(normalizado).stream()
                .filter(p -> !Objects.equals(p.getId(), base.getId()))
                .forEach(p -> map.putIfAbsent(p.getId(), p));
    }

    private ProcessoLinkageCandidateResponse avaliar(Processo base, Processo candidato) {
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>();
        int score = 0;
        if (sameText(base.getTribunalCodigoRoteado(), candidato.getTribunalCodigoRoteado())) {
            score += 18;
            fundamentos.add("Mesmo tribunal roteado.");
        }
        if (sameText(base.getUnidadeJudiciariaCodigo(), candidato.getUnidadeJudiciariaCodigo())) {
            score += 10;
            fundamentos.add("Mesma unidade judiciária.");
        }
        if (sameText(base.getClasseProcessual(), candidato.getClasseProcessual())) {
            score += 16;
            fundamentos.add("Mesma classe processual.");
        }
        if (containsRelated(base.getAssunto(), candidato.getAssunto())) {
            score += 14;
            fundamentos.add("Assunto materialmente convergente.");
        }
        if (containsRelated(base.getObjetoProcessual(), candidato.getObjetoProcessual())) {
            score += 10;
            fundamentos.add("Objeto processual semelhante.");
        }
        if (containsRelated(base.getPedidoPrincipal(), candidato.getPedidoPrincipal())) {
            score += 8;
            fundamentos.add("Pedido principal com aderência temática.");
        }
        int polosCompartilhados = polosCompartilhados(base, candidato, fundamentos);
        score += polosCompartilhados * 12;
        if (base.getRito() == candidato.getRito() && base.getRito() != null) {
            score += 6;
            fundamentos.add("Mesmo rito processual.");
        }
        if (base.getFaseAtual() == candidato.getFaseAtual() && base.getFaseAtual() != null) {
            score += 4;
            fundamentos.add("Mesma fase processual.");
        }
        VinculoProcessualTipo tipo = resolverTipo(base, candidato, score, polosCompartilhados);
        boolean recomendado = score >= 34 && tipo != VinculoProcessualTipo.NENHUM;
        String preventionMode = tipo == VinculoProcessualTipo.PREVENCAO ? "PREVENCAO_PROCESSO:" + candidato.getId() : null;
        String linkageMode = tipo == VinculoProcessualTipo.NENHUM ? null : tipo.name() + ":" + candidato.getId();
        return new ProcessoLinkageCandidateResponse(
                candidato.getId(),
                candidato.getNumeroProcesso(),
                tipo,
                score,
                recomendado,
                preventionMode,
                linkageMode,
                List.copyOf(fundamentos)
        );
    }

    private VinculoProcessualTipo resolverTipo(Processo base, Processo candidato, int score, int polosCompartilhados) {
        boolean mesmoNucleo = sameText(base.getClasseProcessual(), candidato.getClasseProcessual()) && containsRelated(base.getAssunto(), candidato.getAssunto());
        boolean candidatoMaisAntigo = compareIds(candidato.getId(), base.getId()) < 0;
        if (mesmoNucleo && polosCompartilhados >= 2 && score >= 40) {
            return VinculoProcessualTipo.CONTINENCIA;
        }
        if (mesmoNucleo && candidatoMaisAntigo && score >= 44) {
            return VinculoProcessualTipo.PREVENCAO;
        }
        if (mesmoNucleo && polosCompartilhados >= 1 && score >= 34) {
            return VinculoProcessualTipo.DEPENDENCIA;
        }
        if (polosCompartilhados >= 1 && score >= 28) {
            return VinculoProcessualTipo.CONEXAO;
        }
        if (score >= 24) {
            return VinculoProcessualTipo.APENSAMENTO;
        }
        if (score >= 18) {
            return VinculoProcessualTipo.REFERENCIA;
        }
        return VinculoProcessualTipo.NENHUM;
    }

    private int polosCompartilhados(Processo base, Processo candidato, Set<String> fundamentos) {
        int count = 0;
        if (sameText(digits(base.getParteAutoraCpf()), digits(candidato.getParteAutoraCpf())) && digits(base.getParteAutoraCpf()) != null) {
            count++;
            fundamentos.add("Mesmo polo ativo documental.");
        }
        if (sameText(digits(base.getParteReuCpf()), digits(candidato.getParteReuCpf())) && digits(base.getParteReuCpf()) != null) {
            count++;
            fundamentos.add("Mesmo polo passivo documental.");
        }
        if (sameText(digits(base.getParteAutoraCpf()), digits(candidato.getParteReuCpf())) && digits(base.getParteAutoraCpf()) != null) {
            count++;
            fundamentos.add("Documento do polo ativo replica no polo passivo do correlato.");
        }
        if (sameText(digits(base.getParteReuCpf()), digits(candidato.getParteAutoraCpf())) && digits(base.getParteReuCpf()) != null) {
            count++;
            fundamentos.add("Documento do polo passivo replica no polo ativo do correlato.");
        }
        return count;
    }

    private boolean similaridadeClassificatoria(Processo base, Processo candidato) {
        return sameText(base.getClasseProcessual(), candidato.getClasseProcessual())
                || containsRelated(base.getAssunto(), candidato.getAssunto())
                || containsRelated(base.getObjetoProcessual(), candidato.getObjetoProcessual());
    }

    private boolean containsRelated(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        if (x == null || y == null) {
            return false;
        }
        return x.contains(y) || y.contains(x);
    }

    private boolean sameText(String a, String b) {
        String x = normalize(a);
        String y = normalize(b);
        return x != null && x.equals(y);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private String digits(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private int compareIds(Long a, Long b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return a.compareTo(b);
    }
}
