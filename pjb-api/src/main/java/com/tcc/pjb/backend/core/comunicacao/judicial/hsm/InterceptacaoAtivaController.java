package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.util.Base64;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.tcc.pjb.backend.core.comunicacao.judicial.ComunicacaoJudicialCompetenciaService;
import com.tcc.pjb.backend.core.comunicacao.judicial.TipoComunicacaoJudicial;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

@RestController
@RequestMapping("/api/v1/interceptacao-ativa")
public class InterceptacaoAtivaController {

    private final MotorInterceptacaoAtiva motor;
    private final CapabilityRateLimiter rateLimiter;
    private final ComunicacaoJudicialCompetenciaService competenciaService;

    public InterceptacaoAtivaController(MotorInterceptacaoAtiva motor,
                                        CapabilityRateLimiter rateLimiter,
                                        ComunicacaoJudicialCompetenciaService competenciaService) {
        this.motor = motor;
        this.rateLimiter = rateLimiter;
        this.competenciaService = competenciaService;
    }

    public record InterceptacaoRequest(
            @NotNull AlvoJuridicoDto alvo,
            @NotNull List<@Valid ViaInterceptacaoDto> vias,
            TipoComunicacaoJudicial tipoComunicacao,
            @NotBlank String payloadBase64
    ) {
    }

    public record AlvoJuridicoDto(
            @NotBlank String documentoUnico,
            boolean exigeCadeiaCertificacao,
            String processoNumero,
            @NotNull Long processoId,
            String nomeDestinatario,
            boolean pessoaJuridica,
            String fundamentoLegal,
            Integer timeoutMs
    ) {
        AlvoJuridico toAlvoJuridico() {
            return new AlvoJuridico(
                    documentoUnico,
                    exigeCadeiaCertificacao,
                    processoNumero,
                    processoId,
                    nomeDestinatario,
                    pessoaJuridica,
                    fundamentoLegal,
                    timeoutMs != null ? timeoutMs : 8_000
            );
        }
    }

    public record ViaInterceptacaoDto(
            @NotBlank String tipo,
            String tokenSessaoGov,
            String ipOrigem,
            String cpfAlvo,
            String nivelConta,
            String chaveDictCentral,
            String ispbBanco,
            String documentoAlvo,
            Boolean chavePixAtiva,
            String emailReceita,
            String enderecoFiscal,
            Boolean cnpjAtivo,
            String ufSefaz,
            String emailOperacionalSefaz,
            String telefoneOperacionalSefaz,
            String enderecoOperacionalSefaz,
            Boolean emissorNfeAtivo,
            String codigoOperadora,
            String numeroVinculado,
            Boolean numeroAtivo,
            String uf,
            String enderecoRegistro,
            String renavam,
            String sistemaOrigem,
            String dadosVinculo,
            String enderecoAtualizado,
            String cpf,
            String oabNumero,
            String oabUf,
            String sistemaPrincipal,
            String emailInstitucional,
            Boolean mniAtivo,
            String codigoTribunalDestino,
            String comarcaDestino,
            String emailCarta,
            String cnpj,
            String razaoSocial,
            Boolean portalAtivo,
            String emailPortal,
            String nomeCompleto,
            String municipioNascimento,
            String ufNascimento
    ) {
        ViaInterceptacao toViaInterceptacao() {
            String tipoNormalizado = tipo.trim().toUpperCase(Locale.ROOT);
            return switch (tipoNormalizado) {
                case "GOV_BR" -> new ViaInterceptacao.GovBrAutenticado(
                        tokenSessaoGov,
                        ipOrigem,
                        InterceptacaoAtivaController.coalesceDocumento(cpfAlvo, cpf),
                        nivelConta
                );
                case "BACEN_DICT" -> new ViaInterceptacao.MalhaFinanceiraBacen(
                        chaveDictCentral,
                        ispbBanco,
                        InterceptacaoAtivaController.coalesceDocumento(documentoAlvo, cpf, cnpj, cpfAlvo),
                        Boolean.TRUE.equals(chavePixAtiva)
                );
                case "SEFAZ_NFE" -> new ViaInterceptacao.SefazNfeEmissor(
                        InterceptacaoAtivaController.coalesceDocumento(cnpj, documentoAlvo),
                        InterceptacaoAtivaController.upperTrim(InterceptacaoAtivaController.coalesceDocumento(ufSefaz, uf)),
                        emailOperacionalSefaz,
                        telefoneOperacionalSefaz,
                        enderecoOperacionalSefaz,
                        !Boolean.FALSE.equals(emissorNfeAtivo)
                );
                case "RECEITA_FEDERAL" -> new ViaInterceptacao.ReceitaFederalCnpjCpf(
                        InterceptacaoAtivaController.coalesceDocumento(documentoAlvo, cpf, cnpj, cpfAlvo),
                        inferirPessoaJuridica(documentoAlvo, cnpj),
                        emailReceita,
                        enderecoFiscal,
                        Boolean.TRUE.equals(cnpjAtivo)
                );
                case "ANATEL" -> new ViaInterceptacao.AnatelOperadora(
                        InterceptacaoAtivaController.coalesceDocumento(documentoAlvo, cpf, cnpj, cpfAlvo),
                        codigoOperadora,
                        numeroVinculado,
                        Boolean.TRUE.equals(numeroAtivo)
                );
                case "DETRAN" -> new ViaInterceptacao.DetranRegistroVeiculo(
                        InterceptacaoAtivaController.coalesceDocumento(documentoAlvo, cpf, cnpj, cpfAlvo),
                        InterceptacaoAtivaController.upperTrim(uf),
                        enderecoRegistro,
                        renavam
                );
                case "SERPRO" -> new ViaInterceptacao.Serpro(
                        InterceptacaoAtivaController.coalesceDocumento(documentoAlvo, cpf, cnpj, cpfAlvo),
                        sistemaOrigem,
                        dadosVinculo,
                        enderecoAtualizado
                );
                case "OAB" -> new ViaInterceptacao.OabSistemaJudicial(
                        InterceptacaoAtivaController.coalesceDocumento(cpf, cpfAlvo),
                        oabNumero,
                        InterceptacaoAtivaController.upperTrim(oabUf),
                        sistemaPrincipal,
                        emailInstitucional,
                        Boolean.TRUE.equals(mniAtivo)
                );
                case "CNJ_COOPERACAO" -> new ViaInterceptacao.CooperacaoCnjMalha(
                        codigoTribunalDestino,
                        comarcaDestino,
                        InterceptacaoAtivaController.upperTrim(uf),
                        emailCarta
                );
                case "PORTAL_EMPRESA" -> new ViaInterceptacao.PortalGovBrEmpresa(
                        InterceptacaoAtivaController.coalesceDocumento(cnpj, documentoAlvo),
                        razaoSocial,
                        Boolean.TRUE.equals(portalAtivo),
                        emailPortal
                );
                case "CRC" -> new ViaInterceptacao.CartorioRegistroCivil(
                        InterceptacaoAtivaController.coalesceDocumento(cpf, cpfAlvo),
                        nomeCompleto,
                        municipioNascimento,
                        InterceptacaoAtivaController.upperTrim(ufNascimento)
                );
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de via desconhecido: " + tipo);
            };
        }

        private static boolean inferirPessoaJuridica(String documentoAlvo, String cnpj) {
            if (cnpj != null && !cnpj.isBlank()) {
                return true;
            }
            String digits = InterceptacaoAtivaController.digitsOnly(documentoAlvo);
            return digits != null && digits.length() == 14;
        }
    }

    public record ViaDescricaoDto(
            String tipo,
            String label,
            int prioridade,
            int presuncaoHoras,
            String descricao
    ) {
    }

    @PostMapping("/deflagrar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','MINISTRO','SERVIDOR','ASSESSOR_JUDICIAL','ASSESSOR_DESEMBARGADOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<ReciboCitacaoHsm> deflagrar(@Valid @RequestBody InterceptacaoRequest req,
                                                      Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "interceptacao_deflagrar", ApiVersion.V1);
        competenciaService.exigirInterceptacao(req.alvo().processoId(), req.tipoComunicacao(), false);
        byte[] payload = decodePayload(req.payloadBase64());
        List<ViaInterceptacao> vias = req.vias().stream()
                .map(ViaInterceptacaoDto::toViaInterceptacao)
                .distinct()
                .limit(10)
                .toList();
        if (vias.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ao menos uma via de interceptação válida é obrigatória");
        }
        ReciboCitacaoHsm recibo = motor.interceptarComViasSugeridas(req.alvo().toAlvoJuridico(), payload, vias);
        HttpStatus status = recibo.foiEntregue() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(recibo);
    }

    @PostMapping("/fallback-fisico/{expedicaoUuid}")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','JUIZ_FEDERAL','DESEMBARGADOR','SERVIDOR','ASSESSOR_JUDICIAL')")
    public ResponseEntity<Void> acionarFallbackFisico(@PathVariable String expedicaoUuid,
                                                      @RequestBody ReciboCitacaoHsm reciboFalha,
                                                      Authentication auth) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, auth, "interceptacao_fallback", ApiVersion.V1);
        competenciaService.exigirFrustracao(expedicaoUuid);
        motor.acionarFallbackFisicoSeNecessario(reciboFalha, expedicaoUuid);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/vias")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ViaDescricaoDto>> listarVias() {
        return ResponseEntity.ok(List.of(
                new ViaDescricaoDto("GOV_BR", "Gov.br Push Autenticado", 1, 24, "Conta Gov.br Prata/Ouro"),
                new ViaDescricaoDto("BACEN_DICT", "Malha Financeira BACEN/PIX", 2, 48, "Chave Pix ativa no DICT"),
                new ViaDescricaoDto("SEFAZ_NFE", "SEFAZ NF-e Emissor", 3, 96, "Cadastro operacional de emissão NF-e com e-mail e endereço do estabelecimento"),
                new ViaDescricaoDto("RECEITA_FEDERAL", "Receita Federal CPF/CNPJ", 4, 72, "Cadastro fiscal federal para pessoa jurídica e domicílio fiscal para pessoa física"),
                new ViaDescricaoDto("ANATEL", "ANATEL / Operadora", 5, 48, "Número celular vinculado ao CPF/CNPJ"),
                new ViaDescricaoDto("DETRAN", "DETRAN / Veículo", 6, 72, "Endereço do proprietário via RENAVAM"),
                new ViaDescricaoDto("SERPRO", "SERPRO / RAIS / CAGED", 7, 72, "Vínculo empregatício e endereço atualizado"),
                new ViaDescricaoDto("OAB", "OAB / MNI Judicial", 8, 72, "Advogado com e-mail institucional ou MNI ativo"),
                new ViaDescricaoDto("CNJ_COOPERACAO", "Cooperação Judicial CNJ", 9, 240, "Malha intertribunais para outra comarca ou estado"),
                new ViaDescricaoDto("PORTAL_EMPRESA", "Portal Gov.br Empresa", 10, 72, "CNPJ com portal Gov.br ativo"),
                new ViaDescricaoDto("CRC", "Cartório Registro Civil", 11, 120, "Último recurso estruturado antes do edital")
        ));
    }

    private static byte[] decodePayload(String payloadBase64) {
        try {
            byte[] payload = Base64.getDecoder().decode(payloadBase64);
            if (payload.length == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadBase64 vazio após decodificação");
            }
            return payload;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payloadBase64 inválido", e);
        }
    }

    private static String upperTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private static String coalesceDocumento(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String digits = digitsOnly(value);
            if (digits != null) {
                return digits;
            }
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return null;
    }

    private static String digitsOnly(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }
}
