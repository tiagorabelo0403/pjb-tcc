package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.AcaoProcessualServidor;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.entity.servidor.FuncaoServidorJudiciarioEntity;
import com.tcc.pjb.backend.model.repository.FuncaoServidorJudiciarioRepository;
import java.time.LocalDate;
import java.util.List;

final class PjbAuthorizationFuncaoServidorFacade {

    private final PolicyRegistry policyRegistry;
    private final PjbAuthorizationDecisionContextResolver contextResolver;
    private final FuncaoServidorJudiciarioRepository funcaoServidorRepository;
    private final PjbAuthorizationTrailAssembler trailAssembler;

    PjbAuthorizationFuncaoServidorFacade(PolicyRegistry policyRegistry,
                                         PjbAuthorizationDecisionContextResolver contextResolver,
                                         FuncaoServidorJudiciarioRepository funcaoServidorRepository,
                                         PjbAuthorizationTrailAssembler trailAssembler) {
        this.policyRegistry = policyRegistry;
        this.contextResolver = contextResolver;
        this.funcaoServidorRepository = funcaoServidorRepository;
        this.trailAssembler = trailAssembler;
    }

    PjbAuthorizationEvaluation evaluate(Long unidadeId, AcaoProcessualServidor acao, Processo processo) {
        PolicyRegistry.ActivePolicy activePolicy = policyRegistry.active();
        PjbAuthorizationDecisionContext context = contextResolver.resolve();
        Usuario usuario = context.usuario();
        if (unidadeId == null || acao == null || usuario == null || usuario.getId() == null) {
            AuthzDecision denied = AuthzDecision.deny("funcao_servidor_contexto_invalido", activePolicy.policy().version());
            return trailAssembler.assembleFuncaoServidorCapability(processo, unidadeId, acao, false, context, activePolicy, denied);
        }
        List<FuncaoServidorJudiciarioEntity> ativas =
                funcaoServidorRepository.findByUsuarioIdAndUnidadeIdAndAtivo(usuario.getId(), unidadeId, true);
        LocalDate hoje = LocalDate.now();
        boolean autorizado = ativas.stream()
                .filter(f -> f.isVigente(hoje))
                .anyMatch(f -> possuiCapacidade(f.getFuncao(), acao));
        AuthzDecision decision = autorizado
                ? AuthzDecision.allow("funcao_servidor_capacidade_concedida", activePolicy.policy().version())
                : AuthzDecision.deny("funcao_servidor_capacidade_ausente", activePolicy.policy().version());
        return trailAssembler.assembleFuncaoServidorCapability(processo, unidadeId, acao, autorizado, context, activePolicy, decision);
    }

    private boolean possuiCapacidade(FuncaoServidorJudiciario funcao, AcaoProcessualServidor acao) {
        return switch (acao) {
            case PROFERIR -> funcao.podeProferir();
            case CONCLUIR -> funcao.podeConcluir();
            case INTIMAR -> funcao.podeIntimar();
            case DISTRIBUIR -> funcao.podeDistribuir();
            case ARQUIVAR -> funcao.podeArquivar();
        };
    }
}
