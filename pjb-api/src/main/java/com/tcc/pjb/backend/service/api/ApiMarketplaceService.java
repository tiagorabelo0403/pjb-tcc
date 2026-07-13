package com.tcc.pjb.backend.service.api;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.service.AjuizamentoService;

@Service
public class ApiMarketplaceService {

    private final AjuizamentoService ajuizamentoService;
    private final MarketplaceGovernanceService governanceService;

    public ApiMarketplaceService(AjuizamentoService ajuizamentoService,
                                 MarketplaceGovernanceService governanceService) {
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.governanceService = Objects.requireNonNull(governanceService);
    }

    @Transactional
    public MarketplaceProtocoloResponse protocolar(MarketplaceProtocoloRequest request, String clientId) {
        governanceService.assertClientCanProtocol(clientId);

        Processo processo = new Processo();
        processo.setNumeroUnificado(request.numeroExterno());
        processo.setNumeroProcesso(request.numeroExterno());
        processo.setTipoJustica(TipoJustica.fromString(request.tipoJustica()) == null ? TipoJustica.ESTADUAL : TipoJustica.fromString(request.tipoJustica()));
        processo.setRamoDireito(RamoDireito.fromString(request.ramoDireito()) == null ? RamoDireito.CIVIL : RamoDireito.fromString(request.ramoDireito()));
        processo.setMateria(MateriaJurisdicao.fromRamo(processo.getRamoDireito()));
        processo.setUf(request.uf());
        processo.setComarca(request.comarca());
        processo.setClasseProcessual(request.classeProcessual());
        processo.setAssunto(request.assunto());
        processo.setPedidoPrincipal(request.pedidoPrincipal());
        processo.setPedidosConsolidados(request.pedidosConsolidados());
        processo.setParteAutoraNome(request.parteAutoraNome());
        processo.setParteAutoraCpf(request.parteAutoraCpf());
        processo.setParteReuNome(request.parteReuNome());
        processo.setParteReuCpf(request.parteReuCpf());
        processo.setValorCausa(request.valorCausa());
        processo.setConnectorSystem("MARKETPLACE_API");
        processo.setConnectorProtocolReference(clientId + ":" + request.clientReference());
        processo.setConnectorSubmissionStatus("RECEBIDO_MARKETPLACE");
        processo.setConnectorSubmissionMessage("Protocolo recebido via marketplace OAuth2 preparado para integradores.");
        processo.setConnectorSubmissionProcessedAt(LocalDateTime.now());
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        Processo salvo = ajuizamentoService.ajuizar(processo);

        governanceService.registrarConsumoProtocolo(clientId);
        governanceService.publicarEventoProtocolo(clientId, salvo.getId(), salvo.getNumeroProcesso(), salvo.getConnectorProtocolReference());

        return new MarketplaceProtocoloResponse(
                salvo.getId(),
                salvo.getNumeroProcesso(),
                salvo.getConnectorProtocolReference(),
                salvo.getConnectorSubmissionStatus(),
                salvo.getTipoJustica() != null ? salvo.getTipoJustica().name() : null,
                salvo.getRamoDireito() != null ? salvo.getRamoDireito().name() : null,
                LocalDateTime.now()
        );
    }

    public record MarketplaceProtocoloRequest(
            @NotBlank String clientReference,
            @NotBlank String numeroExterno,
            String tipoJustica,
            String ramoDireito,
            @NotBlank String uf,
            String comarca,
            @NotBlank String classeProcessual,
            String assunto,
            @NotBlank String pedidoPrincipal,
            String pedidosConsolidados,
            @NotBlank String parteAutoraNome,
            String parteAutoraCpf,
            String parteReuNome,
            String parteReuCpf,
            java.math.BigDecimal valorCausa
    ) {
    }

    public record MarketplaceProtocoloResponse(
            Long processoId,
            String numeroProcesso,
            String protocoloMarketplace,
            String status,
            String tipoJustica,
            String ramoDireito,
            LocalDateTime recebidoEm
    ) {
    }
}
