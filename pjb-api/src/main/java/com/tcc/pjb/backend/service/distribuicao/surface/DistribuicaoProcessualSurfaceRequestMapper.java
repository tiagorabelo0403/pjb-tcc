package com.tcc.pjb.backend.service.distribuicao.surface;

import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualDiagnosticoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.DistribuicaoProcessualRequest;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DistribuicaoProcessualSurfaceRequestMapper {

    public DistribuicaoProcessualNacionalEngine.DistribuicaoRequest toDistribuicaoRequest(DistribuicaoProcessualRequest request) {
        Objects.requireNonNull(request, "request");
        return new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                request.numeroProcesso(),
                request.uf(),
                request.comarca(),
                request.rito(),
                request.valorCausa(),
                request.autor(),
                request.reu(),
                request.grau(),
                firstNonBlank(request.cidade(), request.cidadeAutor(), request.comarca()),
                firstNonBlank(request.foro(), request.secaoJudiciaria(), request.comarca()),
                request.secaoJudiciaria(),
                request.subsecaoJudiciaria(),
                request.assunto(),
                request.classeProcessual(),
                request.classeProcessual(),
                request.assunto(),
                request.circunscricao(),
                request.preventionReference(),
                request.processoReferencia(),
                request.classeProcessual(),
                request.assunto(),
                request.dependenciaDeclarada(),
                request.conexaoDeclarada(),
                request.continenciaDeclarada(),
                request.pedidoLiminar() || request.plantaoJudicial(),
                false,
                request.segredoSolicitado(),
                request.hasRelationalSignal(),
                request.redistribuicaoImpedimento(),
                request.plantaoJudicial(),
                request.pedidoLiminar(),
                request.segredoSolicitado()
        );
    }

    public DistribuicaoProcessualNacionalEngine.DiagnosticoRequest toDiagnosticoRequest(DistribuicaoProcessualDiagnosticoRequest request) {
        Objects.requireNonNull(request, "request");
        return new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                request.rito(),
                request.grau(),
                request.comarca(),
                request.uf(),
                request.valorCausa() != null ? request.valorCausa().doubleValue() : 0.0d,
                request.cidade(),
                request.foro(),
                request.secaoJudiciaria(),
                request.subsecaoJudiciaria(),
                request.circunscricao(),
                request.cidadeAutor(),
                request.cidadeReu(),
                request.cidadeFato(),
                request.municipioFato(),
                request.preventionReference(),
                request.processoReferencia(),
                request.classeProcessual(),
                request.assunto(),
                request.dependenciaDeclarada(),
                request.conexaoDeclarada(),
                request.continenciaDeclarada(),
                request.pedidoLiminar() || request.plantaoJudicial(),
                false,
                request.segredoSolicitado(),
                request.hasAdvancedRelationalSignal(),
                request.redistribuicaoImpedimento(),
                request.plantaoJudicial(),
                request.pedidoLiminar(),
                request.segredoSolicitado()
        );
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
