package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.institutional.InstituicaoJudicial;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ProcessoAjuizamentoIntelligenceService {

    public record AjuizamentoRequest(
            String classeProcessualCnj,
            RitoProcessual ritoDeclarado,
            MateriaJurisdicao materia,
            BigDecimal valorCausa,
            String uf,
            String orgaoDestinoSigla
    ) {}

    public record AjuizamentoReport(
            RitoProcessual ritoDetectado,
            NaturezaProcessualResolver natureza,
            Optional<InstituicaoJudicial> orgaoSugerido,
            List<String> inconsistencias,
            List<String> acoesSugeridas,
            boolean aprovadoParaAutuacao
    ) {}

    private final TribunalConfigurationRegistry registry;
    private final CompetenciaJudicialMatrix matrix;

    public ProcessoAjuizamentoIntelligenceService(
            TribunalConfigurationRegistry registry,
            CompetenciaJudicialMatrix matrix) {
        this.registry = registry;
        this.matrix = matrix;
    }

    public AjuizamentoReport validar(AjuizamentoRequest request) {
        List<String> inconsistencias = new ArrayList<>();
        List<String> acoes = new ArrayList<>();

        RitoProcessual ritoDetectado = detectarRito(request);
        NaturezaProcessualResolver natureza = NaturezaProcessualResolver
                .resolverPorRito(ritoDetectado);

        if (ritoDetectado != request.ritoDeclarado()) {
            inconsistencias.add("Rito declarado '" + request.ritoDeclarado().name()
                    + "' diverge do rito detectado pela classe CNJ '" + ritoDetectado.name() + "'");
            acoes.add("Corrigir rito para " + ritoDetectado.name() + " antes da autuação");
        }

        Optional<InstituicaoJudicial> orgaoDestino = request.orgaoDestinoSigla() != null
                ? registry.buscarPorSigla(request.orgaoDestinoSigla())
                : Optional.empty();

        if (orgaoDestino.isPresent()) {
            InstituicaoJudicial orgao = orgaoDestino.get();
            if (!orgao.aceitaRito(ritoDetectado)) {
                inconsistencias.add("Órgão '" + orgao.sigla()
                        + "' não aceita o rito " + ritoDetectado.name());
                acoes.add("Redistribuir para órgão competente no rito " + ritoDetectado.name());
            }
            if (orgao.ePlantao()) {
                inconsistencias.add("Órgão de destino está em regime de plantão");
                acoes.add("Verificar se o ajuizamento é urgente — plantão aceita apenas atos urgentes");
            }
        } else {
            acoes.add("Selecionar órgão julgador antes de autuar");
        }

        if (ritoDetectado.isJuizado() && request.valorCausa() != null
                && request.valorCausa().compareTo(new BigDecimal("40000")) > 0) {
            inconsistencias.add("Valor da causa supera limite do Juizado Especial (40 salários mínimos)");
            acoes.add("Adequar classe processual para procedimento ordinário ou sumário");
        }

        CompetenciaJudicialMatrix.ResolucaoCompetencia competencia = matrix.resolver(
                ritoDetectado, natureza, request.materia(), request.valorCausa(), request.uf());

        boolean aprovado = inconsistencias.isEmpty() && competencia.orgaoSugerido().isPresent();

        return new AjuizamentoReport(ritoDetectado, natureza,
                competencia.orgaoSugerido(), inconsistencias, acoes, aprovado);
    }

    private RitoProcessual detectarRito(AjuizamentoRequest request) {
        if (request.ritoDeclarado() != null) return request.ritoDeclarado();
        if (request.materia() == MateriaJurisdicao.TRABALHISTA) return RitoProcessual.TRABALHISTA_ORDINARIO;
        if (request.materia() == MateriaJurisdicao.PENAL) return RitoProcessual.PROCEDIMENTO_PENAL_COMUM;
        if (request.valorCausa() != null
                && request.valorCausa().compareTo(new BigDecimal("40000")) <= 0)
            return RitoProcessual.JUIZADO_ESPECIAL_CIVEL;
        return RitoProcessual.COMUM_ORDINARIO;
    }
}
