package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.AlvoJuridico;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.ViaInterceptacao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class CitacaoIntimacaoExpedicaoSupport {

    private CitacaoIntimacaoExpedicaoSupport() {
    }

    static ExpedicaoJudicial.TipoDestinatario resolverTipoDestinatario(CitacaoIntimacaoEngine.PerfilDestinatario dest) {
        return switch (dest) {
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica ignored -> ExpedicaoJudicial.TipoDestinatario.PESSOA_FISICA;
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica ignored -> ExpedicaoJudicial.TipoDestinatario.PESSOA_JURIDICA;
            case CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab ignored -> ExpedicaoJudicial.TipoDestinatario.ADVOGADO_OAB;
            case CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico ignored -> ExpedicaoJudicial.TipoDestinatario.DEFENSOR_PUBLICO;
            case CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico ignored -> ExpedicaoJudicial.TipoDestinatario.MINISTERIO_PUBLICO;
            case CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica ignored -> ExpedicaoJudicial.TipoDestinatario.FAZENDA_PUBLICA;
            case CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado ignored -> ExpedicaoJudicial.TipoDestinatario.JUIZO_DEPRECADO;
            default -> throw new IllegalStateException("Perfil de destinatário não suportado para tipo processual: " + dest);
        };
    }

    static String extrairDocumento(CitacaoIntimacaoEngine.PerfilDestinatario dest) {
        return switch (dest) {
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica pf -> normalizarDocumento(pf.cpf());
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica pj -> normalizarDocumento(pj.cnpj());
            case CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab adv -> normalizarDocumento(adv.cpf());
            case CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico d -> normalizarDocumento(d.cpf());
            case CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico m -> normalizarDocumento(m.cpf());
            case CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica fp -> normalizarDocumento(fp.cnpj());
            case CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado j -> upperTrim(j.codigoTribunal());
            default -> throw new IllegalStateException("Perfil de destinatário não suportado para documento: " + dest);
        };
    }

    static String extrairNome(CitacaoIntimacaoEngine.PerfilDestinatario dest) {
        return switch (dest) {
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica pf -> prefer(pf.nome(), "DESTINATÁRIO PESSOA FÍSICA");
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica pj -> prefer(pj.razaoSocial(), "DESTINATÁRIO PESSOA JURÍDICA");
            case CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab adv -> "Adv. OAB/" + upperTrim(adv.uf()) + " " + prefer(adv.oabNumero(), "SEM_NUMERO");
            case CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico d -> "Defensor Público - " + prefer(d.funcionalDefensoria(), "SEM_MATRICULA");
            case CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico m -> "Ministério Público - " + prefer(m.funcionalMp(), "SEM_MATRICULA");
            case CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica fp -> prefer(fp.razaoSocial(), "FAZENDA PÚBLICA");
            case CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado j -> "Juízo Deprecado - " + prefer(j.comarca(), "SEM_COMARCA") + "/" + prefer(j.uf(), "NA");
            default -> throw new IllegalStateException("Perfil de destinatário não suportado para nome: " + dest);
        };
    }

    static String montarFundamentacao(CitacaoIntimacaoEngine.ExpedicaoRequest request, Processo processo, ModalidadeExpedicaoJudicial modalidade) {
        String base = request.tipoComunicacao().getFundamentoLegal();
        String adicional = request.fundamentoAdicional() != null && !request.fundamentoAdicional().isBlank()
                ? " | " + request.fundamentoAdicional().trim()
                : "";
        String numeroProcesso = processo.getNumeroUnificado() != null ? processo.getNumeroUnificado() : String.valueOf(processo.getId());
        return base + adicional + " | Modalidade: " + modalidade.getLabel() + " | Processo: " + numeroProcesso;
    }

    static ExpedicaoJudicial.StatusExpedicao statusParaFallback(ModalidadeExpedicaoJudicial fallback) {
        return switch (fallback) {
            case OFICIAL_JUSTICA_ROTA_OTIMIZADA -> ExpedicaoJudicial.StatusExpedicao.PENDENTE_OFICIAL;
            case CORREIO_AR_DIGITAL -> ExpedicaoJudicial.StatusExpedicao.REMETIDA_CORREIO;
            case EDITAL_DOU_DJE -> ExpedicaoJudicial.StatusExpedicao.PUBLICADA_EDITAL;
            default -> ExpedicaoJudicial.StatusExpedicao.EXPEDIDA;
        };
    }

    static CitacaoIntimacaoEngine.ExpedicaoResponse buildResponseFrom(ExpedicaoJudicial expedicao, List<String> alertas) {
        return new CitacaoIntimacaoEngine.ExpedicaoResponse(
                expedicao.getExpedicaoUuid(),
                expedicao.getProcessoId(),
                expedicao.getTipoComunicacao(),
                expedicao.getModalidade(),
                expedicao.getStatus(),
                mascararDocumento(expedicao.getDestinatarioDocumento()),
                expedicao.getDestinatarioNome(),
                expedicao.getCanalDigitalUtilizado(),
                expedicao.getExpedidaEm(),
                expedicao.getPresuncaoEntregaEm(),
                alertas,
                List.of(),
                expedicao.getHashIntegridade(),
                expedicao.getFundamentacaoLegal(),
                expedicao.isEvasaoDetectada()
        );
    }

    static NationalPrazoEngine.TipoPrazo resolverTipoPrazoPadrao(ExpedicaoJudicial expedicao) {
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao()) {
            return NationalPrazoEngine.TipoPrazo.CONTESTACAO;
        }
        if (expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isIntimacao()) {
            return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
        }
        return NationalPrazoEngine.TipoPrazo.PRAZO_GENERICO;
    }

    static TipoUsuario resolverTipoUsuarioPrazo(ExpedicaoJudicial expedicao) {
        return switch (expedicao.getTipoDestinatario()) {
            case ADVOGADO_OAB -> TipoUsuario.ADVOGADO;
            case DEFENSOR_PUBLICO -> TipoUsuario.DEFENSOR_PUBLICO;
            case MINISTERIO_PUBLICO -> TipoUsuario.MEMBRO_MINISTERIO_PUBLICO;
            case FAZENDA_PUBLICA -> TipoUsuario.PROCURADOR;
            case JUIZO_DEPRECADO -> TipoUsuario.SERVIDOR;
            default -> TipoUsuario.CIDADAO;
        };
    }

    static CitacaoIntimacaoEngine.ExpedicaoRequest rebuildRequest(ExpedicaoJudicial expedicao, Processo processo) {
        CitacaoIntimacaoEngine.PerfilDestinatario perfil = switch (expedicao.getTipoDestinatario()) {
            case PESSOA_FISICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail(),
                    expedicao.getDestinatarioTelefone(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH || expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_WHATSAPP_GOV,
                    false
            );
            case PESSOA_JURIDICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail(),
                    expedicao.getDestinatarioTelefone(),
                    expedicao.getDestinatarioEnderecoEntrega(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.PORTAL_EMPRESA_CNPJ,
                    false,
                    false,
                    false,
                    true
            );
            case ADVOGADO_OAB -> new CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab(
                    expedicao.getDestinatarioDocumento(),
                    "NAO_INFORMADO",
                    processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : "NA",
                    expedicao.getDestinatarioEmail(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI,
                    "PJB"
            );
            case DEFENSOR_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico(
                    expedicao.getDestinatarioDocumento(),
                    "DEFENSORIA",
                    expedicao.getDestinatarioEmail()
            );
            case MINISTERIO_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico(
                    expedicao.getDestinatarioDocumento(),
                    "MP",
                    expedicao.getDestinatarioEmail()
            );
            case FAZENDA_PUBLICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail(),
                    "ENTE_PUBLICO"
            );
            case JUIZO_DEPRECADO -> new CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado(
                    processo.getTribunalCodigoRoteado(),
                    processo.getComarca() != null ? processo.getComarca() : "COMARCA_NAO_INFORMADA",
                    processo.getJurisdicao() != null ? processo.getJurisdicao().getEstado() : "NA",
                    expedicao.getDestinatarioEmail()
            );
        };
        return new CitacaoIntimacaoEngine.ExpedicaoRequest(
                expedicao.getProcessoId(),
                expedicao.getTipoComunicacao(),
                perfil,
                expedicao.getFundamentacaoLegal(),
                expedicao.getFundamentacaoLegal(),
                expedicao.getModalidade() != null && expedicao.getModalidade().isDigital(),
                expedicao.getModalidade() == ModalidadeExpedicaoJudicial.OFICIAL_JUSTICA_ROTA_OTIMIZADA,
                expedicao.getJuizResponsavelId(),
                expedicao.getServidorExpedidorId()
        );
    }

    static List<ViaInterceptacao> montarVias(CitacaoIntimacaoEngine.PerfilDestinatario dest, Processo processo, ExpedicaoJudicial expedicao) {
        List<ViaInterceptacao> vias = new ArrayList<>();
        switch (dest) {
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica pf -> {
                if (pf.possuiContaGovBr()) {
                    vias.add(new ViaInterceptacao.GovBrAutenticado(pf.govbrAccountId(), null, normalizarDocumento(pf.cpf()), "BRONZE"));
                }
                vias.add(new ViaInterceptacao.ReceitaFederalCnpjCpf(normalizarDocumento(pf.cpf()), false, pf.email(), pf.telefone(), true));
                if (pf.telefone() != null && !pf.telefone().isBlank()) {
                    vias.add(new ViaInterceptacao.AnatelOperadora(normalizarDocumento(pf.cpf()), "GOV", pf.telefone(), true));
                    vias.add(new ViaInterceptacao.AnatelOperadora(normalizarDocumento(pf.cpf()), "SMS", pf.telefone(), true));
                }
            }
            case CitacaoIntimacaoEngine.PerfilDestinatario.PessoaJuridica pj -> {
                vias.add(new ViaInterceptacao.ReceitaFederalCnpjCpf(normalizarDocumento(pj.cnpj()), true, pj.emailReceita(), pj.telefoneReceita(), pj.cnpjAtivo()));
                if (pj.possuiPortalGovBr()) {
                    vias.add(new ViaInterceptacao.PortalGovBrEmpresa(normalizarDocumento(pj.cnpj()), pj.razaoSocial(), true, pj.emailReceita()));
                }
                if (pj.emailReceita() != null && !pj.emailReceita().isBlank()) {
                    vias.add(new ViaInterceptacao.ReceitaFederalCnpjCpf(normalizarDocumento(pj.cnpj()), true, pj.emailReceita(), pj.enderecoSede(), pj.cnpjAtivo()));
                }
                if (pj.telefoneReceita() != null && !pj.telefoneReceita().isBlank()) {
                    vias.add(new ViaInterceptacao.AnatelOperadora(normalizarDocumento(pj.cnpj()), "GOV", pj.telefoneReceita(), true));
                }
                if (pj.isBanco() || pj.isGrandeEmpresa()) {
                    vias.add(new ViaInterceptacao.MalhaFinanceiraBacen("DICT:" + normalizarDocumento(pj.cnpj()), "PJBBANK", normalizarDocumento(pj.cnpj()), true));
                }
            }
            case CitacaoIntimacaoEngine.PerfilDestinatario.AdvogadoOab adv -> {
                vias.add(new ViaInterceptacao.OabSistemaJudicial(normalizarDocumento(adv.cpf()), adv.oabNumero(), adv.uf(), adv.sistemaPrincipal(), adv.emailOab(), adv.cadastradoSistemaJudicial()));
                if (adv.emailOab() != null && !adv.emailOab().isBlank()) {
                    vias.add(new ViaInterceptacao.ReceitaFederalCnpjCpf(normalizarDocumento(adv.cpf()), false, adv.emailOab(), null, true));
                }
            }
            case CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico defensor -> {
                if (defensor.emailInstitucional() != null && !defensor.emailInstitucional().isBlank()) {
                    vias.add(new ViaInterceptacao.Serpro(normalizarDocumento(defensor.cpf()), "DEFENSORIA", defensor.funcionalDefensoria(), null));
                }
            }
            case CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico mp -> {
                if (mp.emailInstitucional() != null && !mp.emailInstitucional().isBlank()) {
                    vias.add(new ViaInterceptacao.Serpro(normalizarDocumento(mp.cpf()), "MINISTERIO_PUBLICO", mp.funcionalMp(), null));
                }
            }
            case CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica fp -> vias.add(new ViaInterceptacao.ReceitaFederalCnpjCpf(
                    normalizarDocumento(fp.cnpj()),
                    true,
                    fp.emailPgfn(),
                    null,
                    true
            ));
            case CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado juizo -> vias.add(new ViaInterceptacao.CooperacaoCnjMalha(
                    upperTrim(juizo.codigoTribunal()),
                    juizo.comarca(),
                    juizo.uf(),
                    juizo.emailCarta()
            ));
        }
        return vias.stream().distinct().toList();
    }

    static AlvoJuridico construirAlvo(ExpedicaoJudicial expedicao, Processo processo, PjbHsmProperties hsmProperties) {
        return new AlvoJuridico(
                expedicao.getProcessoId(),
                expedienteNumero(processo),
                expedicao.getDestinatarioDocumento(),
                expedicao.getDestinatarioNome(),
                expedicao.getTipoDestinatario() == ExpedicaoJudicial.TipoDestinatario.PESSOA_JURIDICA
                        || expedicao.getTipoDestinatario() == ExpedicaoJudicial.TipoDestinatario.FAZENDA_PUBLICA,
                expedicao.getDestinatarioEmail(),
                expedicao.getDestinatarioTelefone(),
                expedicao.getDestinatarioEnderecoEntrega(),
                expedicao.getModalidade() != null && expedicao.getModalidade().isDigital(),
                expedicao.getFundamentacaoLegal(),
                hsmProperties.interceptacaoTimeoutMs()
        );
    }

    static String expedienteNumero(Processo processo) {
        return processo.getNumeroUnificado() != null ? processo.getNumeroUnificado() : String.valueOf(processo.getId());
    }

    static String computarHash(Long processoId, String documento, ModalidadeExpedicaoJudicial modalidade, TipoComunicacaoJudicial tipo, Instant timestamp, String anterior) {
        String raw = processoId + "|" + documento + "|" + modalidade + "|" + tipo + "|" + timestamp.toEpochMilli() + "|" + anterior;
        return sha256Hex(raw);
    }

    static String computarHashAcuse(CitacaoIntimacaoEngine.AcuseRecebimentoRequest acuse) {
        return sha256Hex(acuse.expedicaoUuid() + "|" + acuse.tokenAcuse() + "|" + Instant.now().toEpochMilli());
    }

    static String computarHashPainel(long... valores) {
        StringBuilder builder = new StringBuilder();
        for (long valor : valores) {
            builder.append(valor).append('|');
        }
        return sha256Hex(builder.toString());
    }

    static String mascararDocumento(String doc) {
        if (doc == null || doc.length() < 4) {
            return "***";
        }
        if (doc.length() == 11) {
            return doc.substring(0, 3) + ".***.***-" + doc.substring(9);
        }
        if (doc.length() == 14) {
            return doc.substring(0, 2) + ".***.***/****-" + doc.substring(12);
        }
        return doc.substring(0, 2) + "***" + doc.substring(doc.length() - 2);
    }

    static String prefer(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed != null ? trimmed : fallback;
    }

    static String normalizarDocumento(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D+", "");
        return digits.isBlank() ? trimmed.toUpperCase(Locale.ROOT) : digits;
    }

    static String upperTrim(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }
}
