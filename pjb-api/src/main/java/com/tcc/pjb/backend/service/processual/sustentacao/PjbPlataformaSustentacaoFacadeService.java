package com.tcc.pjb.backend.service.processual.sustentacao;

import com.tcc.pjb.backend.core.plataforma.sustentacao.application.PjbPlataformaSustentacaoApplicationService;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoAggregate;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoCenario;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoModulo;
import com.tcc.pjb.backend.model.dto.processual.sustentacao.PjbPlataformaSustentacaoCenarioResponse;
import com.tcc.pjb.backend.model.dto.processual.sustentacao.PjbPlataformaSustentacaoEixoResponse;
import com.tcc.pjb.backend.model.dto.processual.sustentacao.PjbPlataformaSustentacaoModuloResponse;
import com.tcc.pjb.backend.model.dto.processual.sustentacao.PjbPlataformaSustentacaoResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbPlataformaSustentacaoFacadeService {

    private final PjbPlataformaSustentacaoApplicationService applicationService;

    public PjbPlataformaSustentacaoFacadeService(PjbPlataformaSustentacaoApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbPlataformaSustentacaoResponse avaliar() {
        PjbPlataformaSustentacaoAggregate aggregate = applicationService.avaliar();
        return new PjbPlataformaSustentacaoResponse(
                aggregate.scoreGeral(),
                aggregate.aptoPreBuild(),
                aggregate.eixosProntos(),
                aggregate.totalEixos(),
                aggregate.eixos().stream().map(this::mapEixo).toList(),
                aggregate.modulos().stream().map(this::mapModulo).toList(),
                aggregate.cenariosDourados().stream().map(this::mapCenario).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.proximasAcoes(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private PjbPlataformaSustentacaoEixoResponse mapEixo(PjbPlataformaSustentacaoEixo eixo) {
        return new PjbPlataformaSustentacaoEixoResponse(
                eixo.codigo(),
                eixo.titulo(),
                eixo.score(),
                eixo.status(),
                eixo.pronto(),
                eixo.sinais(),
                eixo.bloqueadores(),
                eixo.proximasAcoes(),
                eixo.evidencias()
        );
    }

    private PjbPlataformaSustentacaoModuloResponse mapModulo(PjbPlataformaSustentacaoModulo modulo) {
        return new PjbPlataformaSustentacaoModuloResponse(
                modulo.codigo(),
                modulo.titulo(),
                modulo.camada(),
                modulo.beansConectados(),
                modulo.score(),
                modulo.status(),
                modulo.conexoes(),
                modulo.riscos()
        );
    }

    private PjbPlataformaSustentacaoCenarioResponse mapCenario(PjbPlataformaSustentacaoCenario cenario) {
        return new PjbPlataformaSustentacaoCenarioResponse(
                cenario.codigo(),
                cenario.titulo(),
                cenario.tribunalCodigo(),
                cenario.ramo(),
                cenario.rito(),
                cenario.score(),
                cenario.apto(),
                cenario.alertas(),
                cenario.fundamentos()
        );
    }
}
