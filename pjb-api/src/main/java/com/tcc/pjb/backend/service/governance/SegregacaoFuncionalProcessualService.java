package com.tcc.pjb.backend.service.governance;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.lgpd.LgpdProcessualSensibilityEngine;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.governance.FunctionalAuthorityEvaluationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.OperacaoProcessualCritica;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class SegregacaoFuncionalProcessualService {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final LgpdProcessualSensibilityEngine sensibilityEngine;
    private final AuditLedgerService auditLedgerService;

    public SegregacaoFuncionalProcessualService(ProcessoRepository processoRepository,
                                                CurrentUserService currentUserService,
                                                PjbAuthorizationService authorizationService,
                                                LgpdProcessualSensibilityEngine sensibilityEngine,
                                                AuditLedgerService auditLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.sensibilityEngine = Objects.requireNonNull(sensibilityEngine);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    public FunctionalAuthorityEvaluationResponse avaliar(Long processoId,
                                                         OperacaoProcessualCritica operacao,
                                                         boolean stepUpAtivo,
                                                         boolean duplaAprovacaoAtiva,
                                                         boolean revisaoIndependenteAtiva,
                                                         boolean justificativaRegistrada,
                                                         String finalidadeDeclarada) {
        Objects.requireNonNull(processoId);
        Objects.requireNonNull(operacao);
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        var sensibilidade = sensibilityEngine.classificar(processoId);
        boolean leituraPermitida = authorizationService.canReadProcesso(processo).allowed();
        LinkedHashSet<String> capacidades = resolveCapacidades(tipo);
        LinkedHashSet<String> exigencias = new LinkedHashSet<>();
        LinkedHashSet<String> restricoes = new LinkedHashSet<>();
        List<String> fundamentos = new ArrayList<>();
        boolean exigeStepUp = operacao.exigeAssinatura() || sensibilidade.sigiloAutomatico();
        boolean exigeDuplaAprovacao = operacao.exigeDuplaValidacao() || operacao == OperacaoProcessualCritica.HOMOLOGAR;
        boolean exigeRevisaoIndependente = operacao == OperacaoProcessualCritica.PUBLICAR || operacao == OperacaoProcessualCritica.HOMOLOGAR;
        if (sensibilidade.sigiloAutomatico()) {
            fundamentos.add("Processo classificado com sensibilidade máxima e sigilo automático.");
        }
        if (finalidadeDeclarada != null && !finalidadeDeclarada.isBlank()) {
            fundamentos.add("Finalidade declarada: " + finalidadeDeclarada.trim());
        }
        boolean permitido = leituraPermitida && isBaseAllowed(tipo, operacao);
        if (!leituraPermitida) {
            restricoes.add("O perfil atual não possui autorização material para leitura do processo.");
        }
        if (exigeStepUp && !stepUpAtivo) {
            permitido = false;
            exigencias.add("Step-up de autenticação obrigatório para a operação solicitada.");
        }
        if (exigeDuplaAprovacao && !duplaAprovacaoAtiva) {
            permitido = false;
            exigencias.add("Dupla aprovação institucional obrigatória para a operação solicitada.");
        }
        if (exigeRevisaoIndependente && !revisaoIndependenteAtiva) {
            permitido = false;
            exigencias.add("Revisão independente obrigatória antes da consolidação do ato.");
        }
        if ((operacao == OperacaoProcessualCritica.CONSULTAR_SENSIVEL || sensibilidade.sigiloAutomatico()) && !justificativaRegistrada) {
            permitido = false;
            exigencias.add("Justificativa formal obrigatória para acesso ou manuseio de dado sensível.");
        }
        if (!allowedBySensitivity(tipo, sensibilidade.nivel().name(), operacao)) {
            permitido = false;
            restricoes.add("A sensibilidade do processo exige autoridade funcional superior para esta operação.");
        }
        fundamentos.add("Perfil executor: " + (tipo == null ? "NAO_IDENTIFICADO" : tipo.name()));
        fundamentos.add("Operação crítica: " + operacao.name());
        fundamentos.add("Leitura processual autorizada: " + (leituraPermitida ? "SIM" : "NAO"));
        String recurso = processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()
                ? processo.getNumeroUnificado()
                : String.valueOf(processo.getId());
        auditLedgerService.appendSafely(
                "FUNCTIONAL_AUTHORITY_EVALUATION",
                "PROCESSO",
                recurso,
                operacao.name() + "|" + permitido + "|" + (tipo == null ? "-" : tipo.name())
        );
        return new FunctionalAuthorityEvaluationResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                operacao,
                tipo == null ? "NAO_IDENTIFICADO" : tipo.name(),
                permitido,
                resolveAutoridadeResponsavel(tipo, operacao),
                exigeStepUp,
                exigeDuplaAprovacao,
                exigeRevisaoIndependente,
                List.copyOf(capacidades),
                List.copyOf(exigencias),
                List.copyOf(restricoes),
                List.copyOf(fundamentos)
        );
    }

    private boolean isBaseAllowed(TipoUsuario tipo, OperacaoProcessualCritica operacao) {
        if (tipo == null) {
            return false;
        }
        return switch (operacao) {
            case MINUTAR -> tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario() || tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria();
            case REVISAR -> tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario();
            case ASSINAR, HOMOLOGAR, PUBLICAR -> tipo.isMagistratura();
            case CONTRASSINAR -> tipo.isMagistratura() || tipo.isServidorJudiciario();
            case CUMPRIR -> tipo.isServidorJudiciario() || tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR;
            case CERTIFICAR -> tipo.isServidorJudiciario() || tipo == TipoUsuario.OFICIAL_JUSTICA || tipo.isPerito();
            case AUDITAR -> tipo.isAdmin() || tipo.isMagistratura() || tipo.isServidorJudiciario();
            case CONSULTAR_SENSIVEL -> tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario() || tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico();
        };
    }

    private boolean allowedBySensitivity(TipoUsuario tipo, String nivel, OperacaoProcessualCritica operacao) {
        if (tipo == null) {
            return false;
        }
        if (!"MAXIMO".equalsIgnoreCase(nivel)) {
            return true;
        }
        if (operacao == OperacaoProcessualCritica.CUMPRIR || operacao == OperacaoProcessualCritica.CERTIFICAR) {
            return tipo.isServidorJudiciario() || tipo == TipoUsuario.OFICIAL_JUSTICA || tipo.isPerito() || tipo.isMagistratura();
        }
        if (operacao == OperacaoProcessualCritica.CONSULTAR_SENSIVEL) {
            return tipo.isMagistratura() || tipo.isAssessor() || tipo.isServidorJudiciario() || tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria();
        }
        return tipo.isMagistratura() || tipo.isAssessor();
    }

    private LinkedHashSet<String> resolveCapacidades(TipoUsuario tipo) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (tipo == null) {
            return out;
        }
        out.add("CONSULTAR");
        if (tipo.isMagistratura()) {
            out.add("MINUTAR");
            out.add("REVISAR");
            out.add("ASSINAR");
            out.add("HOMOLOGAR");
            out.add("PUBLICAR");
            out.add("AUDITAR");
        }
        if (tipo.isAssessor()) {
            out.add("MINUTAR");
            out.add("REVISAR");
            out.add("CONSULTAR_SENSIVEL");
        }
        if (tipo.isServidorJudiciario()) {
            out.add("REVISAR");
            out.add("CONTRASSINAR");
            out.add("CUMPRIR");
            out.add("CERTIFICAR");
            out.add("AUDITAR");
        }
        if (tipo == TipoUsuario.OFICIAL_JUSTICA || tipo == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
            out.add("CUMPRIR");
            out.add("CERTIFICAR");
        }
        if (tipo.isPerito()) {
            out.add("CERTIFICAR");
        }
        if (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria() || tipo.isMinisterioPublico()) {
            out.add("MINUTAR");
            out.add("CONSULTAR_SENSIVEL");
        }
        return out;
    }

    private String resolveAutoridadeResponsavel(TipoUsuario tipo, OperacaoProcessualCritica operacao) {
        if (operacao == OperacaoProcessualCritica.ASSINAR || operacao == OperacaoProcessualCritica.HOMOLOGAR || operacao == OperacaoProcessualCritica.PUBLICAR) {
            return "MAGISTRATURA";
        }
        if (operacao == OperacaoProcessualCritica.CUMPRIR || operacao == OperacaoProcessualCritica.CERTIFICAR) {
            return "SECRETARIA_E_CUMPRIMENTO";
        }
        if (operacao == OperacaoProcessualCritica.AUDITAR) {
            return tipo != null && tipo.isAdmin() ? "ADMINISTRACAO_NACIONAL" : "GOVERNANCA_JUDICIAL";
        }
        return "UNIDADE_PROCESSUAL";
    }
}
