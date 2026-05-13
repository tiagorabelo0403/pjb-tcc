package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Processo;

@Service
public class ComunicacaoJudicialMensagemPrazoVivoService {

    public enum MarcoPrazo {
        ABERTO,
        FALTAM_TRES_DIAS,
        FALTA_UM_DIA,
        VENCE_HOJE,
        VENCIDO
    }

    public record MensagemPrazo(String titulo,
                                String corpo,
                                String resumo,
                                String cor,
                                String hashMensagem) {
    }

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MatrizComunicacaoJudicialResolver matrizResolver;

    public ComunicacaoJudicialMensagemPrazoVivoService(MatrizComunicacaoJudicialResolver matrizResolver) {
        this.matrizResolver = Objects.requireNonNull(matrizResolver, "matrizResolver");
    }

    public MensagemPrazo construir(ExpedicaoJudicial expedicao,
                                   Processo processo,
                                   PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                                   MarcoPrazo marco,
                                   boolean representante) {
        Objects.requireNonNull(expedicao, "expedicao");
        Objects.requireNonNull(prazo, "prazo");
        Objects.requireNonNull(marco, "marco");
        ProceduralCommunicationDecision decisao = matrizResolver.resolver(processo, expedicao.getTipoComunicacao(), reconstruirPerfil(expedicao));
        String numero = numeroProcesso(expedicao, processo);
        String tipoAto = tipoComunicacao(expedicao);
        String acao = acaoPrincipal(prazo.tipoPrazo(), tipoAto);
        String titulo = titulo(numero, tipoAto, prazo, marco, representante);
        String corpo = corpo(numero, tipoAto, prazo, marco, representante, acao, decisao, processo);
        String resumo = resumo(tipoAto, prazo, marco, representante);
        String cor = cor(marco, expedicao);
        String hash = Integer.toHexString(Objects.hash(expedicao.getExpedicaoUuid(), prazo.prazoUuid(), marco, representante, corpo));
        return new MensagemPrazo(titulo, corpo, resumo, cor, hash);
    }

    private String titulo(String numero,
                          String tipoAto,
                          PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                          MarcoPrazo marco,
                          boolean representante) {
        String base = representante ? "Prazo do cliente no processo " + numero : "Prazo no processo " + numero;
        return switch (marco) {
            case ABERTO -> base + " - " + tipoAto + " com vencimento em " + formatarData(prazo.vencimentoEm());
            case FALTAM_TRES_DIAS -> base + " - faltam 3 dias para " + verboCurto(prazo.tipoPrazo());
            case FALTA_UM_DIA -> base + " - falta 1 dia para " + verboCurto(prazo.tipoPrazo());
            case VENCE_HOJE -> base + " - vence hoje a providência de " + verboCurto(prazo.tipoPrazo());
            case VENCIDO -> base + " - prazo vencido para " + verboCurto(prazo.tipoPrazo());
            default -> base + " - " + tipoAto + " com vencimento em " + formatarData(prazo.vencimentoEm());
        };
    }

    private String corpo(String numero,
                         String tipoAto,
                         PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                         MarcoPrazo marco,
                         boolean representante,
                         String acao,
                         ProceduralCommunicationDecision decisao,
                         Processo processo) {
        List<String> partes = new ArrayList<>();
        partes.add(cabecalho(numero, tipoAto, prazo, marco, representante, acao));
        partes.add(contexto(decisao, processo));
        partes.add(materializacao(decisao, expedicaoLabel(tipoAto, decisao)));
        partes.add(prazoTexto(prazo, marco));
        partes.add(orientacaoFinal(tipoAto, prazo, marco, representante, decisao, acao));
        return partes.stream().filter(Objects::nonNull).filter(s -> !s.isBlank()).map(String::trim).reduce((a, b) -> a + " " + b).orElse("");
    }

    private String cabecalho(String numero,
                             String tipoAto,
                             PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                             MarcoPrazo marco,
                             boolean representante,
                             String acao) {
        String alvo = representante ? "cliente representado" : "seu cadastro";
        return switch (marco) {
            case ABERTO -> "O PJB consolidou o prazo decorrente da " + tipoAto + " ligada ao " + alvo + " no processo " + numero + ". A providência principal indicada é " + acao + ".";
            case FALTAM_TRES_DIAS -> "O PJB identificou que faltam 3 dias para o vencimento da providência de " + acao + " no processo " + numero + ".";
            case FALTA_UM_DIA -> "O PJB identificou que resta 1 dia para o vencimento da providência de " + acao + " no processo " + numero + ".";
            case VENCE_HOJE -> "O PJB identificou que o prazo para " + acao + " se encerra hoje no processo " + numero + ".";
            case VENCIDO -> "O PJB identificou que o prazo para " + acao + " já alcançou o vencimento registrado no processo " + numero + ".";
            default -> "O PJB consolidou o prazo decorrente da " + tipoAto + " ligada ao " + alvo + " no processo " + numero + ". A providência principal indicada é " + acao + ".";
        };
    }

    private String contexto(ProceduralCommunicationDecision decisao, Processo processo) {
        List<String> partes = new ArrayList<>();
        if (processo != null && processo.getRito() != null) {
            partes.add("rito " + humanizarEnum(processo.getRito().name()));
        }
        if (processo != null && processo.getFaseAtual() != null) {
            partes.add("fase " + humanizarEnum(processo.getFaseAtual().name()));
        }
        if (decisao != null && decisao.eixoProcedimental() != null && !decisao.eixoProcedimental().isBlank()) {
            partes.add("eixo " + decisao.eixoProcedimental().replace('/', ' '));
        }
        if (partes.isEmpty()) {
            return null;
        }
        return "Contexto processual considerado: " + String.join("; ", partes) + ".";
    }

    private String materializacao(ProceduralCommunicationDecision decisao, String fallback) {
        if (decisao == null || decisao.marcadores() == null) {
            return "Forma de ciência considerada: " + fallback + ".";
        }
        for (String marcador : decisao.marcadores()) {
            if (!marcador.startsWith("materializacao=")) {
                continue;
            }
            String valor = marcador.substring("materializacao=".length());
            String texto = switch (valor) {
                case "representante_digital" -> "ciência digital por representante habilitado";
                case "oficial_justica" -> "cumprimento presencial por oficial de justiça";
                case "digital_direta" -> "ciência digital direta no PJB";
                case "hora_certa" -> "trilha subsidiária de hora certa";
                case "cooperacao_externa" -> "cooperação judicial externa";
                case "executiva" -> "fase executiva com reforço coercitivo";
                default -> humanizarEnum(valor);
            };
            return "Forma de ciência considerada: " + texto + ".";
        }
        return "Forma de ciência considerada: " + fallback + ".";
    }

    private String prazoTexto(PrazoRespostaPosEntregaEngine.PrazoResposta prazo, MarcoPrazo marco) {
        String modo = prazo.emDiasUteis() ? "dias úteis" : "dias corridos";
        String fundamento = prazo.fundamentoLegal() == null || prazo.fundamentoLegal().isBlank()
                ? null
                : prazo.fundamentoLegal().trim();
        String base = "Prazo identificado para " + verboCurto(prazo.tipoPrazo()) + ": início em " + formatarData(prazo.inicioEm()) + ", vencimento em " + formatarData(prazo.vencimentoEm()) + ", com contagem em " + modo + ".";
        String alerta = switch (marco) {
            case ABERTO -> "O acompanhamento já foi lançado na agenda processual do PJB.";
            case FALTAM_TRES_DIAS -> "O sistema recomenda organização prévia da peça e validação de eventual suspensão local ou prazo em dobro.";
            case FALTA_UM_DIA -> "O sistema recomenda revisão final imediata da providência, porque a janela útil restante é mínima.";
            case VENCE_HOJE -> "O sistema recomenda atuação imediata dentro do expediente forense aplicável.";
            case VENCIDO -> "O sistema recomenda verificar imediatamente se houve protocolo, suspensão ou necessidade de petição superveniente.";
            default -> "O acompanhamento já foi lançado na agenda processual do PJB.";
        };
        if (fundamento == null) {
            return base + " " + alerta;
        }
        return base + " Fundamento-base: " + resumir(fundamento, 180) + ". " + alerta;
    }

    private String orientacaoFinal(String tipoAto,
                                   PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                                   MarcoPrazo marco,
                                   boolean representante,
                                   ProceduralCommunicationDecision decisao,
                                   String acao) {
        if (marco == MarcoPrazo.VENCIDO) {
            return representante
                    ? "Providência sugerida: verificar com prioridade se o cliente já cumpriu a providência, se houve protocolo tempestivo ou se existe medida remanescente de regularização."
                    : "Providência sugerida: verificar imediatamente se a manifestação já foi protocolada e buscar orientação jurídica sem aguardar novo lembrete.";
        }
        if (decisao != null && decisao.bloquearPresuncao()) {
            return representante
                    ? "Providência sugerida: acompanhar a trilha formal do ato com cautela reforçada, porque este fluxo possui sensibilidade processual elevada."
                    : "Providência sugerida: acompanhar a íntegra do ato e o painel do processo com cautela reforçada, porque este fluxo possui sensibilidade processual elevada.";
        }
        String alvo = representante ? "cliente representado" : "cadastro vinculado";
        return "Providência sugerida: tratar o marco de " + formatarData(prazo.vencimentoEm()) + " como referência operacional central para " + acao + ", mantendo o acompanhamento do " + alvo + " no PJB até a confirmação do protocolo ou da providência equivalente.";
    }

    private String resumo(String tipoAto,
                          PrazoRespostaPosEntregaEngine.PrazoResposta prazo,
                          MarcoPrazo marco,
                          boolean representante) {
        String alvo = representante ? "do cliente" : "do ato";
        return switch (marco) {
            case ABERTO -> "Prazo " + alvo + " lançado na agenda com vencimento em " + formatarData(prazo.vencimentoEm()) + ".";
            case FALTAM_TRES_DIAS -> "Faltam 3 dias para o vencimento " + alvo + ".";
            case FALTA_UM_DIA -> "Falta 1 dia para o vencimento " + alvo + ".";
            case VENCE_HOJE -> "Prazo " + alvo + " vence hoje.";
            case VENCIDO -> "Prazo " + alvo + " registrado como vencido.";
            default -> "Prazo " + alvo + " lançado na agenda com vencimento em " + formatarData(prazo.vencimentoEm()) + ".";
        };
    }

    private String cor(MarcoPrazo marco, ExpedicaoJudicial expedicao) {
        return switch (marco) {
            case ABERTO -> expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao() ? "AMBER" : "BLUE";
            case FALTAM_TRES_DIAS -> "ORANGE";
            case FALTA_UM_DIA, VENCE_HOJE, VENCIDO -> "RED";
            default -> expedicao.getTipoComunicacao() != null && expedicao.getTipoComunicacao().isCitacao() ? "AMBER" : "BLUE";
        };
    }

    private String expedicaoLabel(String tipoAto, ProceduralCommunicationDecision decisao) {
        if (decisao != null && decisao.priorizarOficialJustica()) {
            return tipoAto + " com reforço presencial";
        }
        return tipoAto;
    }

    private String tipoComunicacao(ExpedicaoJudicial expedicao) {
        if (expedicao.getTipoComunicacao() == null) {
            return "comunicação judicial";
        }
        if (expedicao.getTipoComunicacao().isCitacao()) {
            return "citação";
        }
        if (expedicao.getTipoComunicacao().isIntimacao()) {
            return "intimação";
        }
        if (expedicao.getTipoComunicacao().isMandado()) {
            return "mandado";
        }
        if (expedicao.getTipoComunicacao().isEdital()) {
            return "edital";
        }
        String descricao = expedicao.getTipoComunicacao().getDescricao();
        return descricao == null || descricao.isBlank() ? humanizarEnum(expedicao.getTipoComunicacao().name()) : descricao.toLowerCase(Locale.ROOT);
    }

    private String acaoPrincipal(com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.TipoPrazo tipoPrazo, String tipoAto) {
        if (tipoPrazo == null) {
            return tipoAto.equals("citação") ? "apresentar resposta processual" : "apresentar manifestação";
        }
        return switch (tipoPrazo) {
            case CONTESTACAO -> "apresentar contestação";
            case RECONVENCAO -> "avaliar e protocolar reconvenção";
            case REPLICA -> "apresentar réplica";
            case EMBARGOS_DECLARACAO -> "avaliar e protocolar embargos de declaração";
            case APELACAO -> "avaliar e protocolar apelação";
            case AGRAVO_INTERNO -> "avaliar e protocolar agravo interno";
            case AGRAVO_INSTRUMENTO -> "avaliar e protocolar agravo de instrumento";
            case AGRAVO_RECURSO_SUPERIOR -> "avaliar e protocolar agravo em recurso para tribunal superior";
            case RECURSO_ESPECIAL -> "avaliar e protocolar recurso especial";
            case RECURSO_EXTRAORDINARIO -> "avaliar e protocolar recurso extraordinário";
            case RECURSO_ORDINARIO_CONSTITUCIONAL -> "avaliar e protocolar recurso ordinário constitucional";
            case EMBARGOS_DIVERGENCIA -> "avaliar e protocolar embargos de divergência";
            case RECLAMACAO_CONSTITUCIONAL -> "avaliar e protocolar reclamação constitucional";
            case CONFLITO_COMPETENCIA -> "suscitar ou responder conflito de competência";
            case INCIDENTE_REPETITIVO_ASSUNCAO -> "avaliar e protocolar incidente repetitivo ou de assunção";
            case SUSPENSAO_SEGURANCA_LIMINAR -> "avaliar pedido de suspensão de segurança ou liminar";
            case EMBARGOS_INFRINGENTES_NULIDADE -> "avaliar e protocolar embargos infringentes e de nulidade";
            case CONTRARRAZOES_APELACAO -> "apresentar contrarrazões de apelação";
            case CONTRARRAZOES_SUPERIOR -> "apresentar contrarrazões para tribunal superior";
            case HABEAS_CORPUS -> "atuar imediatamente na tutela de liberdade";
            case MANDADO_SEGURANCA -> "avaliar a providência mandamental";
            case ACAO_RESCISORIA -> "avaliar a medida rescisória";
            case CUMPRIMENTO_SENTENCA -> "adotar providência no cumprimento de sentença";
            case EMBARGOS_EXECUCAO -> "avaliar e protocolar embargos à execução";
            case IMPUGNACAO_CUMPRIMENTO -> "apresentar impugnação ao cumprimento de sentença";
            case RESPOSTA_TRABALHISTA -> "preparar resposta trabalhista";
            case APRESENTACAO_DEFESA_PENAL -> "apresentar defesa penal";
            case ALEGACOES_FINAIS_PENAL -> "apresentar alegações finais";
            case RECURSO_TRABALHISTA -> "avaliar e protocolar recurso trabalhista";
            case RECURSO_ELEITORAL -> "avaliar e protocolar recurso eleitoral";
            case RECURSO_MILITAR -> "avaliar e protocolar recurso militar";
            case PRAZO_MP_MANIFESTACAO -> "apresentar manifestação ministerial";
            case PRAZO_PERICIA -> "cumprir a providência pericial";
            case PETICAO_INICIAL, PRAZO_GENERICO -> tipoAto.equals("citação") ? "apresentar resposta processual" : "apresentar manifestação";
            default -> tipoAto.equals("citação") ? "apresentar resposta processual" : "apresentar manifestação";
        };
    }

    private String verboCurto(com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine.TipoPrazo tipoPrazo) {
        return acaoPrincipal(tipoPrazo, "comunicação judicial").replace("avaliar e protocolar ", "").replace("apresentar ", "");
    }

    private CitacaoIntimacaoEngine.PerfilDestinatario reconstruirPerfil(ExpedicaoJudicial expedicao) {
        ExpedicaoJudicial.TipoDestinatario tipoDestinatario = Objects.requireNonNull(expedicao.getTipoDestinatario(), "tipoDestinatario");
        return switch (tipoDestinatario) {
            case PESSOA_FISICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.PessoaFisica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail(),
                    expedicao.getDestinatarioTelefone(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_GOVBR_PUSH,
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI
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
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail(),
                    expedicao.getModalidade() == ModalidadeExpedicaoJudicial.DIGITAL_DOMICILIO_ELETRONICO_MNI,
                    "PJB"
            );
            case DEFENSOR_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.DefensorPublico(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail()
            );
            case MINISTERIO_PUBLICO -> new CitacaoIntimacaoEngine.PerfilDestinatario.MinisterioPublico(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail()
            );
            case FAZENDA_PUBLICA -> new CitacaoIntimacaoEngine.PerfilDestinatario.FazendaPublica(
                    expedicao.getDestinatarioDocumento(),
                    expedicao.getDestinatarioNome(),
                    expedicao.getDestinatarioEmail(),
                    expedicao.getInstanciaExpedidora()
            );
            case JUIZO_DEPRECADO -> new CitacaoIntimacaoEngine.PerfilDestinatario.JuizoDeprecado(
                    expedicao.getInstanciaExpedidora(),
                    expedicao.getDestinatarioNome(),
                    null,
                    expedicao.getDestinatarioEmail()
            );
            default -> throw new IllegalStateException("Tipo de destinatário não suportado: " + tipoDestinatario);
        };
    }

    private String numeroProcesso(ExpedicaoJudicial expedicao, Processo processo) {
        if (processo != null && processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (expedicao.getNumeroUnificado() != null && !expedicao.getNumeroUnificado().isBlank()) {
            return expedicao.getNumeroUnificado();
        }
        return String.valueOf(expedicao.getProcessoId());
    }

    private String formatarData(LocalDate data) {
        return data == null ? "não definida" : DATA.format(data);
    }

    private String humanizarEnum(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String resumir(String value, int limite) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String compacta = value.trim().replaceAll("\\s+", " ");
        if (compacta.length() <= limite) {
            return compacta;
        }
        return compacta.substring(0, Math.max(0, limite - 1)).trim() + "…";
    }
}
