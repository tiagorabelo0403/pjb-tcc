package com.tcc.pjb.backend.service.api.surface;

import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminPlanRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminSubscriptionRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceAdminWebhookRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceComplementoDocumentalResponse;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthClientRegistrationRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthIntrospectionRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthRevocationRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceOauthTokenRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloRequest;
import com.tcc.pjb.backend.model.dto.processo.marketplace.MarketplaceProtocoloResponse;
import com.tcc.pjb.backend.model.dto.security.SigiloZkChallengeRequest;
import com.tcc.pjb.backend.model.dto.security.SigiloZkChallengeResponse;
import com.tcc.pjb.backend.model.dto.security.SigiloZkVerificationRequest;
import com.tcc.pjb.backend.model.dto.security.SigiloZkVerificationResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.api.ApiMarketplaceService;
import com.tcc.pjb.backend.service.api.MarketplaceDocumentoComplementarService;
import com.tcc.pjb.backend.service.api.MarketplaceGovernanceService;
import com.tcc.pjb.backend.service.api.MarketplaceWebhookDispatcherService;
import com.tcc.pjb.backend.service.api.oauth.MarketplaceOAuth2Service;
import com.tcc.pjb.backend.service.security.SigiloZeroKnowledgeProofService;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class MarketplaceSurfaceFacadeService {

    private final ApiMarketplaceService apiMarketplaceService;
    private final MarketplaceOAuth2Service oauth2Service;
    private final MarketplaceGovernanceService governanceService;
    private final MarketplaceWebhookDispatcherService webhookDispatcherService;
    private final SigiloZeroKnowledgeProofService zkService;
    private final SurfaceProjectionSupport projectionSupport;
    private final MarketplaceDocumentoComplementarService documentoComplementarService;

    public MarketplaceSurfaceFacadeService(ApiMarketplaceService apiMarketplaceService,
                                           MarketplaceOAuth2Service oauth2Service,
                                           MarketplaceGovernanceService governanceService,
                                           MarketplaceWebhookDispatcherService webhookDispatcherService,
                                           SigiloZeroKnowledgeProofService zkService,
                                           SurfaceProjectionSupport projectionSupport,
                                           MarketplaceDocumentoComplementarService documentoComplementarService) {
        this.apiMarketplaceService = Objects.requireNonNull(apiMarketplaceService);
        this.oauth2Service = Objects.requireNonNull(oauth2Service);
        this.governanceService = Objects.requireNonNull(governanceService);
        this.webhookDispatcherService = Objects.requireNonNull(webhookDispatcherService);
        this.zkService = Objects.requireNonNull(zkService);
        this.projectionSupport = Objects.requireNonNull(projectionSupport);
        this.documentoComplementarService = Objects.requireNonNull(documentoComplementarService);
    }

    public MarketplaceProtocoloResponse protocolar(MarketplaceProtocoloRequest request, String clientId) {
        var result = apiMarketplaceService.protocolar(new ApiMarketplaceService.MarketplaceProtocoloRequest(
                request.clientReference(),
                request.numeroExterno(),
                request.tipoJustica(),
                request.ramoDireito(),
                request.uf(),
                request.comarca(),
                request.classeProcessual(),
                request.assunto(),
                request.pedidoPrincipal(),
                request.pedidosConsolidados(),
                request.parteAutoraNome(),
                request.parteAutoraCpf(),
                request.parteReuNome(),
                request.parteReuCpf(),
                request.valorCausa(),
                request.ufAutor(),
                request.comarcaAutor(),
                request.ufReu(),
                request.comarcaReu(),
                request.enderecoReuDesconhecido(),
                request.documentos(),
                request.perfilAtor()
        ), clientId);
        return new MarketplaceProtocoloResponse(
                result.processoId(),
                result.numeroProcesso(),
                result.protocoloMarketplace(),
                result.status(),
                result.tipoJustica(),
                result.ramoDireito(),
                result.recebidoEm(),
                result.documentacaoCompleta(),
                result.documentosFaltantes()
        );
    }

    public SurfaceCollectionResponse listarClientesOauth() {
        return projectionSupport.collection("marketplace.oauth.clients", oauth2Service.listarClientes());
    }

    public SurfaceSnapshotResponse registrarClienteOauth(MarketplaceOauthClientRegistrationRequest request, String ipAddress) {
        return projectionSupport.snapshot("marketplace.oauth.client-registration", oauth2Service.registrarCliente(
                new MarketplaceOAuth2Service.ClientRegistrationRequest(
                        request.clientId(),
                        request.displayName(),
                        request.ownerName(),
                        request.ownerEmail(),
                        request.allowedScopes(),
                        request.trustedOrigin(),
                        request.accessTokenTtlSeconds()
                ),
                ipAddress
        ));
    }

    public SurfaceSnapshotResponse emitirTokenOauth(MarketplaceOauthTokenRequest request, String ipAddress) {
        return projectionSupport.snapshot("marketplace.oauth.token", oauth2Service.emitirToken(
                new MarketplaceOAuth2Service.TokenRequest(
                        request.clientId(),
                        request.clientSecret(),
                        request.grantType(),
                        request.scope()
                ),
                ipAddress
        ));
    }

    public SurfaceSnapshotResponse introspectOauth(MarketplaceOauthIntrospectionRequest request, String ipAddress) {
        return projectionSupport.snapshot("marketplace.oauth.introspection", oauth2Service.introspect(request.token(), ipAddress));
    }

    public SurfaceActionResponse revokeOauth(MarketplaceOauthRevocationRequest request, String ipAddress) {
        return projectionSupport.action("marketplace.oauth", "revoke", null, oauth2Service.revogar(request.token(), request.motivo(), ipAddress));
    }

    public SurfaceCollectionResponse listarPlanos() {
        return projectionSupport.collection("marketplace.admin.plans", governanceService.listarPlanos());
    }

    public SurfaceSnapshotResponse criarPlano(MarketplaceAdminPlanRequest request) {
        return projectionSupport.snapshot("marketplace.admin.plan", governanceService.criarPlano(
                new MarketplaceGovernanceService.PlanRequest(
                        request.code(),
                        request.displayName(),
                        request.maxProtocolosDia(),
                        request.maxWebhookEndpoints(),
                        request.allowStreaming(),
                        request.allowHighVolume(),
                        request.description()
                )
        ));
    }

    public SurfaceCollectionResponse listarAssinaturas(String clientId) {
        return projectionSupport.collection("marketplace.admin.subscriptions", governanceService.listarAssinaturas(clientId));
    }

    public SurfaceSnapshotResponse vincularAssinatura(String clientId, MarketplaceAdminSubscriptionRequest request) {
        return projectionSupport.snapshot("marketplace.admin.subscription", governanceService.vincularCliente(
                clientId,
                new MarketplaceGovernanceService.SubscriptionRequest(
                        request.planCode(),
                        request.startedAt(),
                        request.endsAt(),
                        request.webhookEndpointLimitOverride(),
                        request.observacoes()
                )
        ));
    }

    public SurfaceCollectionResponse listarWebhooks(String clientId) {
        return projectionSupport.collection("marketplace.admin.webhooks", governanceService.listarWebhooks(clientId));
    }

    public SurfaceSnapshotResponse registrarWebhook(String clientId, MarketplaceAdminWebhookRequest request) {
        return projectionSupport.snapshot("marketplace.admin.webhook", governanceService.registrarWebhook(
                clientId,
                new MarketplaceGovernanceService.WebhookRequest(
                        request.callbackUrl(),
                        request.eventFilter(),
                        request.signingSecret()
                )
        ));
    }

    public SurfaceCollectionResponse listarEntregas(String clientId) {
        return projectionSupport.collection("marketplace.admin.webhook-deliveries", governanceService.listarEntregas(clientId));
    }

    public SurfaceActionResponse dispatchPendentes(int limit) {
        return projectionSupport.action("marketplace.admin.webhook-deliveries", "dispatch", null, webhookDispatcherService.dispatchPending(limit));
    }

    public SurfaceActionResponse redispatch(String clientId, Long deliveryId) {
        return projectionSupport.action("marketplace.admin.webhook-deliveries", "redispatch", deliveryId, webhookDispatcherService.redispatch(clientId, deliveryId));
    }

    public SigiloZkChallengeResponse emitirSigiloChallenge(Long processoId, SigiloZkChallengeRequest request) {
        var result = zkService.emitirDesafio(processoId, new SigiloZeroKnowledgeProofService.ChallengeRequest(request.escopo(), request.statement()));
        return new SigiloZkChallengeResponse(
                result.challengeId(),
                result.processoId(),
                result.numeroProcesso(),
                result.escopo(),
                result.statement(),
                result.challengePayload(),
                result.commitmentHash(),
                result.expiraEm()
        );
    }

    public SigiloZkVerificationResponse verificarSigiloChallenge(String challengeId, SigiloZkVerificationRequest request) {
        var result = zkService.verificar(challengeId, new SigiloZeroKnowledgeProofService.VerificationRequest(
                request.snapshotHashConhecido(),
                request.proofToken()
        ));
        return new SigiloZkVerificationResponse(
                result.challengeId(),
                result.processoId(),
                result.numeroProcesso(),
                result.verificado(),
                result.snapshotHashConfere(),
                result.provaConfere(),
                result.status(),
                result.verificadoEm()
        );
    }

    public MarketplaceComplementoDocumentalResponse complementarDocumentos(Long processoId,
                                                                            MarketplaceComplementoDocumentalRequest request,
                                                                            String clientId) {
        return documentoComplementarService.complementar(processoId, request.documentos(), clientId);
    }
}
