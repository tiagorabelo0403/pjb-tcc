package com.tcc.pjb.backend.service.processual.substituicao.federativa.centrocomando;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaCentroComandoApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCentroComandoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.centrocomando.PjbSubstituicaoFederativaCentroComandoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.common.PjbSubstituicaoFederativaTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaCentroComandoFacadeService {

    private final PjbSubstituicaoFederativaCentroComandoApplicationService applicationService;

    public PjbSubstituicaoFederativaCentroComandoFacadeService(PjbSubstituicaoFederativaCentroComandoApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaCentroComandoResponse avaliar() {
        PjbSubstituicaoFederativaCentroComandoAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaCentroComandoResponse(
                aggregate.scoreNacional(),
                aggregate.prontoRolloutFederativo(),
                aggregate.prontoRollbackGovernado(),
                aggregate.tribunaisMonitorados(),
                aggregate.tribunaisProntosCorteAssistido(),
                aggregate.tribunaisComBloqueio(),
                aggregate.pendenciasCriticas(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaTribunalResponse mapTribunal(PjbSubstituicaoFederativaTribunal tribunal) {
        return new PjbSubstituicaoFederativaTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.fallbackNacional(),
                tribunal.ondaAtual(),
                tribunal.status().name(),
                tribunal.scoreProntidao(),
                tribunal.prontoRollout(),
                tribunal.prontoRollback(),
                tribunal.sistemasProntos(),
                tribunal.sistemasSaudaveis(),
                tribunal.guardrails(),
                tribunal.rollback(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes()
        );
    }
}
