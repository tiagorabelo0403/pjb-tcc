package com.tcc.pjb.backend.core.processo.stf.application;

import com.tcc.pjb.backend.core.processo.estado.application.ProcessoEstadoApplicationService;
import com.tcc.pjb.backend.core.processo.stf.domain.VotacaoPlenarioEngine;
import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorSTF;
import com.tcc.pjb.backend.model.entity.enums.ResultadoVotacaoSTF;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoSessaoSTF;
import com.tcc.pjb.backend.model.entity.processo.ImpedimentoMinistro;
import com.tcc.pjb.backend.model.entity.processo.PautaSTF;
import com.tcc.pjb.backend.model.entity.processo.PedidoVistaSTF;
import com.tcc.pjb.backend.model.repository.ImpedimentoMinistroRepository;
import com.tcc.pjb.backend.model.repository.PautaSTFRepository;
import com.tcc.pjb.backend.model.repository.PedidoVistaSTFRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PautaSTFApplicationService {

    private static final int DIAS_PEDIDO_VISTA = 90;
    private static final Set<ResultadoVotacaoSTF> RESULTADOS_FINAIS = EnumSet.of(
            ResultadoVotacaoSTF.APROVADO, ResultadoVotacaoSTF.REJEITADO,
            ResultadoVotacaoSTF.EMPATE_SOLUCAO_CONTRARIA, ResultadoVotacaoSTF.EMPATE_VOTO_QUALIDADE);

    private final PautaSTFRepository pautaRepository;
    private final PedidoVistaSTFRepository pedidoVistaRepository;
    private final ImpedimentoMinistroRepository impedimentoRepository;
    private final ProcessoEstadoApplicationService estadoService;
    private final VotacaoPlenarioEngine votacaoEngine;

    @Inject
    public PautaSTFApplicationService(PautaSTFRepository pautaRepository,
                                       PedidoVistaSTFRepository pedidoVistaRepository,
                                       ImpedimentoMinistroRepository impedimentoRepository,
                                       ProcessoEstadoApplicationService estadoService) {
        this.pautaRepository = pautaRepository;
        this.pedidoVistaRepository = pedidoVistaRepository;
        this.impedimentoRepository = impedimentoRepository;
        this.estadoService = estadoService;
        this.votacaoEngine = new VotacaoPlenarioEngine();
    }

    @Transactional
    public PautaSTF incluirNaPauta(Long processoId, OrgaoJulgadorSTF orgao,
                                    TipoSessaoSTF tipoSessao, LocalDate dataPauta, Long relatorId) {
        PautaSTF pauta = new PautaSTF(processoId, orgao, tipoSessao, dataPauta, relatorId);
        return pautaRepository.save(pauta);
    }

    @Transactional
    public PautaSTF registrarVotacao(Long pautaId, int votosFavoraveis, int votosContrarios,
                                      int votosVista, int ministrosImpedidos,
                                      boolean presidenteVotouFavoravel) {
        PautaSTF pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new EntityNotFoundException("Pauta STF não encontrada: " + pautaId));
        ResultadoVotacaoSTF resultado = votacaoEngine.calcular(pauta.getOrgaoJulgador(),
                votosFavoraveis, votosContrarios, votosVista, ministrosImpedidos, presidenteVotouFavoravel);
        pauta.registrarVotacao(votosFavoraveis, votosContrarios, votosVista, ministrosImpedidos, resultado);
        pautaRepository.save(pauta);
        if (RESULTADOS_FINAIS.contains(resultado)) {
            estadoService.transitar(pauta.getProcessoId(), StatusProcesso.SENTENCIADO,
                    pauta.getRelatorId(), "Votação plenária: " + resultado);
        }
        return pauta;
    }

    @Transactional
    public PedidoVistaSTF registrarPedidoVista(Long pautaId, Long ministroId) {
        Instant dataLimite = Instant.now().plusSeconds((long) DIAS_PEDIDO_VISTA * 86400);
        PedidoVistaSTF pedido = new PedidoVistaSTF(pautaId, ministroId, dataLimite);
        return pedidoVistaRepository.save(pedido);
    }

    @Transactional
    public void declararImpedimento(Long processoId, Long ministroId,
                                     String tipoImpedimento, String motivo) {
        ImpedimentoMinistro impedimento = new ImpedimentoMinistro(processoId, ministroId,
                tipoImpedimento, motivo);
        impedimentoRepository.save(impedimento);
    }

    @Transactional(readOnly = true)
    public boolean estaImpedido(Long processoId, Long ministroId) {
        return impedimentoRepository.existsByProcessoIdAndMinistroId(processoId, ministroId);
    }
}
