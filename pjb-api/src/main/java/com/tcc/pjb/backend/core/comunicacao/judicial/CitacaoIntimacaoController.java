package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/comunicacao-judicial")
public class CitacaoIntimacaoController {

    private final CitacaoIntimacaoEngine engine;
    private final CapabilityRateLimiter rateLimiter;
    private final ComunicacaoJudicialCompetenciaService competenciaService;

    public CitacaoIntimacaoController(CitacaoIntimacaoEngine engine,
                                      CapabilityRateLimiter rateLimiter,
                                      ComunicacaoJudicialCompetenciaService competenciaService) {
        this.engine = engine;
        this.rateLimiter = rateLimiter;
        this.competenciaService = competenciaService;
    }

    public record ExpedicaoApiRequest(
            @NotNull Long processoId,
            @NotNull TipoComunicacaoJudicial tipoComunicacao,
            @NotNull DestinatarioRequest destinatario,
            @NotBlank String conteudoDoAto,
            String fundamentoAdicional,
            boolean forcarDigital,
            boolean forcarOficial,
            Long juizResponsavelId,
            Long servidorExpedidorId
    ) {
    }

    public record DestinatarioRequest(
            @NotBlank String tipoDestinatario,
            String cpf,
            String cnpj,
            String nome,
            String razaoSocial,
            String email,
            String telefone,
            String emailReceita,
            String telefoneReceita,
            String enderecoSede,
            String govbrAccountId,
            String oabNumero,
            String oabUf,
            String emailOab,
            String sistemaPrincipal,
            String funcionalInstitucional,
            String emailInstitucional,
            String codigoTribunal,
            String comarca,
            String uf,
            boolean possuiContaGovBr,
            boolean possuiPortalGovBr,
            boolean possuiAdvogado,
            boolean cadastradoSistemaJudicial,
            boolean cnpjAtivo,
            boolean isGrandeEmpresa,
            boolean isBanco,
            boolean isFazendaPublica
    ) {
        public CitacaoIntimacaoEngine.PerfilDestinatario toPerfilDestinatario() {
            return switch (tipoDestinatario.trim().toUpperCase(Locale.ROOT)) {
                case "PESSOA_FISICA" -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                        cpf,
                        nome,
                        govbrAccountId,
                        email,
                        telefone,
                        possuiContaGovBr,
                        possuiAdvogado
                );
                case "PESSOA_JURIDICA" -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                        cnpj,
                        razaoSocial,
                        emailReceita,
                        telefoneReceita,
                        enderecoSede,
                        possuiPortalGovBr,
                        isGrandeEmpresa,
                        isBanco,
                        isFazendaPublica,
                        cnpjAtivo
                );
                case "ADVOGADO_OAB" -> new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                        cpf,
                        oabNumero,
                        oabUf,
                        emailOab,
                        cadastradoSistemaJudicial,
                        sistemaPrincipal
                );
                case "DEFENSOR_PUBLICO" -> new CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico(
                        cpf,
                        funcionalInstitucional,
                        emailInstitucional
                );
                case "MINISTERIO_PUBLICO" -> new CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico(
                        cpf,
                        funcionalInstitucional,
                        emailInstitucional
                );
                case "FAZENDA_PUBLICA" -> new CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica(
                        cnpj,
                        razaoSocial,
                        emailInstitucional,
                        nome
                );
                case "JUIZO_DEPRECADO" -> new CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado(
                        codigoTribunal,
                        comarca,
                        uf,
                        emailInstitucional
                );
                default -> throw new IllegalArgumentException("tipoDestinatario inválido: " + tipoDestinatario);
            };
        }
    }

    public record AcuseApiRequest(
            @NotBlank String tokenAcuse,
            String ipOrigem,
            String deviceFingerprint,
            String govbrSessionToken
    ) {
    }

    public record TipoDescricaoDto(
            String codigo,
            String descricao,
            String natureza,
            String fundamentoLegal,
            boolean exigePessoalidade,
            boolean admiteDigital,
            int diasPresuncaoEntrega
    ) {
    }

    public record ModalidadeDescricaoDto(
            String codigo,
            String label,
            String descricao,
            int prioridade,
            boolean digital,
            boolean exigeOficial,
            boolean exigeCorreio,
            int horasPresuncaoEntrega
    ) {
    }

    public record CompetenciaDescricaoDto(
            Long usuarioId,
            String usuarioNome,
            String perfil,
            String acao,
            Long processoId,
            String processoNumero,
            String microssistema,
            String grauJurisdicao,
            String tribunalSuperior,
            String tipoComunicacao,
            String autoridadeCompetente,
            String executorPreferencial,
            boolean permitido,
            boolean requerMagistrado,
            boolean requerOficial,
            boolean reservaTribunal,
            boolean representaParte,
            boolean delegavelSecretaria,
            boolean revisaoRegimentalHumana,
            List<String> fundamentos,
            List<String> alertas
    ) {
        static CompetenciaDescricaoDto from(ComunicacaoJudicialCompetenciaService.CompetenciaComunicacaoSnapshot snapshot) {
            return new CompetenciaDescricaoDto(
                    snapshot.usuarioId(),
                    snapshot.usuarioNome(),
                    snapshot.perfil(),
                    snapshot.acao().name(),
                    snapshot.processoId(),
                    snapshot.processoNumero(),
                    snapshot.microssistema(),
                    snapshot.grauJurisdicao(),
                    snapshot.tribunalSuperior(),
                    snapshot.tipoComunicacao(),
                    snapshot.autoridadeCompetente(),
                    snapshot.executorPreferencial(),
                    snapshot.permitido(),
                    snapshot.requerMagistrado(),
                    snapshot.requerOficial(),
                    snapshot.reservaTribunal(),
                    snapshot.representaParte(),
                    snapshot.delegavelSecretaria(),
                    snapshot.revisaoRegimentalHumana(),
                    snapshot.fundamentos(),
                    snapshot.alertas()
            );
        }
    }

    @PostMapping("/expedicoes")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','MINISTRO','SERVIDOR','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<CitacaoIntimacaoEngine.ExpedicaoResponse> expedir(@Valid @RequestBody ExpedicaoApiRequest req,
                                                                            Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "citacao_expedir", ApiVersion.V1);
        competenciaService.exigirExpedicao(req.processoId(), req.tipoComunicacao(), req.forcarOficial(), req.forcarDigital());
        CitacaoIntimacaoEngine.ExpedicaoRequest engineReq = new CitacaoIntimacaoEngine.ExpedicaoRequest(
                req.processoId(),
                req.tipoComunicacao(),
                req.destinatario().toPerfilDestinatario(),
                req.conteudoDoAto(),
                req.fundamentoAdicional(),
                req.forcarDigital(),
                req.forcarOficial(),
                req.juizResponsavelId(),
                req.servidorExpedidorId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(engine.expedir(engineReq));
    }

    @PostMapping("/expedicoes/{uuid}/acuse-recebimento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirmarRecebimento(@PathVariable String uuid,
                                                     @Valid @RequestBody AcuseApiRequest req,
                                                     Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, auth, "citacao_acuse_recebimento", ApiVersion.V1);
        engine.processarAcuseRecebimento(new CitacaoIntimacaoEngine.AcuseRecebimentoRequest(
                uuid,
                req.tokenAcuse(),
                req.ipOrigem(),
                req.deviceFingerprint(),
                req.govbrSessionToken()
        ));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expedicoes/{uuid}/confirmacao-leitura")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirmarLeitura(@PathVariable String uuid,
                                                 @RequestParam @NotBlank String acuseHash,
                                                 Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.CITIZEN, auth, "citacao_confirmacao_leitura", ApiVersion.V1);
        engine.processarConfirmacaoLeitura(uuid, acuseHash);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/expedicoes/{uuid}/frustracao")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','SERVIDOR','OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<CitacaoIntimacaoEngine.ExpedicaoResponse> registrarFrustracao(@PathVariable String uuid,
                                                                                         @RequestParam @NotBlank String motivo,
                                                                                         Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "citacao_frustracao", ApiVersion.V1);
        competenciaService.exigirFrustracao(uuid);
        return ResponseEntity.ok(engine.registrarFrustracaoEAcionarFallback(uuid, motivo));
    }

    @GetMapping("/evasao/{documento}")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','MINISTRO','SERVIDOR','ASSESSOR_JUDICIAL')")
    public ResponseEntity<CitacaoIntimacaoEngine.EvasaoRelatorio> analisarEvasao(@PathVariable @NotBlank String documento,
                                                                                  Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "citacao_evasao_analise", ApiVersion.V1);
        return ResponseEntity.ok(engine.analisarEvasao(documento));
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','MINISTRO','SERVIDOR')")
    public ResponseEntity<CitacaoIntimacaoEngine.PainelExpedicoes> painel(Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "citacao_painel", ApiVersion.V1);
        competenciaService.exigirPainel(null);
        return ResponseEntity.ok(engine.gerarPainel());
    }

    @GetMapping("/competencias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CompetenciaDescricaoDto> avaliarCompetencia(@RequestParam @NotNull Long processoId,
                                                                      @RequestParam(required = false) TipoComunicacaoJudicial tipoComunicacao,
                                                                      @RequestParam(defaultValue = "false") boolean forcarOficial,
                                                                      @RequestParam(defaultValue = "false") boolean forcarDigital,
                                                                      @RequestParam(defaultValue = "EXPEDIR") String acao) {
        ComunicacaoJudicialCompetenciaService.AcaoComunicacaoJudicial modo;
        try {
            modo = ComunicacaoJudicialCompetenciaService.AcaoComunicacaoJudicial.valueOf(acao.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ação de competência inválida: " + acao);
        }
        ComunicacaoJudicialCompetenciaService.CompetenciaComunicacaoSnapshot snapshot = switch (modo) {
            case EXPEDIR -> competenciaService.analisarExpedicao(processoId, tipoComunicacao, forcarOficial, forcarDigital);
            case DEFLAGRAR_INTERCEPTACAO -> competenciaService.analisarInterceptacao(processoId, tipoComunicacao, forcarOficial);
            case PAINEL_OPERACIONAL -> competenciaService.analisarPainel(processoId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ação de competência exige contexto específico de expedição ou diligência.");
        };
        return ResponseEntity.ok(CompetenciaDescricaoDto.from(snapshot));
    }

    @GetMapping("/tipos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TipoDescricaoDto>> listarTipos() {
        List<TipoDescricaoDto> tipos = Arrays.stream(TipoComunicacaoJudicial.values())
                .map(t -> new TipoDescricaoDto(
                        t.name(),
                        t.getDescricao(),
                        t.getNatureza().name(),
                        t.getFundamentoLegal(),
                        t.isExigePessoalidade(),
                        t.isAdmiteDigital(),
                        t.getDiasPresuncaoEntrega()
                ))
                .toList();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/modalidades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ModalidadeDescricaoDto>> listarModalidades() {
        List<ModalidadeDescricaoDto> modalidades = Arrays.stream(ModalidadeExpedicaoJudicial.values())
                .sorted(Comparator.comparingInt(ModalidadeExpedicaoJudicial::getPrioridade))
                .map(m -> new ModalidadeDescricaoDto(
                        m.name(),
                        m.getLabel(),
                        m.getDescricao(),
                        m.getPrioridade(),
                        m.isDigital(),
                        m.isExigeOficial(),
                        m.isExigeCorreio(),
                        m.getHorasPresuncaoEntrega()
                ))
                .toList();
        return ResponseEntity.ok(modalidades);
    }
}
