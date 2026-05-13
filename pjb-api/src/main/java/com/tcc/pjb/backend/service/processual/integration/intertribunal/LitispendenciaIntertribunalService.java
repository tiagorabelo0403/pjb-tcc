package com.tcc.pjb.backend.service.processual.integration.intertribunal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.dto.ajuizamento.federal.FederalismoNodeResponse;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;

@Service
public class LitispendenciaIntertribunalService {

    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final FederalismoJudicialEngine federalismoJudicialEngine;

    public LitispendenciaIntertribunalService(TriagemNacionalIAEngine triagemNacionalIAEngine,
                                              FederalismoJudicialEngine federalismoJudicialEngine) {
        this.triagemNacionalIAEngine = Objects.requireNonNull(triagemNacionalIAEngine);
        this.federalismoJudicialEngine = Objects.requireNonNull(federalismoJudicialEngine);
    }

    @Transactional(readOnly = true)
    public LitispendenciaReport analisar(LitispendenciaProbeRequest request) {
        TriagemNacionalIAEngine.PedidoTriagem pedido = new TriagemNacionalIAEngine.PedidoTriagem(
                request.nupnProvisorio(),
                request.classeTpuSugerida(),
                request.assuntoTpuSugerido(),
                request.ramoDireito(),
                request.valorCausa(),
                request.textoFatosResumido(),
                request.cpfCnpjAutor(),
                request.cpfCnpjReu(),
                request.oabAdvogado(),
                request.ufAdvogado(),
                request.documentosAnexados(),
                request.dataFatoGerador(),
                request.requerLiminar(),
                request.atoJurisdicionalAnterior(),
                request.processoId()
        );
        TriagemNacionalIAEngine.ResultadoTriagem resultado = triagemNacionalIAEngine.triar(pedido);
        List<FederalismoNodeResponse> nos = federalismoJudicialEngine.listarNos();
        long nosSincronizados = nos.stream().filter(FederalismoNodeResponse::aceitaRecepcaoEventos).count();
        long nosOnline = nos.stream().filter(FederalismoNodeResponse::operacaoAutonomaAtiva).count();
        return new LitispendenciaReport(
                resultado.nupnProvisorio(),
                resultado.veredito().name(),
                resultado.confiancaGeral(),
                resultado.resumoDecisao(),
                resultado.processosConexos().stream().map(conexo -> new ProcessoConexoView(
                        conexo.nupn(),
                        conexo.tribunalCodigo(),
                        conexo.similaridade(),
                        conexo.tipoConexao().name(),
                        conexo.descricaoSimilaridade()
                )).toList(),
                resultado.pendencias().stream().map(p -> new PendenciaView(
                        p.tipo().name(),
                        p.descricao(),
                        p.impeditiva(),
                        p.severidade().name(),
                        p.orientacaoCorrecao()
                )).toList(),
                new CoberturaFederativaView(nos.size(), nosOnline, nosSincronizados,
                        nos.stream().map(no -> new TribunalFederadoView(no.codigoTribunal(), no.uf(), no.statusAtual() != null ? no.statusAtual().name() : null, no.capacidades())).toList()),
                Instant.now()
        );
    }

    public record LitispendenciaProbeRequest(
            String nupnProvisorio,
            String classeTpuSugerida,
            String assuntoTpuSugerido,
            String ramoDireito,
            BigDecimal valorCausa,
            String textoFatosResumido,
            String cpfCnpjAutor,
            String cpfCnpjReu,
            String oabAdvogado,
            String ufAdvogado,
            List<String> documentosAnexados,
            LocalDate dataFatoGerador,
            boolean requerLiminar,
            boolean atoJurisdicionalAnterior,
            Long processoId
    ) {
    }

    public record LitispendenciaReport(
            String nupnProvisorio,
            String veredito,
            double confiancaGeral,
            String resumoDecisao,
            List<ProcessoConexoView> processosConexos,
            List<PendenciaView> pendencias,
            CoberturaFederativaView coberturaFederativa,
            Instant analisadoEm
    ) {
    }

    public record ProcessoConexoView(
            String nupn,
            String tribunalCodigo,
            double similaridade,
            String tipoConexao,
            String descricaoSimilaridade
    ) {
    }

    public record PendenciaView(
            String tipo,
            String descricao,
            boolean impeditiva,
            String severidade,
            String orientacaoCorrecao
    ) {
    }

    public record CoberturaFederativaView(
            long totalNos,
            long nosOnline,
            long nosSincronizados,
            List<TribunalFederadoView> tribunais
    ) {
    }

    public record TribunalFederadoView(
            String codigoTribunal,
            String uf,
            String statusAtual,
            java.util.Set<String> capacidades
    ) {
    }
}
