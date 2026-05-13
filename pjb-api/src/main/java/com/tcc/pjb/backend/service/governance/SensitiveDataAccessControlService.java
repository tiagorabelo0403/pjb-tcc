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
import com.tcc.pjb.backend.model.dto.governance.SensitiveDataAccessResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class SensitiveDataAccessControlService {

    private final ProcessoRepository processoRepository;
    private final CurrentUserService currentUserService;
    private final PjbAuthorizationService authorizationService;
    private final LgpdProcessualSensibilityEngine sensibilityEngine;
    private final AuditLedgerService auditLedgerService;

    public SensitiveDataAccessControlService(ProcessoRepository processoRepository,
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

    public SensitiveDataAccessResponse avaliar(Long processoId,
                                               boolean acessoExcepcional,
                                               boolean stepUpAtivo,
                                               boolean justificativaRegistrada,
                                               boolean duplaAprovacaoAtiva,
                                               String finalidadeDeclarada) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        var classificacao = sensibilityEngine.classificar(processoId);
        boolean autorizado = authorizationService.canReadProcesso(processo).allowed();
        boolean exigeStepUp = classificacao.sigiloAutomatico() || (tipo != null && tipo.isPerfilCritico());
        boolean exigeJustificativa = acessoExcepcional || classificacao.sigiloAutomatico();
        boolean exigeDuplaAprovacao = acessoExcepcional && classificacao.sigiloAutomatico();
        LinkedHashSet<String> camposVisiveis = new LinkedHashSet<>(List.of("numeroProcesso", "classeProcessual", "assunto", "faseAtual", "statusProcesso"));
        LinkedHashSet<String> camposMascarados = new LinkedHashSet<>();
        LinkedHashSet<String> restricoes = new LinkedHashSet<>();
        List<String> fundamentos = new ArrayList<>();
        boolean permitido = true;
        if (classificacao.sigiloAutomatico()) {
            camposMascarados.add("parteAutoraCpf");
            camposMascarados.add("parteReuCpf");
            camposMascarados.add("materialProbatorioResumo");
        }
        if ("ALTO".equalsIgnoreCase(classificacao.nivel().name()) || classificacao.sigiloAutomatico()) {
            camposMascarados.add("parteAutoraNome");
            camposMascarados.add("parteReuNome");
            camposMascarados.add("pedidoPrincipal");
        } else {
            camposVisiveis.add("parteAutoraNome");
            camposVisiveis.add("parteReuNome");
        }
        if (!autorizado) {
            permitido = false;
            restricoes.add("O perfil atual não possui autorização material de leitura do processo.");
        }
        if (exigeStepUp && !stepUpAtivo) {
            permitido = false;
            restricoes.add("Step-up obrigatório para acesso a dados processuais sensíveis.");
        }
        if (exigeJustificativa && !justificativaRegistrada) {
            permitido = false;
            restricoes.add("Justificativa formal obrigatória para acesso sensível ou excepcional.");
        }
        if (exigeDuplaAprovacao && !duplaAprovacaoAtiva) {
            permitido = false;
            restricoes.add("Dupla aprovação obrigatória para acesso excepcional em processo de sensibilidade máxima.");
        }
        if (tipo == TipoUsuario.CIDADAO && classificacao.sigiloAutomatico()) {
            permitido = false;
            restricoes.add("Acesso cidadão bloqueado para processo com sigilo automático, salvo canal próprio autorizado.");
        }
        if (tipo != null && (tipo.isAdvocacia() || tipo.isDefensoriaPublica() || tipo.isProcuradoria()) && classificacao.sigiloAutomatico()) {
            camposMascarados.add("materialProbatorioHash");
            camposMascarados.add("janelaAcordoResumo");
        }
        fundamentos.add("Perfil executor: " + (tipo == null ? "NAO_IDENTIFICADO" : tipo.name()));
        fundamentos.add("Nível de sensibilidade: " + classificacao.nivel().name());
        fundamentos.addAll(classificacao.controles());
        if (finalidadeDeclarada != null && !finalidadeDeclarada.isBlank()) {
            fundamentos.add("Finalidade declarada: " + finalidadeDeclarada.trim());
        }
        auditLedgerService.appendSafely(
                "SENSITIVE_ACCESS_EVALUATION",
                "PROCESSO",
                String.valueOf(processoId),
                classificacao.nivel().name() + "|" + permitido + "|" + (tipo == null ? "-" : tipo.name())
        );
        return new SensitiveDataAccessResponse(
                processoId,
                processo.getNumeroProcesso(),
                tipo == null ? "NAO_IDENTIFICADO" : tipo.name(),
                classificacao.nivel().name(),
                permitido,
                exigeStepUp,
                exigeJustificativa,
                exigeDuplaAprovacao,
                List.copyOf(camposVisiveis),
                List.copyOf(camposMascarados),
                List.copyOf(fundamentos),
                List.copyOf(restricoes)
        );
    }
}
