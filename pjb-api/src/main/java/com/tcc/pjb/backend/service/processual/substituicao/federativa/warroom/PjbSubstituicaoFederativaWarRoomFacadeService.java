package com.tcc.pjb.backend.service.processual.substituicao.federativa.warroom;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaWarRoomApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRamo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRito;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomRamoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomRitoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom.PjbSubstituicaoFederativaWarRoomTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaWarRoomFacadeService {

    private final PjbSubstituicaoFederativaWarRoomApplicationService applicationService;

    public PjbSubstituicaoFederativaWarRoomFacadeService(PjbSubstituicaoFederativaWarRoomApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaWarRoomResponse avaliar() {
        PjbSubstituicaoFederativaWarRoomAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaWarRoomResponse(
                aggregate.scoreGeral(),
                aggregate.freezeNacionalAtivo(),
                aggregate.prontoCorteControlado(),
                aggregate.tribunaisComJanelaAberta(),
                aggregate.tribunaisEmFreeze(),
                aggregate.bloqueadoresCriticos(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaWarRoomTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaWarRoomTribunalResponse mapTribunal(PjbSubstituicaoFederativaWarRoomTribunal tribunal) {
        return new PjbSubstituicaoFederativaWarRoomTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.ondaAtual(),
                tribunal.status(),
                tribunal.scoreProntidao(),
                tribunal.janelaAberta(),
                tribunal.freezeAtivo(),
                tribunal.corteLiberado(),
                tribunal.janelaAtual(),
                tribunal.ramos().stream().map(this::mapRamo).toList(),
                tribunal.guardrails(),
                tribunal.rollback(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes()
        );
    }

    private PjbSubstituicaoFederativaWarRoomRamoResponse mapRamo(PjbSubstituicaoFederativaWarRoomRamo ramo) {
        return new PjbSubstituicaoFederativaWarRoomRamoResponse(
                ramo.ramoCodigo(),
                ramo.ramoDescricao(),
                ramo.score(),
                ramo.corteLiberado(),
                ramo.freezeAtivo(),
                ramo.janelaAtual(),
                ramo.ritos().stream().map(this::mapRito).toList(),
                ramo.evidencias(),
                ramo.acoes()
        );
    }

    private PjbSubstituicaoFederativaWarRoomRitoResponse mapRito(PjbSubstituicaoFederativaWarRoomRito rito) {
        return new PjbSubstituicaoFederativaWarRoomRitoResponse(
                rito.ritoCodigo(),
                rito.score(),
                rito.readiness(),
                rito.resilience(),
                rito.observability(),
                rito.janelaAtual(),
                rito.corteLiberado(),
                rito.freezeAtivo(),
                rito.alertas(),
                rito.acoesImediatas(),
                rito.processoReferenciaId(),
                rito.numeroReferencia()
        );
    }
}
