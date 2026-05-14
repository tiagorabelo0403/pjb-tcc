package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RitoComputavelMatrix {

    public record FaseComputa(
            FaseProcessual fase,
            Duration prazoLegal,
            boolean obrigatoria,
            Set<FaseProcessual> transicoesValidas,
            List<String> atosObrigatorios,
            List<String> atosProibidos
    ) {}

    public record RitoComputavelInfo(
            RitoProcessual rito,
            List<FaseComputa> fasesOrdenadas,
            boolean admiteAudienciaUna,
            boolean exigeCitacaoFisica,
            Duration prazoMaximoTotal
    ) {}

    private static final Map<RitoProcessual, RitoComputavelInfo> MATRIX = buildMatrix();

    public Optional<RitoComputavelInfo> consultar(RitoProcessual rito) {
        return Optional.ofNullable(MATRIX.get(rito));
    }

    public List<FaseProcessual> fasesValidasPara(RitoProcessual rito) {
        return consultar(rito)
                .map(i -> i.fasesOrdenadas().stream().map(FaseComputa::fase).toList())
                .orElseGet(() -> List.of(FaseProcessual.CONHECIMENTO, FaseProcessual.RECURSAL));
    }

    public boolean transicaoValida(RitoProcessual rito, FaseProcessual de, FaseProcessual para) {
        return consultar(rito)
                .flatMap(info -> info.fasesOrdenadas().stream()
                        .filter(fc -> fc.fase() == de)
                        .findFirst())
                .map(fc -> fc.transicoesValidas().contains(para))
                .orElse(true);
    }

    private static Map<RitoProcessual, RitoComputavelInfo> buildMatrix() {
        return Map.ofEntries(
            Map.entry(RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO, buildJuizado()),
            Map.entry(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, buildJuizado()),
            Map.entry(RitoProcessual.TRABALHISTA_SUMARISSIMO, buildTrabalhistaSumarissimo()),
            Map.entry(RitoProcessual.PROCEDIMENTO_PENAL_COMUM, buildPenalOrdinario()),
            Map.entry(RitoProcessual.COMUM_ORDINARIO, buildCivilOrdinario()),
            Map.entry(RitoProcessual.EXECUCAO_FISCAL, buildExecucaoFiscal())
        );
    }

    private static RitoComputavelInfo buildJuizado() {
        List<FaseComputa> fases = List.of(
            new FaseComputa(FaseProcessual.AUTUACAO, Duration.ofDays(1), true,
                Set.of(FaseProcessual.CITACAO), List.of("AUTUACAO", "DISTRIBUICAO"), List.of()),
            new FaseComputa(FaseProcessual.CITACAO, Duration.ofDays(30), true,
                Set.of(FaseProcessual.JULGAMENTO), List.of("CITACAO_ELETRON"), List.of()),
            new FaseComputa(FaseProcessual.JULGAMENTO, Duration.ofDays(30), true,
                Set.of(FaseProcessual.ARQUIVAMENTO), List.of("SENTENCA"), List.of())
        );
        return new RitoComputavelInfo(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, fases,
                true, false, Duration.ofDays(90));
    }

    private static RitoComputavelInfo buildTrabalhistaSumarissimo() {
        List<FaseComputa> fases = List.of(
            new FaseComputa(FaseProcessual.AUTUACAO, Duration.ofDays(1), true,
                Set.of(FaseProcessual.CITACAO), List.of("AUTUACAO"), List.of()),
            new FaseComputa(FaseProcessual.CITACAO, Duration.ofDays(15), true,
                Set.of(FaseProcessual.JULGAMENTO), List.of("NOTIFICACAO"), List.of()),
            new FaseComputa(FaseProcessual.JULGAMENTO, Duration.ofDays(15), true,
                Set.of(FaseProcessual.ARQUIVAMENTO), List.of("AUDIENCIA_INSTRUCAO_UNA"), List.of())
        );
        return new RitoComputavelInfo(RitoProcessual.TRABALHISTA_SUMARISSIMO, fases,
                true, false, Duration.ofDays(45));
    }

    private static RitoComputavelInfo buildPenalOrdinario() {
        List<FaseComputa> fases = List.of(
            new FaseComputa(FaseProcessual.AUTUACAO, Duration.ofDays(1), true,
                Set.of(FaseProcessual.CITACAO), List.of("AUTUACAO"), List.of()),
            new FaseComputa(FaseProcessual.CITACAO, Duration.ofDays(10), true,
                Set.of(FaseProcessual.RESPOSTA), List.of("CITACAO"), List.of()),
            new FaseComputa(FaseProcessual.RESPOSTA, Duration.ofDays(10), true,
                Set.of(FaseProcessual.SANEAMENTO, FaseProcessual.INSTRUTORIA),
                List.of("RESPOSTA_ACUSACAO"), List.of()),
            new FaseComputa(FaseProcessual.JULGAMENTO, Duration.ofDays(60), true,
                Set.of(FaseProcessual.RECURSAL, FaseProcessual.ARQUIVAMENTO),
                List.of("AUDIENCIA_INSTRUCAO"), List.of())
        );
        return new RitoComputavelInfo(RitoProcessual.PROCEDIMENTO_PENAL_COMUM, fases,
                false, true, Duration.ofDays(360));
    }

    private static RitoComputavelInfo buildCivilOrdinario() {
        List<FaseComputa> fases = List.of(
            new FaseComputa(FaseProcessual.AUTUACAO, Duration.ofDays(1), true,
                Set.of(FaseProcessual.CITACAO), List.of("AUTUACAO"), List.of()),
            new FaseComputa(FaseProcessual.CITACAO, Duration.ofDays(15), true,
                Set.of(FaseProcessual.RESPOSTA), List.of("CITACAO_ELETRONICA"), List.of()),
            new FaseComputa(FaseProcessual.RESPOSTA, Duration.ofDays(15), false,
                Set.of(FaseProcessual.SANEAMENTO), List.of(), List.of()),
            new FaseComputa(FaseProcessual.SANEAMENTO, Duration.ofDays(30), true,
                Set.of(FaseProcessual.INSTRUTORIA, FaseProcessual.JULGAMENTO),
                List.of("DESPACHO_SANEADOR"), List.of()),
            new FaseComputa(FaseProcessual.JULGAMENTO, Duration.ofDays(180), true,
                Set.of(FaseProcessual.RECURSAL, FaseProcessual.ARQUIVAMENTO),
                List.of("SENTENCA"), List.of())
        );
        return new RitoComputavelInfo(RitoProcessual.COMUM_ORDINARIO, fases,
                false, true, Duration.ofDays(720));
    }

    private static RitoComputavelInfo buildExecucaoFiscal() {
        List<FaseComputa> fases = List.of(
            new FaseComputa(FaseProcessual.AUTUACAO, Duration.ofDays(1), true,
                Set.of(FaseProcessual.CITACAO), List.of("DESPACHO_INICIAL"), List.of()),
            new FaseComputa(FaseProcessual.CITACAO, Duration.ofDays(5), true,
                Set.of(FaseProcessual.EXECUCAO), List.of("CITACAO"), List.of()),
            new FaseComputa(FaseProcessual.EXECUCAO, Duration.ofDays(30), true,
                Set.of(FaseProcessual.PENHORA, FaseProcessual.ARQUIVAMENTO),
                List.of("PENHORA_OU_PARCELAMENTO"), List.of())
        );
        return new RitoComputavelInfo(RitoProcessual.EXECUCAO_FISCAL, fases,
                false, true, Duration.ofDays(365));
    }
}
