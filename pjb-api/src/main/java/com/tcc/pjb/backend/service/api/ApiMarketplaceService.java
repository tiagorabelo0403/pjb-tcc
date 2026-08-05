package com.tcc.pjb.backend.service.api;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;

@Slf4j
@Service
public class ApiMarketplaceService {

    private static final String STATUS_RECEBIDO = "RECEBIDO_MARKETPLACE";
    private static final String STATUS_PENDENTE_DOCUMENTACAO = "PENDENTE_DOCUMENTACAO";

    private final AjuizamentoService ajuizamentoService;
    private final MarketplaceGovernanceService governanceService;
    private final CompletudeDocumentalPolicyService completudeDocumentalPolicyService;
    private final MarketplaceRepresentacaoResolver representacaoResolver;
    private final MarketplaceDocumentoPersistenceService documentoPersistenceService;

    public ApiMarketplaceService(AjuizamentoService ajuizamentoService,
                                 MarketplaceGovernanceService governanceService,
                                 CompletudeDocumentalPolicyService completudeDocumentalPolicyService,
                                 MarketplaceRepresentacaoResolver representacaoResolver,
                                 MarketplaceDocumentoPersistenceService documentoPersistenceService) {
        this.ajuizamentoService = Objects.requireNonNull(ajuizamentoService);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.completudeDocumentalPolicyService = Objects.requireNonNull(completudeDocumentalPolicyService);
        this.representacaoResolver = Objects.requireNonNull(representacaoResolver);
        this.documentoPersistenceService = Objects.requireNonNull(documentoPersistenceService);
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
        processo.setUfAutor(request.ufAutor());
        processo.setComarcaAutor(request.comarcaAutor());
        if (!request.enderecoReuDesconhecido()) {
            processo.setUfReu(request.ufReu());
            processo.setComarcaReu(request.comarcaReu());
        }
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
        processo.setConnectorSubmissionProcessedAt(LocalDateTime.now());
        processo.setFaseAtual(FaseProcessual.CONHECIMENTO);
        processo.setStatusProcesso(StatusProcesso.DISTRIBUIDO);
        processo.setRito(ProceduralCatalogSupport.tryResolveRito(null, request.ramoDireito(), request.classeProcessual())
                .orElse(RitoProcessual.COMUM_ORDINARIO));
        processo.setDataCriacao(LocalDateTime.now());
        processo.setDataDistribuicao(LocalDateTime.now());
        processo.setDataUltimaMovimentacao(LocalDateTime.now());

        var instrumento = representacaoResolver.resolve(processo.getRamoDireito(), processo.getRito(),
                processo.getTribunal(), request.perfilAtor());
        processo.setInstrumentoRepresentacaoResolvido(instrumento == null ? null : instrumento.name());
        var diagnostico = completudeDocumentalPolicyService.diagnosticar(processo.getRito(), request.documentos(), instrumento);
        List<String> documentosFaltantes = diagnostico.faltantes().stream().map(Enum::name).toList();
        boolean documentacaoCompleta = !diagnostico.bloqueante();

        if (documentacaoCompleta) {
            processo.setConnectorSubmissionStatus(STATUS_RECEBIDO);
            processo.setConnectorSubmissionMessage("Protocolo recebido via marketplace OAuth2 preparado para integradores.");
        } else {
            processo.setConnectorSubmissionStatus(STATUS_PENDENTE_DOCUMENTACAO);
            processo.setConnectorSubmissionMessage(
                    "Protocolo recebido via marketplace, pendente de documentacao obrigatoria: " + documentosFaltantes);
        }

        Processo salvo = ajuizamentoService.ajuizar(processo);

        if (request.documentos() != null) {
            for (Attachment attachment : request.documentos()) {
                try {
                    documentoPersistenceService.persistirSeNovo(salvo, attachment);
                } catch (ErroDeValidacaoException e) {
                    log.warn("Documento declarado no protocolo marketplace não pôde ser persistido "
                                    + "(protocolo segue de qualquer forma): processoId={} clientId={} erro={}",
                            salvo.getId(), clientId, e.getMessage());
                }
            }
        }

        governanceService.registrarConsumoProtocolo(clientId);
        governanceService.publicarEventoProtocolo(clientId, salvo.getId(), salvo.getNumeroProcesso(), salvo.getConnectorProtocolReference());
        if (!documentacaoCompleta) {
            governanceService.publicarEventoPendenciaDocumental(clientId, salvo.getId(), salvo.getNumeroProcesso(),
                    salvo.getConnectorProtocolReference(), documentosFaltantes);
        }

        return new MarketplaceProtocoloResponse(
                salvo.getId(),
                salvo.getNumeroProcesso(),
                salvo.getConnectorProtocolReference(),
                salvo.getConnectorSubmissionStatus(),
                salvo.getTipoJustica() != null ? salvo.getTipoJustica().name() : null,
                salvo.getRamoDireito() != null ? salvo.getRamoDireito().name() : null,
                LocalDateTime.now(),
                documentacaoCompleta,
                documentosFaltantes
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
            java.math.BigDecimal valorCausa,
            String ufAutor,
            String comarcaAutor,
            String ufReu,
            String comarcaReu,
            boolean enderecoReuDesconhecido,
            List<Attachment> documentos,
            String perfilAtor
    ) {
    }

    public record MarketplaceProtocoloResponse(
            Long processoId,
            String numeroProcesso,
            String protocoloMarketplace,
            String status,
            String tipoJustica,
            String ramoDireito,
            LocalDateTime recebidoEm,
            boolean documentacaoCompleta,
            List<String> documentosFaltantes
    ) {
    }
}
