package com.tcc.pjb.backend.service.processual.recursal.automation;

import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalAutomationRecommendation;
import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalAutomationScenario;
import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalAutomationSignal;
import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalAutomationSignalStatus;
import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalPretensaoRecursal;
import com.tcc.pjb.backend.core.processo.recursal.domain.automation.RecursalPronunciamentoJudicial;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalFormalSectionLabels;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalJuizoAdmissibilidadeCompetencia;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalMeritoErroTipo;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPoderRecorrerBlueprint;
import com.tcc.pjb.backend.core.processo.recursal.domain.foundation.RecursalPoderRecorrerEvento;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationCandidateView;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationResponse;
import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationSignalView;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RecursalAutomationService {

    public RecursalAutomationResponse advise(RecursalAutomationRequest request) {
        java.util.Objects.requireNonNull(request, "request");
        RecursalAutomationScenario scenario = toScenario(request);
        RecursalPoderRecorrerEvento eventoBloqueio = resolveBlockingEvent(scenario);
        List<RecursalAutomationRecommendation> recommendations = buildRecommendations(scenario, eventoBloqueio);
        List<RecursalAutomationSignal> signals = buildSignals(scenario, recommendations, eventoBloqueio);
        boolean admiteRecursoAdesivo = eventoBloqueio == RecursalPoderRecorrerEvento.NENHUM && allowsAdesive(scenario, recommendations);
        String observacaoRecursoAdesivo = buildAdesiveNote(scenario, admiteRecursoAdesivo, eventoBloqueio);
        return new RecursalAutomationResponse(
                scenario.pronunciamentoJudicial().name(),
                scenario.pretensaoRecursal().name(),
                recommendations.stream().map(this::toView).toList(),
                signals.stream().map(this::toView).toList(),
                admiteRecursoAdesivo,
                observacaoRecursoAdesivo,
                eventoBloqueio != RecursalPoderRecorrerEvento.NENHUM,
                RecursalPoderRecorrerBlueprint.messageFor(eventoBloqueio)
        );
    }

    private List<RecursalAutomationRecommendation> buildRecommendations(RecursalAutomationScenario scenario,
                                                                        RecursalPoderRecorrerEvento eventoBloqueio) {
        if (eventoBloqueio != RecursalPoderRecorrerEvento.NENHUM) {
            return List.of(blockedPowerToAppealRecommendation(eventoBloqueio));
        }
        List<RecursalAutomationRecommendation> recommendations = new ArrayList<>();
        switch (scenario.pronunciamentoJudicial()) {
            case SENTENCA -> recommendations.add(sentencaRecommendation(scenario));
            case DECISAO_INTERLOCUTORIA -> recommendations.add(agravoInstrumentoRecommendation(scenario));
            case DECISAO_MONOCRATICA -> recommendations.add(agravoInternoRecommendation(scenario));
            case ACORDAO -> recommendations.addAll(buildAcordaoRecommendations(scenario));
            case DESPACHO -> recommendations.add(irrecorrivelRecommendation(scenario));
        }
        if (scenario.inadmissaoRecursoExcepcional()) {
            recommendations.add(0, agravoEmRecursoExcepcionalRecommendation(scenario));
            reindexPriorities(recommendations);
        }
        if (scenario.pronunciamentoJudicial() == RecursalPronunciamentoJudicial.ACORDAO
                && scenario.divergenciaJurisprudencialInterna()) {
            recommendations.add(resolveEmbargosDivergenciaInsertionIndex(recommendations), embargosDivergenciaRecommendation(scenario));
            reindexPriorities(recommendations);
        }
        if (scenario.pronunciamentoJudicial() != RecursalPronunciamentoJudicial.DESPACHO
                && (scenario.hasEmbargosGrounds() || scenario.pretensaoRecursal() == RecursalPretensaoRecursal.INTEGRAR_CORRIGIR)) {
            recommendations.add(0, embargosDeclaracaoRecommendation(scenario));
            reindexPriorities(recommendations);
        }
        return new ArrayList<>(recommendations);
    }

    private int resolveEmbargosDivergenciaInsertionIndex(List<RecursalAutomationRecommendation> recommendations) {
        return recommendations.stream()
                .map(RecursalAutomationRecommendation::recurso)
                .anyMatch(recurso -> recurso.equals("AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO")) ? 1 : 0;
    }

    private List<RecursalAutomationRecommendation> buildAcordaoRecommendations(RecursalAutomationScenario scenario) {
        List<RecursalAutomationRecommendation> recommendations = new ArrayList<>();
        recommendations.add(new RecursalAutomationRecommendation(
                "RECURSO_ESPECIAL",
                1,
                "acórdão pode ensejar recurso especial quando houver fundamento infraconstitucional cabível",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.BIPARTIDO_TRIBUNAL_E_CORTE_SUPERIOR.name(),
                meritTypeFor(scenario).name(),
                Set.of(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS),
                false
        ));
        recommendations.add(new RecursalAutomationRecommendation(
                "RECURSO_EXTRAORDINARIO",
                2,
                "acórdão pode ensejar recurso extraordinário quando houver fundamento constitucional cabível",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.BIPARTIDO_TRIBUNAL_E_CORTE_SUPERIOR.name(),
                meritTypeFor(scenario).name(),
                Set.of(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS),
                false
        ));
        return recommendations;
    }

    private RecursalAutomationRecommendation sentencaRecommendation(RecursalAutomationScenario scenario) {
        if (scenario.juizadoEspecial()) {
            return recursoInominadoRecommendation(scenario);
        }
        return apelacaoRecommendation(scenario);
    }

    private RecursalAutomationRecommendation apelacaoRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "APELACAO",
                1,
                "sentença comporta apelação como regra",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS),
                false
        );
    }

    private RecursalAutomationRecommendation recursoInominadoRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "RECURSO_INOMINADO",
                1,
                "sentença do microssistema dos juizados favorece recurso inominado para turma recursal competente",
                10,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                        RecursalFormalSectionLabels.COLEGIADO_RECURSAL_PROPRIO
                ),
                false
        );
    }

    private RecursalAutomationRecommendation agravoInstrumentoRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "AGRAVO_DE_INSTRUMENTO",
                1,
                "decisão interlocutória comporta agravo de instrumento nas hipóteses legais",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.RAZOES_RECURSAIS,
                        RecursalFormalSectionLabels.REGULARIDADE_FORMAL
                ),
                false
        );
    }

    private RecursalAutomationRecommendation agravoInternoRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "AGRAVO_INTERNO",
                1,
                "decisão monocrática de relator comporta agravo interno",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(RecursalFormalSectionLabels.PETICAO_INTERPOSICAO, RecursalFormalSectionLabels.RAZOES_RECURSAIS),
                false
        );
    }

    private RecursalAutomationRecommendation agravoEmRecursoExcepcionalRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "AGRAVO_EM_RECURSO_ESPECIAL_EXTRAORDINARIO",
                1,
                "inadmissão de recurso excepcional desloca a reação para agravo dirigido à instância superior",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.BIPARTIDO_TRIBUNAL_E_CORTE_SUPERIOR.name(),
                meritTypeFor(scenario).name(),
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.IMPUGNACAO_INADMISSIBILIDADE,
                        RecursalFormalSectionLabels.TEMPESTIVIDADE,
                        RecursalFormalSectionLabels.PREPARO
                ),
                false
        );
    }

    private RecursalAutomationRecommendation embargosDivergenciaRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "EMBARGOS_DIVERGENCIA",
                1,
                "divergência interna qualificada entre órgãos fracionários pode ensejar embargos de divergência",
                15,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.DEMONSTRACAO_DIVERGENCIA,
                        RecursalFormalSectionLabels.ACORDAO_PARADIGMA,
                        RecursalFormalSectionLabels.REGULARIDADE_FORMAL
                ),
                false
        );
    }

    private RecursalAutomationRecommendation embargosDeclaracaoRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "EMBARGOS_DECLARACAO",
                1,
                "fundamentos de integração ou correção favorecem embargos de declaração",
                5,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(
                        RecursalFormalSectionLabels.PETICAO_INTERPOSICAO,
                        RecursalFormalSectionLabels.FUNDAMENTOS_EMBARGOS,
                        RecursalFormalSectionLabels.PEDIDO_EFEITO_INFRINGENTE
                ),
                false
        );
    }

    private RecursalAutomationRecommendation irrecorrivelRecommendation(RecursalAutomationScenario scenario) {
        return new RecursalAutomationRecommendation(
                "IRRECORRIVEL",
                1,
                "despacho de mero expediente é irrecorrível",
                5,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                meritTypeFor(scenario).name(),
                Set.of(RecursalFormalSectionLabels.CABIMENTO),
                false
        );
    }

    private RecursalAutomationRecommendation blockedPowerToAppealRecommendation(RecursalPoderRecorrerEvento evento) {
        return new RecursalAutomationRecommendation(
                "PODER_RECORRER_BLOQUEADO",
                1,
                RecursalPoderRecorrerBlueprint.messageFor(evento),
                1,
                RecursalJuizoAdmissibilidadeCompetencia.ORGAO_AD_QUEM.name(),
                RecursalMeritoErroTipo.ERROR_IN_JUDICANDO.name(),
                Set.copyOf(RecursalPoderRecorrerBlueprint.secoesMinimas()),
                false
        );
    }

    private List<RecursalAutomationSignal> buildSignals(
            RecursalAutomationScenario scenario,
            List<RecursalAutomationRecommendation> recommendations,
            RecursalPoderRecorrerEvento eventoBloqueio) {
        List<RecursalAutomationSignal> signals = new ArrayList<>();
        if (eventoBloqueio != RecursalPoderRecorrerEvento.NENHUM) {
            signals.add(new RecursalAutomationSignal(
                    "PODER_RECORRER_BLOQUEADO_" + eventoBloqueio.name(),
                    RecursalAutomationSignalStatus.VEDADO,
                    RecursalPoderRecorrerBlueprint.messageFor(eventoBloqueio)
            ));
            return signals;
        }
        if (scenario.pronunciamentoJudicial() == RecursalPronunciamentoJudicial.DESPACHO) {
            signals.add(new RecursalAutomationSignal(
                    "DESPACHO_IRRECORRIVEL",
                    RecursalAutomationSignalStatus.VEDADO,
                    "despacho de mero expediente não deve gerar recurso autônomo"
            ));
        }
        if (recommendations.stream().anyMatch(candidate -> candidate.recurso().equals("APELACAO"))) {
            signals.add(new RecursalAutomationSignal(
                    "APELACAO_ADMISSIBILIDADE_TRIBUNAL",
                    RecursalAutomationSignalStatus.OK,
                    "na apelação, o juízo a quo garante contraditório e remessa; a admissibilidade recai no tribunal"
            ));
        }
        if (recommendations.stream().anyMatch(candidate -> candidate.recurso().equals("RECURSO_INOMINADO"))) {
            signals.add(new RecursalAutomationSignal(
                    "RECURSO_INOMINADO_TURMA_RECURSAL",
                    RecursalAutomationSignalStatus.OK,
                    "sentença do juizado especial deve seguir para a turma recursal, não para a apelação clássica do tribunal"
            ));
        }
        if (recommendations.stream().anyMatch(candidate -> candidate.recurso().equals("RECURSO_ESPECIAL") || candidate.recurso().equals("RECURSO_EXTRAORDINARIO"))) {
            signals.add(new RecursalAutomationSignal(
                    "FILTRO_PRESIDENCIA_VICE_TRIBUNAL_RECORRIDO",
                    RecursalAutomationSignalStatus.OK,
                    "recursos excepcionais passam por juízo inicial de admissibilidade na presidência ou vice-presidência do tribunal recorrido"
            ));
        }
        if (recommendations.stream().anyMatch(candidate -> candidate.recurso().equals("AGRAVO_INTERNO"))) {
            signals.add(new RecursalAutomationSignal(
                    "AGRAVO_INTERNO_MONOCRATICO",
                    RecursalAutomationSignalStatus.OK,
                    "decisão monocrática do relator favorece agravo interno como reação prioritária"
            ));
        }
        if (scenario.inadmissaoRecursoExcepcional()) {
            signals.add(new RecursalAutomationSignal(
                    "AGRAVO_RECURSO_EXCEPCIONAL_CABIVEL",
                    RecursalAutomationSignalStatus.OK,
                    "inadmissão de recurso excepcional recomenda agravo específico para reabrir o controle da corte superior"
            ));
        }
        if (scenario.divergenciaJurisprudencialInterna()) {
            signals.add(new RecursalAutomationSignal(
                    "DIVERGENCIA_INTERNA_MAPEADA",
                    RecursalAutomationSignalStatus.CONDICIONADO,
                    "divergência interna exige demonstração analítica e acórdão paradigma idôneo"
            ));
        }
        if (scenario.preparoInsuficiente()) {
            signals.add(new RecursalAutomationSignal(
                    "PREPARO_INSUFICIENTE",
                    RecursalAutomationSignalStatus.ATENCAO,
                    "preparo insuficiente exige complementação em 5 dias, não deserção imediata"
            ));
        }
        if (!scenario.preparoEfetuado()
                && recommendations.stream().noneMatch(candidate -> candidate.recurso().equals("IRRECORRIVEL"))) {
            signals.add(new RecursalAutomationSignal(
                    "PREPARO_AUSENTE",
                    RecursalAutomationSignalStatus.CONDICIONADO,
                    "ausência total de preparo tende a gerar deserção; verificar hipóteses de dispensa legal e autos eletrônicos"
            ));
        }
        if (scenario.feriadoLocalAplicavel() && !scenario.feriadoLocalComprovado()) {
            signals.add(new RecursalAutomationSignal(
                    "FERIADO_LOCAL_NAO_COMPROVADO",
                    RecursalAutomationSignalStatus.ATENCAO,
                    "feriado local precisa ser comprovado no ato da interposição"
            ));
        }
        if (scenario.processoFisico()
                && recommendations.stream().anyMatch(candidate -> candidate.recurso().equals("AGRAVO_DE_INSTRUMENTO"))) {
            signals.add(new RecursalAutomationSignal(
                    "AGRAVO_PEÇAS_PROCESSO_FISICO",
                    RecursalAutomationSignalStatus.CONDICIONADO,
                    "agravo de instrumento em processo físico exige atenção reforçada à regularidade formal das peças obrigatórias"
            ));
        }
        if (scenario.hasEmbargosGrounds()) {
            signals.add(new RecursalAutomationSignal(
                    "EMBARGOS_DECLARACAO_IDENTIFICADOS",
                    RecursalAutomationSignalStatus.OK,
                    "foram identificados fundamentos típicos de embargos de declaração no cenário informado"
            ));
        }
        if (scenario.pretendeEfeitoInfringente()) {
            signals.add(new RecursalAutomationSignal(
                    "EFEITO_INFRINGENTE_PRETENDIDO",
                    scenario.hasEmbargosGrounds() ? RecursalAutomationSignalStatus.CONDICIONADO : RecursalAutomationSignalStatus.ATENCAO,
                    scenario.hasEmbargosGrounds()
                            ? "efeito infringente exige fundamento apto e tende a demandar contraditório prévio"
                            : "efeito infringente sem fundamento típico de embargos ou erro relevante exige cautela reforçada"
            ));
        }
        if (scenario.autosEletronicos()) {
            signals.add(new RecursalAutomationSignal(
                    "AUTOS_ELETRONICOS",
                    RecursalAutomationSignalStatus.OK,
                    "em autos eletrônicos o porte de remessa e retorno não se aplica"
            ));
        }
        if (scenario.contrarrazoesJaProtocoladas()) {
            signals.add(new RecursalAutomationSignal(
                    "ADESIVO_PRAZO_CONSUMIDO",
                    RecursalAutomationSignalStatus.VEDADO,
                    "contrarrazões já protocoladas esgotam a janela prática do recurso adesivo no cenário informado"
            ));
        }
        if (scenario.desejaSustentacaoOral()) {
            signals.add(new RecursalAutomationSignal(
                    "SUSTENTACAO_ORAL_MAPEADA",
                    RecursalAutomationSignalStatus.OK,
                    "o cenário pede preparação de sustentação oral dentro da trilha de tribunal"
            ));
        }
        return signals;
    }

    private boolean allowsAdesive(RecursalAutomationScenario scenario, List<RecursalAutomationRecommendation> recommendations) {
        if (!scenario.sucumbenciaReciproca() || !scenario.recursoPrincipalInterposto() || !scenario.recursoPrincipalConhecido()) {
            return false;
        }
        if (scenario.contrarrazoesJaProtocoladas()) {
            return false;
        }
        return recommendations.stream()
                .map(RecursalAutomationRecommendation::recurso)
                .anyMatch(recurso -> recurso.equals("APELACAO") || recurso.equals("RECURSO_ESPECIAL") || recurso.equals("RECURSO_EXTRAORDINARIO"));
    }

    private String buildAdesiveNote(RecursalAutomationScenario scenario,
                                    boolean admits,
                                    RecursalPoderRecorrerEvento eventoBloqueio) {
        if (eventoBloqueio != RecursalPoderRecorrerEvento.NENHUM) {
            return "recurso adesivo fica inviável porque o poder de recorrer está bloqueado por " + eventoBloqueio.name().toLowerCase(Locale.ROOT);
        }
        if (admits) {
            return "recurso adesivo é viável porque há sucumbência recíproca, recurso principal já interposto, principal conhecido e janela de contrarrazões ainda aberta";
        }
        if (!scenario.sucumbenciaReciproca()) {
            return "recurso adesivo depende de sucumbência recíproca";
        }
        if (!scenario.recursoPrincipalInterposto()) {
            return "recurso adesivo exige recurso principal previamente interposto pela outra parte";
        }
        if (!scenario.recursoPrincipalConhecido()) {
            return "recurso adesivo fica subordinado ao conhecimento do recurso principal";
        }
        if (scenario.contrarrazoesJaProtocoladas()) {
            return "recurso adesivo exige janela útil de contrarrazões ainda aberta; contrarrazões já protocoladas bloqueiam a via adesiva no cenário informado";
        }
        return "recurso adesivo não se encaixa no cenário informado";
    }

    private RecursalAutomationScenario toScenario(RecursalAutomationRequest request) {
        Set<String> grounds = request.fundamentosEmbargos() == null
                ? Set.of()
                : request.fundamentosEmbargos().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new RecursalAutomationScenario(
                RecursalPronunciamentoJudicial.valueOf(request.pronunciamentoJudicial().trim().toUpperCase(Locale.ROOT)),
                RecursalPretensaoRecursal.valueOf(request.pretensaoRecursal().trim().toUpperCase(Locale.ROOT)),
                request.sucumbenciaReciproca(),
                request.recursoPrincipalInterposto(),
                request.recursoPrincipalConhecido(),
                request.processoFisico(),
                request.autosEletronicos(),
                request.preparoEfetuado(),
                request.preparoInsuficiente(),
                request.feriadoLocalAplicavel(),
                request.feriadoLocalComprovado(),
                request.pretendeEfeitoInfringente(),
                request.contrarrazoesJaProtocoladas(),
                request.inadmissaoRecursoExcepcional(),
                request.divergenciaJurisprudencialInterna(),
                request.renunciaDireitoRecorrer(),
                request.desistiuRecursoInterposto(),
                request.aquiescenciaExpressaOuTacita(),
                request.desejaSustentacaoOral(),
                request.juizadoEspecial(),
                grounds
        );
    }

    private RecursalPoderRecorrerEvento resolveBlockingEvent(RecursalAutomationScenario scenario) {
        return RecursalPoderRecorrerBlueprint.resolve(
                scenario.renunciaDireitoRecorrer(),
                scenario.desistiuRecursoInterposto(),
                scenario.aquiescenciaExpressaOuTacita()
        );
    }

    private RecursalMeritoErroTipo meritTypeFor(RecursalAutomationScenario scenario) {
        return scenario.pretensaoRecursal() == RecursalPretensaoRecursal.INVALIDAR
                ? RecursalMeritoErroTipo.ERROR_IN_PROCEDENDO
                : RecursalMeritoErroTipo.ERROR_IN_JUDICANDO;
    }

    private RecursalAutomationCandidateView toView(RecursalAutomationRecommendation recommendation) {
        return new RecursalAutomationCandidateView(
                recommendation.recurso(),
                recommendation.prioridade(),
                recommendation.fundamentoBase(),
                recommendation.prazoDiasUteis(),
                recommendation.juizoAdmissibilidadeCompetencia(),
                recommendation.meritoErroTipoSugerido(),
                recommendation.secoesObrigatorias(),
                recommendation.subordinadoAoPrincipal()
        );
    }

    private RecursalAutomationSignalView toView(RecursalAutomationSignal signal) {
        return new RecursalAutomationSignalView(
                signal.codigo(),
                signal.status().name(),
                signal.mensagem()
        );
    }

    private static void reindexPriorities(List<RecursalAutomationRecommendation> recommendations) {
        for (int index = 0; index < recommendations.size(); index++) {
            RecursalAutomationRecommendation current = recommendations.get(index);
            recommendations.set(index, new RecursalAutomationRecommendation(
                    current.recurso(),
                    index + 1,
                    current.fundamentoBase(),
                    current.prazoDiasUteis(),
                    current.juizoAdmissibilidadeCompetencia(),
                    current.meritoErroTipoSugerido(),
                    current.secoesObrigatorias(),
                    current.subordinadoAoPrincipal()
            ));
        }
    }
}
