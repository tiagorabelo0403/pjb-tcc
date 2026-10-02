package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.AlvoJuridico;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.MotorInterceptacaoAtiva;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ReciboCitacaoHsm;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ViaInterceptacao;
import com.tcc.pjb.backend.model.entity.Processo;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CitacaoInterceptacaoDigitalService {

    private static final Logger log = LoggerFactory.getLogger(CitacaoInterceptacaoDigitalService.class);

    private final ObjectProvider<MotorInterceptacaoAtiva> motorInterceptacaoProvider;
    private final PjbHsmProperties hsmProperties;
    private final CitacaoSefazCadastroEnrichmentService citacaoSefazCadastroEnrichmentService;

    public CitacaoInterceptacaoDigitalService(ObjectProvider<MotorInterceptacaoAtiva> motorInterceptacaoProvider,
                                              PjbHsmProperties hsmProperties,
                                              CitacaoSefazCadastroEnrichmentService citacaoSefazCadastroEnrichmentService) {
        this.motorInterceptacaoProvider = Objects.requireNonNull(motorInterceptacaoProvider, "motorInterceptacaoProvider");
        this.hsmProperties = Objects.requireNonNull(hsmProperties, "hsmProperties");
        this.citacaoSefazCadastroEnrichmentService = Objects.requireNonNull(citacaoSefazCadastroEnrichmentService, "citacaoSefazCadastroEnrichmentService");
    }

    public enum Tipo {
        ENTREGUE,
        FRUSTRADO,
        SEM_ACAO
    }

    public record Resultado(Tipo tipo, ReciboCitacaoHsm recibo, String motivo) {

        public static Resultado entregue(ReciboCitacaoHsm recibo) {
            return new Resultado(Tipo.ENTREGUE, recibo, null);
        }

        public static Resultado frustrado(String motivo) {
            return new Resultado(Tipo.FRUSTRADO, null, motivo);
        }

        public static Resultado semAcao() {
            return new Resultado(Tipo.SEM_ACAO, null, null);
        }
    }

    public Resultado interceptar(ExpedicaoJudicial expedicao,
                                 CitacaoIntimacaoEngine.PerfilDestinatario destinatario,
                                 byte[] conteudo,
                                 Processo processo) {
        MotorInterceptacaoAtiva motor = motorInterceptacaoProvider.getIfAvailable();
        if (motor == null) {
            log.warn("[CitacaoInterceptacao] MotorInterceptacaoAtiva indisponivel para uuid={}", expedicao.getExpedicaoUuid());
            return Resultado.semAcao();
        }
        citacaoSefazCadastroEnrichmentService.enriquecerExpedicaoComCadastroSefaz(expedicao, processo);
        List<ViaInterceptacao> vias = CitacaoIntimacaoExpedicaoSupport.montarVias(destinatario, processo, expedicao);
        if (vias.isEmpty()) {
            return Resultado.frustrado("Nenhuma via segura de interceptação foi construída para o destinatário.");
        }
        AlvoJuridico alvo = CitacaoIntimacaoExpedicaoSupport.construirAlvo(expedicao, processo, hsmProperties);
        ReciboCitacaoHsm recibo = motor.interceptarComViasSugeridas(alvo, conteudo, vias);
        if (recibo.foiEntregue()) {
            log.info("[CitacaoInterceptacao] Interceptacao bem-sucedida uuid={} canal={}", expedicao.getExpedicaoUuid(), recibo.canalVencedor());
            return Resultado.entregue(recibo);
        }
        motor.acionarFallbackFisicoSeNecessario(recibo, expedicao.getExpedicaoUuid());
        return Resultado.semAcao();
    }
}
